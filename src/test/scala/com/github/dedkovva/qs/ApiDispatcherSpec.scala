package com.github.dedkovva.qs

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit}
import com.github.dedkovva.qs.ApiDispatcher.HttpRqs
import com.github.dedkovva.qs.AppConfig.Http.{host, port}
import com.github.dedkovva.qs.Types._
import com.github.dedkovva.qs.rest.Routes
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


class ApiDispatcherSpec extends TestKit(ActorSystem("ApiDispatcherSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  private val uriPrefix = s"http://$host:$port"

  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val routes = Route.handlerFlow(Routes.paths)
  private val http = Http(system)
  private val binding = http.bindAndHandle(routes, host, port)

  private def createApiDispatcher(): ActorRef = system.actorOf(ApiDispatcher.props(5000 millis))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "num of http requests was not exceeded" in {
    val apiDispatcher = createApiDispatcher

    apiDispatcher ! HttpRqs(Range(0, 100).map(i => HttpRq(s"$uriPrefix/product?q=$i")))

    val result = receiveWhile() {
      case msg: ApisCallResult => msg
    }

    result.length shouldBe 1
    result.head.productApiCallResults.length shouldBe 100
    result.head.orderApiCallResults.length shouldBe 0
    result.head.unknownCalls.length shouldBe 0
    val productsR = result.head.productApiCallResults.map(e => (e.httpRq, e.result)).toMap
    productsR(HttpRq(s"$uriPrefix/product?q=0")) shouldBe Right(Map("0" -> Seq(CompanyProduct.NULL)))
    productsR(HttpRq(s"$uriPrefix/product?q=1")) shouldBe Right(Map("1" -> Seq(CompanyProduct.NULL)))
    productsR(HttpRq(s"$uriPrefix/product?q=99")) shouldBe Right(Map("99" -> Seq(CompanyProduct.NULL)))
  }

  "num of http requests was not exceeded serial" in {
    val apiDispatcher = createApiDispatcher()

    val range = Range(0, 100)
    range.foreach(i =>
      apiDispatcher ! HttpRqs(Seq(HttpRq(s"$uriPrefix/product?q=$i")))
    )

    val result: Seq[ApisCallResult] = receiveWhile() {
      case msg: ApisCallResult => msg
    }

    result.length shouldBe range.length

    def checkAndReturnProductNum(index: Int): String = {
      val r = result(index).productApiCallResults.map(e => (e.httpRq, e.result)).toMap
      r.keys.toSeq.length shouldBe 1
      r.keys.head.uri.startsWith(s"$uriPrefix/product?q=") shouldBe true
      val productNum = r.keys.head.uri.substring(s"$uriPrefix/product?q=".length)
      r.head._2 shouldBe Right(Map(productNum -> Seq(CompanyProduct.NULL)))
      productNum
    }

    val productNum1 = checkAndReturnProductNum(0)
    val productNum2 = checkAndReturnProductNum(1)

    productNum1 != productNum2 shouldBe true
    range.map(_.toString).contains(productNum1) shouldBe true
    range.map(_.toString).contains(productNum2) shouldBe true
  }

  "all messages with responses bcz of queue timer" in {
    val apiDispatcher = createApiDispatcher()

    apiDispatcher ! HttpRqs(Range(1, 100).map(i => HttpRq(s"$i")))
    apiDispatcher ! HttpRqs(Range(1, 5).map(i => HttpRq(s"$uriPrefix/product?q=$i")))
    apiDispatcher ! HttpRqs(Range(1, 5).map(i => HttpRq(s"$uriPrefix/order?q=$i")))
    apiDispatcher ! HttpRqs(Range(1, 100).map(i => HttpRq(s"$uriPrefix/product?q=$i")))
    val result: Seq[ApisCallResult] = receiveWhile() {
      case msg: ApisCallResult => msg
    }
    result.length shouldBe 4

    def checkResult(apisCallResult: ApisCallResult, productsNum: Int, ordersNum: Int, unknownNum: Int) = {
      apisCallResult.productApiCallResults.length shouldBe productsNum
      apisCallResult.orderApiCallResults.length shouldBe ordersNum
      apisCallResult.unknownCalls.length shouldBe unknownNum
    }

    checkResult(result(0), 0, 0, 99)
    checkResult(result(1), 4, 0, 0)
    checkResult(result(2), 0, 4, 0)
    checkResult(result(3), 99, 0, 0)

    apiDispatcher ! HttpRqs(Range(1, 5).map(i => HttpRq(s"$uriPrefix/order?q=$i")))
    val result2: ApisCallResult = expectMsgPF(7 seconds) {
      case msg: ApisCallResult => msg
    }

    checkResult(result2, 0, 4, 0)
  }

  "repeats" in {
    val apiDispatcher = createApiDispatcher()

    def repeat() = {
      apiDispatcher ! HttpRqs(Seq(
        HttpRq(s"$uriPrefix/product?q=1,2,3"),
        HttpRq(s"$uriPrefix/product?q=1,2,123456789"),
        HttpRq(s"$uriPrefix/product?q=1,2,3"),
        HttpRq(s"$uriPrefix/product?q=1,2,3"),
        HttpRq(s"$uriPrefix/order?q=123456789,1,2"),
        HttpRq(s"$uriPrefix/product?q=1,123456783"),
        HttpRq(s"$uriPrefix/unknown2"),
        HttpRq(s"$uriPrefix/order?q=123456781"),
        HttpRq(s"$uriPrefix/order?q=-12345678"),
        HttpRq(s"$uriPrefix/unknown")
      ))

      import CompanyProduct.{NULL => PNULL, _}
      import OrderStatus.{NULL => SNULL, _}
      val r: ApisCallResult = expectMsgPF() {
        case r: ApisCallResult => r
      }

      r.productApiCallResults.size shouldBe 5
      val products = r.productApiCallResults.map(e => (e.httpRq, e.result)).toMap
      products(HttpRq(s"$uriPrefix/product?q=1,2,3")) shouldBe Right(Map(
        "1" -> Seq(PNULL),
        "2" -> Seq(PNULL),
        "3" -> Seq(PNULL)
      ))

      val productR1 = products(HttpRq(s"$uriPrefix/product?q=1,2,123456789")).right.getOrElse(Map.empty)
      productR1.size shouldBe 3
      productR1("1") shouldBe Seq(PNULL)
      productR1("2") shouldBe Seq(PNULL)
      productR1("123456789").size shouldBe 9
      productR1("123456789").take(3) shouldBe Seq(ELECTRICITY, EV, GAS)

      val productR2 = products(HttpRq(s"$uriPrefix/product?q=1,123456783")).right.getOrElse(Map.empty)
      productR2.size shouldBe 2
      productR2("1") shouldBe Seq(PNULL)
      productR2("123456783") shouldBe Seq(ELECTRICITY, EV, GAS)

      r.orderApiCallResults.size shouldBe 3
      val orders = r.orderApiCallResults.map(e => (e.httpRq, e.result)).toMap

      val order1 = orders(HttpRq(s"$uriPrefix/order?q=123456789,1,2")).right.getOrElse(Map.empty)
      order1.size shouldBe 3
      order1("1") shouldBe SNULL
      order1("2") shouldBe SNULL
      Seq(NEW, DELIVERED, ORDERING, SETUP).contains(order1("123456789")) shouldBe true

      val order2 = orders(HttpRq(s"$uriPrefix/order?q=123456781")).right.getOrElse(Map.empty)
      order2.size == 1 shouldBe true
      Seq(NEW, DELIVERED, ORDERING, SETUP).contains(order2("123456781")) shouldBe true

      val order3 = orders(HttpRq(s"$uriPrefix/order?q=-12345678")).right.getOrElse(Map.empty)
      order3("-12345678") shouldBe SNULL

      r.unknownCalls.size shouldBe 2
      r.unknownCalls shouldBe Seq(HttpRq(s"$uriPrefix/unknown2"), HttpRq(s"$uriPrefix/unknown"))
    }

    Range(0, 10).foreach(_ => repeat())
  }

  "scheduler send messages" in {
    val apiDispatcher = createApiDispatcher()

    apiDispatcher ! HttpRqs(Seq(HttpRq(s"$uriPrefix/product?q=123456780,123456781,123456782")))
    val r1 = expectMsgPF(7 seconds) {
      case msg: ApisCallResult => msg
    }
    r1.productApiCallResults.length shouldBe 1

    apiDispatcher ! HttpRqs(Seq(HttpRq(s"$uriPrefix/order?q=123456780,123456781,123456782")))
    val r2 = expectMsgPF(7 seconds) {
      case msg: ApisCallResult => msg
    }
    r2.orderApiCallResults.length shouldBe 1
  }
}
