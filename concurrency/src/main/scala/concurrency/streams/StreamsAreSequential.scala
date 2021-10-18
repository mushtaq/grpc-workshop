package concurrency.streams

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

object StreamsAreSequential {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[?] = ActorSystem(Behaviors.empty, "demo")
    import sys.executionContext

    var balance = 0
    val source  = Source(1 to 1000000)
    val sink    = Sink.foreach[Int](_ => balance - 100)
    val flow    = Flow.fromSinkAndSource(sink, source)
    source.via(flow).toMat(sink)(Keep.right).run().onComplete { x =>
      println(x)
      println(balance)
    }
  }

}
