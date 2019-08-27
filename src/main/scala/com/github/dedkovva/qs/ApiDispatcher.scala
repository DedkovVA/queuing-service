package com.github.dedkovva.qs

import java.nio.charset.Charset

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import com.github.dedkovva.qs.Boot._
import com.github.dedkovva.qs.Types._
import com.github.dedkovva.qs.Util._
import com.github.dedkovva.qs.ApiDispatcher._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ApiDispatcher(toStrictEntityTimeOut: FiniteDuration) extends Actor with ActorLogging with JsonTransformer {
  private val minQueueDepth = AppConfig.Queue.minNumBeforeProcessing
  private val pullEveryNSeconds = AppConfig.Queue.pullEveryNSeconds

  private var scheduler: Option[Cancellable] = None
  private var accumulator = Seq.empty[RqsByActor]
  private var rqsWithAwaitingSenders = Seq.empty[RqsByActor]
  private var productsQueue = Seq.empty[HttpRq]
  private var ordersQueue = Seq.empty[HttpRq]

  override def receive: Receive = {
    case msg@HttpRqs(requests) =>
      log.info(s"got msg: $msg")
      val accumulatorBeforeUpdateWasEmpty = accumulator.isEmpty
      accumulator :+= RqsByActor(msg, sender)

      val (productApiRqs, orderApiRqs, _) = splitRequests(requests)
      productsQueue ++= productApiRqs
      ordersQueue ++= orderApiRqs

      if (!checkStateAndCallApiAsync() && accumulatorBeforeUpdateWasEmpty) {
        scheduler = Option(context.system.scheduler.scheduleOnce(pullEveryNSeconds seconds, self, Tick))
      }

    case msg@AsyncApiCallR(productCallR: ApiCall[CompanyProductSeq], orderCallR: ApiCall[OrderStatus]) =>
      log.info(s"got msg: $msg")
      unfoldApiCalls(rqsWithAwaitingSenders, productCallR, orderCallR).foreach {
        case(result: ApisCallResult, sender: ActorRef) => sender ! result
      }
      rqsWithAwaitingSenders = Seq.empty

      self ! Push

    case msg@Push =>
      log.info(s"got msg: $msg")
      checkStateAndCallApiAsync()

    case msg@Tick =>
      log.info(s"got msg: $msg")
      if (rqsWithAwaitingSenders.isEmpty && accumulator.nonEmpty && scheduler.exists(!_.isCancelled)) {
        log.info(s"tick processing")
        callApisAsync()
      }

    case msg =>
      log.info(s"got unknown msg: $msg")
  }

  private def checkStateAndCallApiAsync(): Boolean = {
    val checked = checkState
    if (checked) callApisAsync()
    checked
  }

  private def checkState: Boolean = {
    rqsWithAwaitingSenders.isEmpty && (productsQueue.length > minQueueDepth || ordersQueue.length > minQueueDepth)
  }

  private def callApisAsync() = {
    scheduler.foreach((e: Cancellable) => if (!e.isCancelled) e.cancel())

    rqsWithAwaitingSenders = accumulator
    accumulator = Seq.empty

    val productRq: Option[HttpRq] = foldHttpRqs(productApiPrefix, productsQueue)
    val orderRq: Option[HttpRq] = foldHttpRqs(orderApiPrefix, ordersQueue)

    productsQueue = Seq.empty
    ordersQueue = Seq.empty

    val productsResponseFut: Future[ApiCall[CompanyProductSeq]] = callApiAsync(productRq, _.to[ProductApiCallSuccess])
    val ordersResponseFut: Future[ApiCall[OrderStatus]] = callApiAsync(orderRq, _.to[OrderApiCallSuccess])

    (for {
      productsResponse: ApiCall[CompanyProductSeq] <- productsResponseFut
      orderResponse: ApiCall[OrderStatus] <- ordersResponseFut
    } yield {
      AsyncApiCallR(productsResponse, orderResponse)
    }).pipeTo(self)
  }

  private def callApiAsync[T](rqOpt: Option[HttpRq], unmarshalF: UnmarshalF[ApiCallSuccess[T]]): Future[ApiCall[T]] = {
    rqOpt match {
      case Some(rq) =>
        Http().singleRequest(HttpRequest(uri = rq.uri)).flatMap { httpResponse =>
          log.info(s"got response: $httpResponse")
          httpResponse.status match {
            case StatusCodes.OK =>
              unmarshalF(Unmarshal(httpResponse.entity)).map((successR: ApiCallSuccess[T]) =>
                SingleApiCallResult(rq, Right(successR)))
            case statusCode =>
              log.warning(s"got http non-success response $httpResponse while processing request $rq")
              httpResponse.entity.toStrict(toStrictEntityTimeOut).map { strict: Strict =>
                val apiCallFailure = ApiCallFailure(
                  statusCode.intValue(),
                  strict.data.decodeString(Charset.forName("UTF-8")))
                SingleApiCallResult(rq, Left(apiCallFailure))
              }
          }
        }
      case _ =>
        Future.successful(EmptyApiCall)
    }
  }

  private def foldHttpRqs(prefix: String, httpRqs: Seq[HttpRq]): Option[HttpRq] = {
    if (httpRqs.nonEmpty) {
      val ids = httpRqs.flatMap(httpRqToUris(_, prefix)).distinct.mkString(",")
      Option(HttpRq(s"$prefix$ids"))
    } else {
      None
    }
  }

  private def unfoldApiCalls(rqsWithAwaitingSenders: Seq[RqsByActor],
                             productCallR: ApiCall[CompanyProductSeq],
                             orderCallR: ApiCall[OrderStatus]): Seq[(ApisCallResult, ActorRef)] = {
    rqsWithAwaitingSenders.map { case RqsByActor(HttpRqs(httpRqs: Seq[HttpRq]), sender: ActorRef) =>
      val (productApiRqs, orderApiRqs, unknownRqs) = splitRequests(httpRqs)

      val productApiCallResults = unfoldApiCall(productCallR, productApiRqs, productApiPrefix)
      val orderApiCallResults = unfoldApiCall(orderCallR, orderApiRqs, orderApiPrefix)

      (ApisCallResult(productApiCallResults, orderApiCallResults, unknownRqs), sender)
    }
  }

  private def unfoldApiCall[T](apiCall: ApiCall[T],
                               httpRqs: Seq[HttpRq],
                               prefix: String): Seq[SingleApiCallResult[T]] = {
    val things: Seq[(HttpRq, Seq[String])] =
      httpRqs.map(httpRq => (httpRq, httpRqToUris(httpRq, prefix)))

    apiCall match {
      case r: SingleApiCallResult[T] =>
        things.map { case(httpRq: HttpRq, uris: Seq[String]) =>
          SingleApiCallResult[T](httpRq, r.result.map((apiCallSuccess: ApiCallSuccess[T]) =>
            apiCallSuccess.filter { case(uri: String, _: T) => uris.contains(uri) }))
        }
      case EmptyApiCall =>
        Seq.empty[SingleApiCallResult[T]]
    }
  }

  private def splitRequests(requests: Seq[HttpRq]): (Seq[HttpRq], Seq[HttpRq], Seq[HttpRq]) = {
    val (productApiRqs, otherRqs) = requests.partition(checkRq(_, productApiPrefix))
    val (orderApiRqs, unknownRqs) = otherRqs.partition(checkRq(_, orderApiPrefix))
    (productApiRqs, orderApiRqs, unknownRqs)
  }

  private def httpRqToUris(httpRq: HttpRq, prefix: String): Seq[String] =
    queryToIds(httpRq.uri.substring(prefix.length))

  private def checkRq(rq: HttpRq, uri: String): Boolean = rq.method == GET.value && rq.uri.startsWith(uri)
}

object ApiDispatcher {
  def props(toStrictEntityTimeOut: FiniteDuration) = Props(new ApiDispatcher(toStrictEntityTimeOut))

  case class HttpRqs(requests: Seq[HttpRq])
  case class AsyncApiCallR(productCallR: ApiCall[CompanyProductSeq], orderCallR: ApiCall[OrderStatus])
  case object Push
  case object Tick
}
