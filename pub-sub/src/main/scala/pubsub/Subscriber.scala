package pubsub

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings

object SubscriberP {
  def main(args: Array[String]): Unit = Subscriber.subscribe("subscriber-P")
}

object SubscriberQ {
  def main(args: Array[String]): Unit = Subscriber.subscribe("subscriber-Q")
}

object Subscriber {

  def subscribe(name: String): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "Subscriber")
    import sys.executionContext

    val client = PubSubServiceClient(
      GrpcClientSettings
        .fromConfig("PubSubService")
        .withTls(false)
    )

    client
      .subscribe(SubscribeRequest("topic-1"))
      .runForeach { x =>
        println(s"$name received: ${x.message}")
      }
      .onComplete(println)
  }
}
