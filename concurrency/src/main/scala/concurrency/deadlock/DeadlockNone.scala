package concurrency.deadlock

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import concurrency.{Strand, StrandEC}

import scala.async.Async.{async, await}
import scala.concurrent.Future

object DeadlockNone {
  class Friend(val name: String)(implicit actorSystem: ActorSystem[?]) extends Strand {
    def bow(bower: Friend): Future[Unit] = async {
      println(s"$name: ${bower.name} has bowed to me!")
      await(bower.bowBack(this))
    }

    def bowBack(bower: Friend): Future[Unit] = async {
      println(s"$name: ${bower.name} has bowed back to me!")
    }
  }

  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[?] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    val alphonse = new Friend("Alphonse")
    val gaston   = new Friend("Gaston")

    async(alphonse.bow(gaston))
    async(gaston.bow(alphonse))
  }
}
