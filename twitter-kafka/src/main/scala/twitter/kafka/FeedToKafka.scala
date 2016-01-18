package twitter.kafka

import java.util

import akka.actor._
import akka.stream.actor.ActorSubscriber
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.softwaremill.react.kafka.{ProducerMessage, ProducerProperties, ReactiveKafka}
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import spray.json.{DefaultJsonProtocol, _}
import twitter.kafka.PrivateFeedToKafka.{Tweet, TweetSerializer}
import twitter4j.{Status, _}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * twitter to kafka microservice example
 * todo;; externalize configuration
 */
object FeedToKafka extends App with LazyLogging {
    implicit val actorSystem: ActorSystem = ActorSystem("FeedToKafka")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val topicName = "test_topic"

    val subscriberProps = new ReactiveKafka().producerActorProps(ProducerProperties(
        bootstrapServers = "localhost:9092",
        topic = topicName,
        valueSerializer = TweetSerializer
    ))
    val subscriber = actorSystem.actorOf(subscriberProps)
    val (actorRef, publisher) = Source.actorRef[Status](1000, OverflowStrategy.fail).toMat(Sink.asPublisher(false))(Keep.both).run()

    val factory = new TwitterStreamFactory()
    val twitterStream = factory.getInstance()
    twitterStream.addListener(new StatusForwarder(actorRef))
    twitterStream.filter(new FilterQuery("espn"))

    Source.fromPublisher(publisher).map(s => ProducerMessage(Tweet(s.getUser.getName, s.getText)))
    .runWith(Sink.fromSubscriber(ActorSubscriber[ProducerMessage[Array[Byte], Tweet]](subscriber)))

    Await.ready(actorSystem.whenTerminated, Duration.Inf)
    // read a twitter feed writing to kafka topic
}

class StatusForwarder(publisher: ActorRef) extends StatusListener {
    def onStatus(status: Status): Unit = publisher ! status

    //\\ nop all the others for now  //\\
    def onStallWarning(warning: StallWarning): Unit = {}
    def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {}
    def onScrubGeo(userId: Long, upToStatusId: Long): Unit = {}
    def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {}
    def onException(ex: Exception): Unit = {}
}

private[kafka] object PrivateFeedToKafka {
    case class Tweet(user: String, text: String)

    object TweetProtocol extends DefaultJsonProtocol {
        implicit val currencyRteFormat = jsonFormat2(Tweet)
    }

    object TweetSerializer extends Serializer[Tweet] {
        import TweetProtocol._

        override def serialize(s: String, t: Tweet): Array[Byte] =
            t.toJson.compactPrint.getBytes("UTF-8")

        override def close(): Unit = {}
        override def configure(map: util.Map[String, _], b: Boolean): Unit = {}
    }

    object TweetDeserializer extends Deserializer[Tweet] {
        import TweetProtocol._

        override def deserialize(s: String, bytes: Array[Byte]): Tweet =
            new String(bytes, "UTF-8").parseJson.convertTo[Tweet]


        override def close(): Unit = {}
        override def configure(map: util.Map[String, _], b: Boolean): Unit = {}
    }
}