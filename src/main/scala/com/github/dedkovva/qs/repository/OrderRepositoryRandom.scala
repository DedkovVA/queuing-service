package com.github.dedkovva.qs.repository

import com.github.dedkovva.qs.Types.{OrderApiCallSuccess, OrderStatus}

import scala.util.Random

object OrderRepositoryRandom extends OrderRepository {
  import OrderStatus._
  private val statuses = Seq(NEW, ORDERING, SETUP, DELIVERED)

  override def fetchOrders(customerIds: Seq[String]): OrderApiCallSuccess = {
    customerIds.map(e => e -> statuses(Random.nextInt(statuses.length))).toMap
  }
}
