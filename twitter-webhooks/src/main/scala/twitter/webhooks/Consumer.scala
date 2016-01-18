package twitter.webhooks

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import spray.json.DefaultJsonProtocol
import wiii.awa._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


/**
 * a consumer of the notifications the producer provides
 * 1) subscribe
 * 2) consume
 */
object Consumer extends App with WebApi with Directives with SprayJsonSupport with LazyLogging {
    import DefaultJsonProtocol._
    implicit val simpleStatus = jsonFormat1(ConsumedNotification)

    val serverPort = 2222
    val clientPort = 9999

    implicit val actorSystem: ActorSystem = ActorSystem("NotificationConsumer")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    override def config: Option[Config] = Option(ConfigFactory.parseString(s"{webapi.port=$clientPort}"))

    logger.info("starting the client")

    val callback =
        path("status") {
            (put & entity(as[ConsumedNotification])) { s =>
                complete {
                    logger.info(s"notification\n[ ${s.text} ]")
                    StatusCodes.OK
                }
            }
        }
    webstart(callback)


    /////////////////
    // subscribe to the producer service
    val hookServer = s"http://localhost:$serverPort/subscribe"
    val subscription =
        s"""{
            |"host":"http://localhost",
            |"port":$clientPort,
            |"path":"status",
            |"body":"{ \\"text\\":\\"{{text}}\\" }"
            |}""".stripMargin.stripLineEnd

    Http().singleRequest(HttpRequest(PUT, Uri(hookServer), entity = HttpEntity(`application/json`, subscription)))
    //
    /////////////////


    Await.ready(actorSystem.whenTerminated, Duration.Inf)
}

// modeling a disconnect between the producer and consumer service
case class ConsumedNotification(text: String)