package com.github.dedkovva.qs.repository

import org.scalatest.{FreeSpec, Matchers}

class RandomThingsGeneratorSpec extends FreeSpec with Matchers {
  import com.github.dedkovva.qs.Types.CompanyProduct._
  val products = Seq(ELECTRICITY, EV, GAS)
  val gen = new RandomThingsGenerator(products, 8)

  "generate random combinations" in {
    gen.genRandomCombination(0) shouldBe Seq.empty

    val g1 = gen.genRandomCombination(1)
    Seq(Seq(0), Seq(1), Seq(2)).contains(g1) shouldBe true
    g1.length shouldBe 1

    val g2 = gen.genRandomCombination(2)
    Seq(Seq(0, 1), Seq(0, 2), Seq(1, 2)).contains(g2) shouldBe true
    g2.length shouldBe 2

    gen.genRandomCombination(3) shouldBe Seq(0, 1, 2)

    assertThrows[IllegalArgumentException] {
      gen.genRandomCombination(4)
    }

    Range(0, 20).map(_ => gen.genRandomCombination(0)).distinct shouldBe Seq(Seq.empty)

    val cg1 = Range(0, 20).map(_ => gen.genRandomCombination(1)).distinct
    val expected1 = Seq(Seq(0), Seq(1), Seq(2))
    expected1.length shouldBe cg1.length
    expected1.diff(cg1).isEmpty shouldBe true

    val cg2 = Range(0, 20).map(_ => gen.genRandomCombination(2)).distinct
    val expected2 = Seq(Seq(0, 1), Seq(0, 2), Seq(1, 2))
    expected2.length shouldBe cg2.length
    expected2.diff(cg2).isEmpty shouldBe true

    Range(0, 20).map(_ => gen.genRandomCombination(3)).distinct shouldBe Seq(Seq(0, 1, 2))
  }

  "evaluate indices" in {
    gen.evalIndices(0) shouldBe Seq.empty
    gen.evalIndices(1).length shouldBe 1
    gen.evalIndices(2).length shouldBe 2
    gen.evalIndices(3) shouldBe Seq(0, 1, 2)

    val g4 = gen.evalIndices(4)
    g4.take(3) shouldBe Seq(0, 1, 2)
    g4.length shouldBe 4

    val g5 = gen.evalIndices(5)
    g5.take(3) shouldBe Seq(0, 1, 2)
    g5.length shouldBe 5

    Range(0, 20).map(_ => gen.evalIndices(0)).distinct shouldBe Seq(Seq.empty)

    val cg1 = Range(0, 20).map(_ => gen.evalIndices(1)).distinct
    val expected1 = Seq(Seq(0), Seq(1), Seq(2))
    expected1.length shouldBe cg1.length
    expected1.diff(cg1).isEmpty shouldBe true

    val cg2 = Range(0, 20).map(_ => gen.evalIndices(2)).distinct
    val expected2 = Seq(Seq(0, 1), Seq(0, 2), Seq(1, 2))
    expected2.length shouldBe cg2.length
    expected2.diff(cg2).isEmpty shouldBe true

    Range(0, 20).map(_ => gen.evalIndices(3)).distinct shouldBe Seq(Seq(0, 1, 2))

    val cg4 = Range(0,20).map(_ => gen.evalIndices(4)).distinct
    val expected4 = Seq(Seq(0, 1, 2, 0), Seq(0, 1, 2, 1), Seq(0, 1, 2, 2))
    expected4.length shouldBe cg4.length
    expected4.diff(cg4).isEmpty shouldBe true

    val cg5 = Range(0, 50).map(_ => gen.evalIndices(5)).distinct
    val expected5 = Seq(
      Seq(0, 1, 2, 0, 0), Seq(0, 1, 2, 0, 1), Seq(0, 1, 2, 0, 2),
      Seq(0, 1, 2, 1, 0), Seq(0, 1, 2, 1, 1), Seq(0, 1, 2, 1, 2),
      Seq(0, 1, 2, 2, 0), Seq(0, 1, 2, 2, 1), Seq(0, 1, 2, 2, 2)
    )
    expected5.length shouldBe cg5.length
    expected5.diff(cg5).isEmpty shouldBe true
  }

  "evaluate things" in {
    gen.evalThings(0) shouldBe Seq.empty
    gen.evalThings(1).length shouldBe 1
    gen.evalThings(2).length shouldBe 2
    gen.evalThings(3) shouldBe products

    val g4 = gen.evalThings(4)
    g4.length shouldBe 4
    g4.take(3) shouldBe products

    val g5 = gen.evalThings(5)
    g5.length shouldBe 5
    g5.take(3) shouldBe products

    Range(0, 20).map(_ => gen.evalIndices(0)).distinct shouldBe Seq(Seq.empty)

    val cg1 = Range(0, 20).map(_ => gen.evalThings(1)).distinct
    val expected1 = Seq(Seq(ELECTRICITY), Seq(EV), Seq(GAS))
    expected1.length shouldBe cg1.length
    expected1.diff(cg1).isEmpty shouldBe true

    val cg2 = Range(0, 20).map(_ => gen.evalThings(2)).distinct
    val expected2 = Seq(Seq(ELECTRICITY, EV), Seq(ELECTRICITY, GAS), Seq(EV, GAS))
    expected2.length shouldBe cg2.length
    expected2.diff(cg2).isEmpty shouldBe true

    Range(0, 20).map(_ => gen.evalThings(3)).distinct shouldBe Seq(Seq(ELECTRICITY, EV, GAS))

    val cg4 = Range(0,20).map(_ => gen.evalThings(4)).distinct
    val expected4 = Seq(
      Seq(ELECTRICITY, EV, GAS, ELECTRICITY), 
      Seq(ELECTRICITY, EV, GAS, EV), 
      Seq(ELECTRICITY, EV, GAS, GAS))
    expected4.length shouldBe cg4.length
    expected4.diff(cg4).isEmpty shouldBe true

    val cg5 = Range(0, 50).map(_ => gen.evalThings(5)).distinct
    val expected5 = Seq(
      Seq(ELECTRICITY, EV, GAS, ELECTRICITY, ELECTRICITY), 
      Seq(ELECTRICITY, EV, GAS, ELECTRICITY, EV), 
      Seq(ELECTRICITY, EV, GAS, ELECTRICITY, GAS),
      
      Seq(ELECTRICITY, EV, GAS, EV, ELECTRICITY), 
      Seq(ELECTRICITY, EV, GAS, EV, EV), 
      Seq(ELECTRICITY, EV, GAS, EV, GAS),
      
      Seq(ELECTRICITY, EV, GAS, GAS, ELECTRICITY), 
      Seq(ELECTRICITY, EV, GAS, GAS, EV), 
      Seq(ELECTRICITY, EV, GAS, GAS, GAS)
    )
    expected5.length shouldBe cg5.length
    expected5.diff(cg5).isEmpty shouldBe true 
  }

  "substring to int" in {
    gen.substringToInt("123456789") shouldBe 9
    gen.substringToInt("1234567810") shouldBe 10
  }
}
