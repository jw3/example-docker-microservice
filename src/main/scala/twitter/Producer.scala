package twitter

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import spray.json.DefaultJsonProtocol
import twitter4j._
import wiii.awa.WebHooks

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
 * an example of a producing service that accepts subscribers
 * in this case just tying into twitter to generate some tweets
 */
object Producer extends App with WebHooks with LazyLogging {
    implicit val actorSystem: ActorSystem = ActorSystem("NotificationProducer")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    override def config: Option[Config] = Option(ConfigFactory.load)

    logger.info("starting Notification Service")
    webstart(webhookRoutes)

    val factory = new TwitterStreamFactory()
    val twitterStream = factory.getInstance()
    twitterStream.addListener(new StatusForwarder(this))
    twitterStream.filter(new FilterQuery("espn"))

    Await.ready(actorSystem.whenTerminated, Duration.Inf)
}


class StatusForwarder(hooks: WebHooks)(implicit mat: ActorMaterializer) extends StatusListener {
    def onStatus(status: Status): Unit =
        hooks.post(Notification(status.getText)).foreach(_.onSuccess {
            //!!!\\ consume the response or it will be interpreted as back pressure //!!!\\
            case r => r.entity.dataBytes.runWith(Sink.ignore)
        })


    //\\ nop all the others for now  //\\
    def onStallWarning(warning: StallWarning): Unit = {}
    def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {}
    def onScrubGeo(userId: Long, upToStatusId: Long): Unit = {}
    def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {}
    def onException(ex: Exception): Unit = {}
}

// simple serializable object for statuses
case class Notification(text: String)
object NotificationProtocol extends DefaultJsonProtocol with LazyLogging {
    implicit val notificationFormat = jsonFormat1(Notification)
}
