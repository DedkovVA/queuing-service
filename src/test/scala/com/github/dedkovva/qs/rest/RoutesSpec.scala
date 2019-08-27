package com.github.dedkovva.qs.rest

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.dedkovva.qs.JsonTransformer
import com.github.dedkovva.qs.Types.{CompanyProduct, OrderStatus}
import org.scalatest.{FreeSpec, Matchers}

class RoutesSpec extends FreeSpec with Matchers with ScalatestRouteTest with JsonTransformer {
  "products api" in {
    import CompanyProduct._
    Get(
      "/product?q=1,abcdefghi,123456780,123456781,123456782,123456783,123456784,123456785," +
        "123456789,123456784") ~>
      Routes.paths ~> check {
        val productsR = responseAs[Map[String, Seq[CompanyProduct]]]
        productsR("1") shouldBe Seq(NULL)
        productsR("123456780") shouldBe Seq()
        Seq(Seq(ELECTRICITY), Seq(EV), Seq(GAS)).contains(productsR("123456781")) shouldBe true
        Seq(Seq(ELECTRICITY, EV), Seq(ELECTRICITY, GAS), Seq(EV, GAS)).contains(productsR("123456782")) shouldBe true
        productsR("123456783") shouldBe Seq(ELECTRICITY, EV, GAS)
        Seq(
          Seq(ELECTRICITY, EV, GAS, ELECTRICITY),
          Seq(ELECTRICITY, EV, GAS, EV),
          Seq(ELECTRICITY, EV, GAS, GAS)
        ).contains(productsR("123456784")) shouldBe true
        Seq(
          Seq(ELECTRICITY, EV, GAS, ELECTRICITY, ELECTRICITY),
          Seq(ELECTRICITY, EV, GAS, ELECTRICITY, EV),
          Seq(ELECTRICITY, EV, GAS, ELECTRICITY, GAS),

          Seq(ELECTRICITY, EV, GAS, EV, ELECTRICITY),
          Seq(ELECTRICITY, EV, GAS, EV, EV),
          Seq(ELECTRICITY, EV, GAS, EV, GAS),

          Seq(ELECTRICITY, EV, GAS, GAS, ELECTRICITY),
          Seq(ELECTRICITY, EV, GAS, GAS, EV),
          Seq(ELECTRICITY, EV, GAS, GAS, GAS)
        ).contains(productsR("123456785")) shouldBe true
        productsR("123456789").length shouldBe 9
        productsR("123456789").take(3) shouldBe Seq(ELECTRICITY, EV, GAS)
        productsR.size shouldBe 9
    }

    Get("/product?q=") ~> Routes.paths ~> check {
      val accountsR = responseAs[Map[String, Seq[CompanyProduct]]]
      accountsR.isEmpty shouldBe true
    }
  }

  "orders api" in {
    import OrderStatus._
    Get(
      "/order?q=1,abcdefghi,123456780,123456781,123456782,123456783,123456784,123456785," +
        "123456789,123456784") ~>
      Routes.paths ~> check {
        val ordersR = responseAs[Map[String, OrderStatus]]
        ordersR("1") shouldBe NULL
        ordersR("abcdefghi") shouldBe NULL
        val values = Seq(NEW, ORDERING, SETUP, DELIVERED)
        values.contains(ordersR("123456780")) shouldBe true
        values.contains(ordersR("123456781")) shouldBe true
        values.contains(ordersR("123456782")) shouldBe true
        values.contains(ordersR("123456783")) shouldBe true
        values.contains(ordersR("123456784")) shouldBe true
        values.contains(ordersR("123456785")) shouldBe true
        values.contains(ordersR("123456789")) shouldBe true
        ordersR.size shouldBe 9
    }
  }
}
