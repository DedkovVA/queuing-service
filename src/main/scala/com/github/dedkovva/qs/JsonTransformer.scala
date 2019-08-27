package com.github.dedkovva.qs

import com.github.dedkovva.qs.ApiDispatcher.HttpRqs
import com.github.dedkovva.qs.Types._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json._


/**
  * Created by dedkov-va on 12.04.18.
  */
trait JsonTransformer extends PlayJsonSupport {
  implicit val companyProductFormat: Format[CompanyProduct] = new Format[CompanyProduct] {
    override def reads(json: JsValue): JsResult[CompanyProduct] = JsSuccess(CompanyProduct.valueOf(json.as[String]))
    override def writes(o: CompanyProduct): JsValue = JsString(o.name)
  }

  implicit val orderStatusFormat: Format[OrderStatus] = new Format[OrderStatus] {
    override def reads(json: JsValue): JsResult[OrderStatus] = JsSuccess(OrderStatus.valueOf(json.as[String]))
    override def writes(o: OrderStatus): JsValue = JsString(o.name)
  }

  implicit val httpRqFormat = Json.format[HttpRq]
  implicit val httpRqsFormat = Json.format[HttpRqs]
}
