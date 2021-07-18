package solution2

import akka.actor.{Actor, ActorLogging, Props}

import scala.io.Source


object Supervisor{
  def props(numberOfChildActors: Int, path:String) = Props(new Supervisor(numberOfChildActors, path))
}

/**
 * Supervisor Actor Handles the creation of the Master Actor.
 * It also sends each line of the csv file using a loop to the Master Actor, who delegates the task of calculating the
 * discount for each purchase to the Worker Actor.
 * It also handles the Latest Discount Calculation for Each cost and adds the result to a local variables that stores
 * the summed costs.
 * @param numberOfChildActors
 * @param path
 */
class Supervisor(numberOfChildActors: Int, path: String) extends Actor with ActorLogging {

  import PaymentsCounterUtil._

  var totalCost = 0.toFloat
  var totalResponse: Int = 0
  var lenghtOfRows = 400


  override def receive: Receive = {
    case "Start" =>
      val master = context.actorOf(Props[PaymentsCounterMaster], "master")
      master ! Initialize(numberOfChildActors)
      log.info("[Supervisor] Initiated Master")
      val src = Source.fromFile(path)
      val csvLines = src.getLines()
      val header = csvLines.take(1).next
      csvLines.foreach(line => master ! line)
      src.close()

    case cost: Float =>
      totalCost = totalCost + cost
      log.debug(s"[Supervisor actor] I received a reply with cost : $cost")
      if (totalResponse == lenghtOfRows-1) {
        println(s"[Supervisor actor] Final Total Cost is: $totalCost")
        System.exit(0)
      }
      totalResponse = totalResponse + 1
  }
}