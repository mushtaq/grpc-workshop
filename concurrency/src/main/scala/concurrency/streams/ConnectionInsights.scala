package concurrency.streams

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.{Future, Promise}

object ConnectionInsights {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[?] = ActorSystem(Behaviors.empty, "demo")
    import sys.executionContext

    val inboundConnection: Flow[Int, String, NotUsed] = Flow[Int].map {
      case 1 => "one"
      case 2 => "two"
      case x => s"number $x"
    }

//    val outboundConnection: Flow[String, Int, NotUsed] =
//      Flow.fromSinkAndSource(Sink.foreach[String](println), Source(1 to 5))

//    outboundConnection.join(inboundConnection).run()

    val socket: Socket[Int, String] = new Socket(inboundConnection)

    (1 to 100).foreach { x =>
      socket.requestResponse(x).onComplete(y => println(x -> y))
    }
  }

  class Socket[I, O](flow: Flow[I, O, Any])(implicit actorSystem: ActorSystem[?]) {
    private val (q, stream)   = Source.queue[I](Int.MaxValue).preMaterialize()
    private val (qP, streamP) = Source.queue[Promise[O]](Int.MaxValue).preMaterialize()

    stream.via(flow).zip(streamP).runForeach { case (response, p) =>
      p.trySuccess(response)
    }

    def requestResponse(request: I): Future[O] = {
      val promise = Promise[O]()
      qP.offer(promise)
      q.offer(request)
      promise.future
    }
  }
}
