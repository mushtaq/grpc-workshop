package concurrency

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Concat, Flow, GraphDSL, Sink, Source}
import akka.stream.{BoundedSourceQueue, QueueOfferResult, SourceShape}

import scala.concurrent.{Future, Promise}

object StreamBasedActors {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "demo")
    import sys.executionContext

    val behaviour = StreamBehavior
      .receiveMsg[Action] { self =>
        var balance                                       = 0
        var subscribers: List[BoundedSourceQueue[Action]] = List.empty
        x =>
          subscribers.foreach(q => q.offer(x))
          x match {
            case Deposit(amount)  => balance += amount
            case Withdraw(amount) => balance -= amount
            case GetBalance(p)    =>
              p.trySuccess(balance)
              self.offer(Deposit(111))
            case GetEvents(q)     => subscribers ::= q
          }
      }

    val actorRef  = behaviour.spawn()
    val actorRef2 = behaviour.spawn()

    actorRef.tell(Deposit(100))

    List(actorRef, actorRef2).foreach { actorRef =>
      actorRef.subscribe(GetEvents).runForeach(println).onComplete(println)
      actorRef.tell(Deposit(100))
      actorRef.tell(Deposit(1000))
      actorRef.tell(Withdraw(1100))
      actorRef.ask(GetBalance).onComplete(println)
      Thread.sleep(100)
      actorRef.ask(GetBalance).onComplete(println)
      Thread.sleep(100)

      val dd = Future {
        (1 to 100).foreach { _ =>
          actorRef.tell(Deposit(9876))
        }
      }

      val ee = Future {
        (1 to 100).foreach { _ =>
          actorRef.tell(Withdraw(9876))
        }
      }

      dd.flatMap(x => ee).onComplete(x => actorRef.ask(GetBalance).onComplete(println))
    }
  }

  sealed trait Action
  case class Deposit(amount: Int)                           extends Action
  case class Withdraw(amount: Int)                          extends Action
  case class GetBalance(replyTo: Promise[Int])              extends Action
  case class GetEvents(replyTo: BoundedSourceQueue[Action]) extends Action

  implicit class RichQueue[A](queue: BoundedSourceQueue[A]) {
    def subscribe[T](f: BoundedSourceQueue[T] => A): Source[T, QueueOfferResult] = {
      Source.queue[T](Int.MaxValue).mapMaterializedValue(q => queue.offer(f(q)))
    }

    def ask[T](f: Promise[T] => A): Future[T] = {
      val p = Promise[T]()
      queue.offer(f(p))
      p.future
    }

    def tell(action: A): QueueOfferResult = queue.offer(action)
  }

  object StreamBehavior {
    def receiveMsg[O](f: BoundedSourceQueue[O] => O => Unit): Source[O, BoundedSourceQueue[O]] = Source
      .queue[O](Int.MaxValue)
      .receiveMsg(f)
  }

  implicit class RichSource[O, M](source: Source[O, M]) {
    def spawn()(implicit actorSystem: ActorSystem[_]): M = {
      import actorSystem.executionContext
      source.to(Sink.ignore.mapMaterializedValue(_.onComplete(println))).run()
    }

    def receiveMsg(f: M => O => Unit): Source[O, M] = source
      .withMat()
      .flatMapPrefix(1) { xs =>
        val mat = xs.head.asInstanceOf[M]
        Flow[Any].map(_.asInstanceOf[O]).statefulMapConcat { () =>
          val action = f(mat)
          x =>
            action(x)
            List(x)
        }
      }

    def withMat(): Source[Any, M] = {
      import akka.stream.scaladsl.GraphDSL.Implicits.*
      val graph = GraphDSL.createGraph(source) { implicit builder => src =>
        val concat = builder.add(Concat[Any]())
        builder.materializedValue ~> concat.in(0)
        src.out ~> concat.in(1)
        SourceShape(concat.out)
      }
      Source.fromGraph(graph)
    }
  }
}
