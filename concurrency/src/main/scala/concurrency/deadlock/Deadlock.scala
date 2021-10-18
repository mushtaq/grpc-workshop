package concurrency.deadlock

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import async.Async.*

object Deadlock {

  class Friend(val name: String) {
    def bow(bower: Friend): Unit = synchronized {
      println(s"$name: ${bower.name} has bowed to me!")
      bower.bowBack(this)
    }

    def bowBack(bower: Friend): Unit = synchronized {
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
