package solution2
import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.LazyLogging

/**
 * Solution 2: Uses Actors to calculate the total sum of the discounted costs
 * Creates three different Actors -
 * 1. Supervisor Actor
 * 2. PaymentsCounter Master Actor
 * 3. PaymentsCounter Worker Actor
 */
object ActorMain extends App with LazyLogging {

  val NUM_OF_RETRIES = 5
  val system = ActorSystem("PaymentsCounter")
  logger.info("Payments Actor System Created")


  val supervisorActor = system.actorOf(Supervisor.props(numberOfChildActors = 10, path="lineitems.csv"), "supervisorActor")
  supervisorActor ! "Start"


}
