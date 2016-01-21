package main.scala.simple

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import wiii.awa.WebApi

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

/**
 * Basic demonstration of AkkaHTTP with the Actor [[WebApi]]
 */
object Bootstrap extends App with WebApi with LazyLogging {
    implicit val actorSystem: ActorSystem = ActorSystem("SimpleService")
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
            } ~
            path("bytes" / IntNumber) { (sz) =>
                complete {
                    val source = Source(random(sz)).map(x => ByteString(x.getBytes))
                    HttpResponse(entity = HttpEntity.Chunked.fromData(ContentTypes.`application/octet-stream`, source))
                }
            }
        }


    logger.info("starting Service")
    webstart(math ~ rand)

    Await.ready(actorSystem.whenTerminated, Duration.Inf)

    def random(sz: Int) = scala.collection.immutable.Seq.fill(sz)(Random.nextString(1))
}
