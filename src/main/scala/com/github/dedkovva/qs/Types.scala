package com.github.dedkovva.qs

import akka.actor.ActorRef
import akka.http.scaladsl.model.ResponseEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.github.dedkovva.qs.ApiDispatcher.HttpRqs

import scala.concurrent.Future

object Types {
  type CompanyProductSeq = Seq[CompanyProduct]

  type ApiCallSuccess[T] = Map[String, T]
  type ProductApiCallSuccess = ApiCallSuccess[CompanyProductSeq]
  type OrderApiCallSuccess = ApiCallSuccess[OrderStatus]

  type ApiCallResult[T] = Seq[SingleApiCallResult[T]]

  type UnmarshalF[T] = Unmarshal[ResponseEntity] => Future[T]

  trait CustomEnum[T] {
    trait Value { self: T =>
      val name: String
    }
    val values: Seq[T]
    def valueOf(name: String): T
  }


  sealed trait CompanyProduct extends CompanyProduct.Value
  object CompanyProduct extends CustomEnum[CompanyProduct] {
    case object ELECTRICITY extends CompanyProduct { val name = "ELECTRICITY"}
    case object EV extends CompanyProduct { val name = "EV"}
    case object GAS extends CompanyProduct { val name = "GAS"}
    case object NULL extends CompanyProduct { val name = "NULL"}
    case object NON_FOUND extends CompanyProduct { val name = "NON_FOUND"}

    val values = Seq(ELECTRICITY, EV, GAS, NULL, NON_FOUND)
    def valueOf(name: String): CompanyProduct = values.find(_.name == name).getOrElse(NON_FOUND)
  }

  sealed trait OrderStatus extends OrderStatus.Value
  object OrderStatus extends CustomEnum[OrderStatus] {
    case object NEW extends OrderStatus { val name = "NEW"}
    case object ORDERING extends OrderStatus { val name = "ORDERING"}
    case object SETUP extends OrderStatus { val name = "SETUP"}
    case object DELIVERED extends OrderStatus { val name = "DELIVERED"}
    case object NULL extends OrderStatus { val name = "NULL"}
    case object NON_FOUND extends OrderStatus { val name = "NON_FOUND"}

    val values = Seq(NEW, ORDERING, SETUP, DELIVERED, NULL, NON_FOUND)
    def valueOf(name: String): OrderStatus = values.find(_.name == name).getOrElse(NON_FOUND)
  }

  case class HttpRq(uri: String, method: String = "GET")

  case class CustomerIds(correct: Seq[String], incorrect: Seq[String])

  case class ApiCallFailure(httpStatusCode: Int, message: String)

  sealed trait ApiCall[+T]
  case class SingleApiCallResult[T](httpRq: HttpRq, result: Either[ApiCallFailure, ApiCallSuccess[T]]) extends ApiCall[T]
  case object EmptyApiCall extends ApiCall[Nothing]

  case class ApisCallResult(productApiCallResults: ApiCallResult[CompanyProductSeq] = Seq.empty,
                            orderApiCallResults: ApiCallResult[OrderStatus] = Seq.empty,
                            unknownCalls: Seq[HttpRq] = Seq.empty)

  case class RqsByActor(httpRqs: HttpRqs, actorRef: ActorRef)
}
