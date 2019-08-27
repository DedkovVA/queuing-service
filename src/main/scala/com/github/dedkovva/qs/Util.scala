package com.github.dedkovva.qs

import com.github.dedkovva.qs.AppConfig.Http.{host, port}
import com.github.dedkovva.qs.Types.CustomerIds

object Util {
  val productApiPrefix = s"http://$host:$port/product?q="
  val orderApiPrefix = s"http://$host:$port/order?q="

  def queryToIds(q: String): Seq[String] = q.split(",").toSeq.map(_.trim).filter(!_.isEmpty).distinct

  def checkIds(ids: Seq[String]): CustomerIds = {
    val checked = ids.partition(_.matches("[0-9]{9}"))
    CustomerIds(checked._1, checked._2)
  }
}
