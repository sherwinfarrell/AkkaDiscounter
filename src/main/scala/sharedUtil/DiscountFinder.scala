package sharedUtil

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

case class Discount(name: String, discount: Float)

trait DiscountJsonProtocol extends DefaultJsonProtocol{
  implicit val discountFormat = jsonFormat2(Discount)
}


/* Functionality to make and handle request to the EyeEm Discounter Web App */
object DiscountFinder extends DiscountJsonProtocol with SprayJsonSupport with LazyLogging  {

  // Initialization of the actor System and the Materializer for Akka Streams used for making Akka HTTP Requests
  implicit val requestActorSystem = ActorSystem("PaymentsAppCounter")
  implicit val materializer = ActorMaterializer()
  import requestActorSystem.dispatcher

  /**
   * Function for sending requests to the EyeEm Discounter and Handle Failure Responses Like Too many Requests, No Discount Found
    Internal Server Failure.
   * Takes in the Discount Code and the Number of Retries for Failed Responses
   * Returns the
  */

  def getDiscount(discountCode: String, retries: Int): Float = {

    /**
     * Takes the discount Code and Fetches the value of the discount form the EyeEm Discounter Microserivce
     * Returns a Furture of HttpResponse
     * */
    def getResponse(disc:String) = {
      Http().singleRequest(HttpRequest(uri = f"http://localhost:9000/api/discounts/${disc}"))
    }

    /**
     * Tail Recursive Function, that gets the HttpResponse from getResponse and handles failure conditions using recursion and match case
     * @param disc
     * @param n
     * @param fn
     * @return Option[Float]
     */
    @annotation.tailrec
    def retry(disc:String, n: Int)(fn:String => Future[HttpResponse]): Option[Float] = {
      import spray.json._

      Try(fn(disc)) match {
        case Success(x) =>
          val awaitedX = Await.result(x,100.seconds)
          awaitedX match {
            case response@(HttpResponse(StatusCodes.OK, _, _, _)) =>
              val strictEntityFuture = response.entity.toStrict(2.seconds)
              val discountFuture = Try(strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[Discount]))
              discountFuture match {
                case Success(value) => Some(Await.result(value, 100.seconds).discount)
                case Failure(exception) => logger.error(s"Failure Decoding Json String: ${exception.toString}")
                  None
              }
            case response@HttpResponse(StatusCodes.NotFound , _, _, _) => None
            case response@HttpResponse(StatusCodes.InternalServerError, _, _, _) if n>=1 => Thread.sleep( 500)
              retry(disc, n - 1)(fn)
            case response@HttpResponse(StatusCodes.TooManyRequests, _, _, _) if n>=1 => Thread.sleep( 500)
              retry(disc, n - 1)(fn)
            case _ if n>=1  => retry(disc, n - 1)(fn)
            case _ =>None
          }

      }
    }

    val response = retry(discountCode,retries)(getResponse)
    response.getOrElse(1)
  }

  /**
   * Terminates the Akka System
   * @return
   */
  def stopActorSystem() = {
    requestActorSystem.terminate()
  }
}
