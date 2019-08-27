package com.github.dedkovva.qs

import com.github.dedkovva.qs.Types.CustomerIds
import org.scalatest.{FreeSpec, Matchers}

class UtilSpec extends FreeSpec with Matchers {
  "check id" in {
    Util.checkIds(Seq("aaa", "1", "123456789", "023456789", "-23456789")) shouldBe
      CustomerIds(Seq("123456789", "023456789"), Seq("aaa", "1", "-23456789"))
  }
}
