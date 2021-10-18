package concurrency.streams

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Flow, Source}

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.async.Async.*
import scala.concurrent.{Future, Promise}

object ConnectionInsights2 {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[?] = ActorSystem(Behaviors.empty, "demo")
    import sys.executionContext

    val inboundConnection: Flow[Message[Int], Message[String], NotUsed] = Flow[Message[Int]]
      .map {
        case Message(id, 1) => Message(id, "one")
        case Message(id, 2) => Message(id, "two")
        case Message(id, x) => Message(id, s"number $x")
      }

    val socket: Socket[Int, String] = new Socket(inboundConnection)

    (1 to 100).foreach { x =>
      async(socket.requestResponse(x).onComplete(y => println(x -> y)))
    }
  }

  class Socket[I, O](flow: Flow[Message[I], Message[O], Any])(implicit actorSystem: ActorSystem[?]) {
    private val map                           = new ConcurrentHashMap[UUID, Promise[O]](1024)
    private val (requestQueue, requestStream) = Source.queue[Message[I]](Int.MaxValue).preMaterialize()

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
  }

  case class Message[T](id: UUID, content: T)
}
