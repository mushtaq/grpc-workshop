package concurrency

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.ExecutionContext

trait StrandEC extends ExecutionContext {
  def stop(): Unit
}

object StrandEC {
  def create()(implicit actorSystem: ActorSystem[?]): StrandEC =
    Source
      .queue[Runnable](Int.MaxValue)
      .mapMaterializedValue { q =>
        new StrandEC {
          override def execute(runnable: Runnable): Unit     = q.offer(runnable)
          override def stop(): Unit                          = q.complete()
          override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
        }
      }
      .map(_.run())
      .to(Sink.ignore)
      .run()
}
