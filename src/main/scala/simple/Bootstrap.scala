package simple

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import wiii.awa.WebApi

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 *
 */
object Bootstrap extends App with WebApi with LazyLogging {
    implicit val actorSystem: ActorSystem = ActorSystem("ServiceB")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    override def config: Option[Config] = Option(ConfigFactory.parseString("webapi.port=2222,webapi.host=0.0.0.0"))

    val math =
        (get & pathPrefix("math")) {
            path("add" / IntNumber / IntNumber) { (l, r) =>
                complete( s"""{"result":"${l + r}"}""")
            }
        }

    logger.info("starting Service")
    webstart(math)

    Await.ready(actorSystem.whenTerminated, Duration.Inf)
}
