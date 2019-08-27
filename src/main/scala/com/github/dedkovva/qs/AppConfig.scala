package com.github.dedkovva.qs

import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by dedkov-va on 07.04.18.
  */
object AppConfig {
  val config: Config = ConfigFactory.load()

  object Http {
    private val http = config.getConfig("http")

    val host: String = http.getString("host")
    val port: Int = http.getInt("port")
  }

  object Queue {
    private val queue: Config = config.getConfig("queue")

    val minNumBeforeProcessing: Int = queue.getInt("minNumBeforeProcessing")
    val pullEveryNSeconds: Int = queue.getInt("pullEveryNSeconds")
  }
}
