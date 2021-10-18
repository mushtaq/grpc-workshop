package concurrency.streams

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Flow, MergeHub, Sink, Source}

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.async.Async.*
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}

object ConnectionMultiplex {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[?] = ActorSystem(Behaviors.empty, "demo")
    import sys.executionContext

    val slow = Source
      .tick(0.second, 1.second, ())
      .scan(0)((acc, _) => acc + 1)
      .map(x => s"$x")

    val fast = Source
      .tick(0.second, 1.second, ())
      .scan(0)((acc, _) => acc + 1)
      .map(x => s"------------> $x")

    fast
      .merge(slow)
      .groupBy(10, _.startsWith("-"))
      .to(Sink.foreach { x =>
        if (x.startsWith("-")) {
          Thread.sleep(10000)
          println(x)
        } else {
          println(x)
        }
      })
      .run()

    val inboundConnection: Flow[Message[Int], Message[String], NotUsed] = Flow[Message[Int]]
      .map {
        case Message(id, 1) => Message(id, "one")
        case Message(id, 2) => Message(id, "two")
        case Message(id, x) => Message(id, s"number $x")
      }

    val inboundConnection2: Flow[Message[Int], Message[String], NotUsed] = Flow[Message[Int]]
      .flatMapMerge(
        1024,
        {
          case Message(id, 1) => Source.tick(0.second, 1.second, ()).map(_ => Message(id, "one"))
          case Message(id, 2) => Source.tick(0.second, 5.second, ()).map(_ => Message(id, "two"))
          case Message(id, x) => Source.single(Message(id, s"number $x"))
        }
      )

//    val socket: Socket[Int, String] = new Socket(inboundConnection)
//
//    (1 to 100).foreach { x =>
//      async(socket.requestResponse(x).onComplete(y => println(x -> y)))
//    }
  }

  class Socket[I, O](flow: Flow[Message[I], Message[O], Any])(implicit actorSystem: ActorSystem[?]) {
    private val map                           = new ConcurrentHashMap[UUID, Promise[O]](1024)
    private val (requestQueue, requestStream) = Source.queue[Message[I]](Int.MaxValue).preMaterialize()

    private val (mergingSink, source) = MergeHub.source[I].preMaterialize()

    requestStream.via(flow).runForeach { x =>
      val promise = map.remove(x.id)
      promise.trySuccess(x.content)
    }

    def requestResponse(request: I): Future[O] = {
      val promise = Promise[O]()
      val uuid    = UUID.randomUUID()
      map.put(uuid, promise)
      requestQueue.offer(Message(uuid, request))
      promise.future
    }

    def requestStream(request: I) = {
      val uuid = UUID.randomUUID()
      Source.single(Message(uuid, request)).via(flow)
    }

  }

  case class Message[T](id: UUID, content: T)
}
