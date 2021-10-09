package pubsub

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.Source

import scala.concurrent.duration.DurationInt

object PublisherA {
  def main(args: Array[String]): Unit = Publisher.publish("publisher-A")
}

object PublisherB {
  def main(args: Array[String]): Unit = Publisher.publish("publisher-B")
}

object Publisher {
  def publish(name: String): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, name)
    import sys.executionContext

    val client = PubSubServiceClient(
      GrpcClientSettings
        .fromConfig("PubSubService")
        .withTls(false)
    )

    Source
      .tick(1.second, 1.second, "tick")
      .zipWithIndex
      .map { case (_, i) => PublishRequest("topic-1", s"$name -> message-$i") }
      .mapAsync(1)(client.publish)
      .runForeach(x => println(x.message))
      .onComplete(println)
  }
}
