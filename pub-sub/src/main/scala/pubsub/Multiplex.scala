package pubsub

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.Source

import scala.concurrent.duration.DurationInt

object Multiplex {
  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "multiplex")
    import sys.executionContext

    val client = PubSubServiceClient(
      GrpcClientSettings
        .fromConfig("PubSubService")
        .withTls(false)
    )

    client.slow(SlowRequest("slow")).runForeach(println).onComplete(println)
    client.slow(SlowRequest("slow")).runForeach(println).onComplete(println)
    client.fast(FastRequest("fast")).throttle(1, 10.seconds).runForeach(println).onComplete(println)
    client.fast(FastRequest("fast")).throttle(1, 10.seconds).runForeach(println).onComplete(println)
  }
}
