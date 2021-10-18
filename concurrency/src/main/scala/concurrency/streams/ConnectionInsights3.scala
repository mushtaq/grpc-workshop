package concurrency.streams

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Flow, Source}

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import async.Async.*

object ConnectionInsights3 {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[?] = ActorSystem(Behaviors.empty, "demo")
    import sys.executionContext

    val inboundConnection: Flow[Message[Int], Message[String], NotUsed] = Flow[Message[Int]].map {
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
    private val (q, stream)   = Source.queue[Message[I]](Int.MaxValue).preMaterialize()
    private val (qP, streamP) = Source.queue[Action[O]](Int.MaxValue).preMaterialize()

    stream.via(flow).runForeach { x =>
      qP.offer(Action.ResponseReceived(x.id, x.content))
    }

    streamP
      .statefulMapConcat { () =>
        val map: mutable.Map[UUID, Promise[O]] = mutable.Map.empty
        x =>
          x match {
            case Action.RequestSent(id, p)             => map.put(id, p)
            case Action.ResponseReceived(id, response) => map.remove(id).get.trySuccess(response)
          }
          List(x)
      }
      .runForeach(_ => ())

    def requestResponse(request: I): Future[O] = {
      val promise = Promise[O]()
      val uuid    = UUID.randomUUID()
      q.offer(Message(uuid, request))
      qP.offer(Action.RequestSent(uuid, promise))
      promise.future
    }
  }

  case class Message[T](id: UUID, content: T)

  sealed trait Action[T]
  object Action {
    case class RequestSent[T](id: UUID, p: Promise[T])    extends Action[T]
    case class ResponseReceived[T](id: UUID, response: T) extends Action[T]
  }
}
