package solution2
import akka.actor.{Actor, ActorLogging}
import sharedUtil.DiscountFinder
import solution2.PaymentsCounterUtil
import solution2.PaymentsCounterUtil.{CostCalculatorReply, CostCalculatorTask}

import scala.util.{Failure, Success, Try}

/**
 * The Worker Actor is in charge of Calculating the discount for a particular purchase by using the
 * DiscountFinder's getDiscount method which is in the SharedUtil folder.
 */
class PaymentsCounterWorker extends Actor with ActorLogging {

  import PaymentsCounterUtil._

  override def receive: Receive = {
    case CostCalculatorTask( text) =>
      log.info(s"${self.path} I have received task with $text")
      val lineSplit = text.split(",")
      val cost = lineSplit(1).toFloat
      val discount = Try(lineSplit(2)) match {
        case Success(disc) => DiscountFinder.getDiscount(disc, ActorMain.NUM_OF_RETRIES)
        case Failure(exception) => 0
      }
      val totalCost = cost - (cost * discount / 100)
      println(s"Total Cost acquired from one worked node is: ${totalCost} ")
      sender() ! CostCalculatorReply(totalCost)
  }
}