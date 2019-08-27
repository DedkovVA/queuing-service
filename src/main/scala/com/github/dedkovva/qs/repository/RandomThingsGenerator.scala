package com.github.dedkovva.qs.repository

import scala.util.Random

class RandomThingsGenerator[T](things: Seq[T], beginIndexInStr: Int) {
  private val thingIndices = things.indices

  private[dedkovva] def genRandomCombination(numOfElements: Int): Seq[Int] = {
    val maxNum = things.length
    require(numOfElements >= 0 && numOfElements <= maxNum)
    val combinations = Range(0, maxNum).combinations(numOfElements).toSeq
    val randomIndex = Random.nextInt(combinations.length)
    combinations(randomIndex)
  }

  private[dedkovva] def evalIndices(n: Int): Seq[Int] = {
    require(n >= 0)
    val len = things.length
    n match {
      case _ if n == len => thingIndices
      case 0 => Seq.empty
      case 1 => Seq(Random.nextInt(len))
      case _ if n < len => genRandomCombination(n)
      case _ =>
        thingIndices ++ Range(0, n - len).map(_ => Random.nextInt(len))
    }
  }

  private[dedkovva] def substringToInt(str: String): Int = {
    str.substring(beginIndexInStr).toInt
  }

  private[dedkovva] def evalThings(num: Int): Seq[T] = {
    evalIndices(num).map(i => things(i))
  }

  private[dedkovva] def evalThings(str: String): Seq[T] = {
    val num = substringToInt(str)
    evalThings(num)
  }

  def evalThings(strings: Seq[String]): Map[String, Seq[T]] = {
    strings.map(s => s -> evalThings(s)).toMap
  }
}
