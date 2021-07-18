package  solution2
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import solution2.PaymentsCounterWorker

object PaymentsCounterUtil {

  case class Initialize(nChildren: Int)

  case class CostCalculatorTask(text: String)

  case class CostCalculatorReply( cost: Float)

  case object TotalCost

}

/**
 * The Master Actor is in charge of creating Worker Actors as well as saving their refs in a List in its context.
 * It is also in charge of delegating tasks, for discount calculation, to Worker Actors.
 * This is done using Robin Robin Load Task Distribution by storing the current index of the worker actor ref in
 * its context and uses simple modulus calculation to loop through the different worker actors in a circular fashion.
 * It also stores the reference of the Supervisor, so that the result of the discount calculation can be sent back.
 */
class PaymentsCounterMaster extends Actor with ActorLogging {

  import PaymentsCounterUtil._

  override def receive: Receive = {
    case Initialize(nChildren) =>
      log.info("[master] initializing...")
      val childrenRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[PaymentsCounterWorker], s"childPaymentsActors$i")
      log.info("[master] Initiated The Child Workers")
      context.become(withChildren(childrenRefs, 0, sender()))
  }

  def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int,  originalSender: ActorRef): Receive = {
    case text: String =>
      log.info(s"[master] I have received: $text - I will send it to child $currentChildIndex")
      val childRef = childrenRefs(currentChildIndex)
      childRef ! CostCalculatorTask(text)
      val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
      context.become(withChildren(childrenRefs, nextChildIndex, originalSender ))
    case CostCalculatorReply(cost) =>
      log.info(s"[master] I have received a reply with $cost")
      originalSender ! cost
      context.become(withChildren(childrenRefs, currentChildIndex, originalSender))
  }
}