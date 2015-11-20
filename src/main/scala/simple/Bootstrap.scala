package simple

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import wiii.awa.WebApi

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

/**
 *
 */
object Bootstrap extends App with WebApi with LazyLogging {
    implicit val actorSystem: ActorSystem = ActorSystem("ServiceB")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    override def config: Option[Config] = Option(actorSystem.settings.config)

    val math =
        (get & pathPrefix("math")) {
            path("add" / IntNumber / IntNumber) { (l, r) =>
                complete( s"""{"result":"${l + r}"}""")
            }
        }

    val rand =
        (get & pathPrefix("random")) {
            path("int") {
                complete( s"""{"result":"${Random.nextInt}"}""")
            } ~
            path("posint" / IntNumber) { (u) =>
                val num = Random.nextInt(u - 1) + 1
                complete( s"""{"result":"$num"}""")
            }
        }


    logger.info("starting Service")
    webstart(math ~ rand)

    Await.ready(actorSystem.whenTerminated, Duration.Inf)
}
