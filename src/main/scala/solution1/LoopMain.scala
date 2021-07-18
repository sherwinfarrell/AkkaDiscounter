package solution1
import com.typesafe.scalalogging.LazyLogging
import sharedUtil.DiscountFinder

import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
 * Solution 1: Uses a map to loop through the lines in the lineitems.csv file and calculate and sums the discounted
 * prices.
 * It also uses the DiscountFinder's getDiscount Method to fetch the correct discount from EyeEm's Discounter
 * Microservice
 */
object LoopMain extends App with LazyLogging   {

  val src = Source.fromFile("lineitems.csv").getLines()
  val headerLine = src.take(1).next
  val NUM_OF_RETRIES = 5

  val lineList: List[String] = src.toList

  val costs = lineList.map( line => {

        val lineSplit: List[String] = line.split(",").toList
        val cost = lineSplit(1).toFloat
        val discount = Try(lineSplit(2)) match {
          case Success(disc) => DiscountFinder.getDiscount(disc, NUM_OF_RETRIES)
          case Failure(exception) => 0
        }
        logger.debug(s"Discounted Cost: ${cost - (cost*discount/100)}")
        cost - (cost*discount/100)
    })

  println(f"\n The Final total cost is: ${costs.sum}")
  DiscountFinder.stopActorSystem()
}

