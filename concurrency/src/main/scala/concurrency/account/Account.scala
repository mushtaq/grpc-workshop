package concurrency.account

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import async.Async.*
import scala.concurrent.Future

object Account {
  class Account {
    private var balance = 0

    def deposit(amount: Int): Unit = synchronized {
      balance += amount
    }

    def withdraw(amount: Int): Unit = synchronized {
      balance -= amount
    }

    def getBalance: Int = synchronized {
      balance
    }
  }
  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[?] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    val account = new Account()

    val resultA = Future.traverse((1 to 10000).toList) { _ =>
      async(account.deposit(100))
    }

    val resultB = Future.traverse((1 to 10000).toList) { _ =>
      async(account.withdraw(100))
    }

    async {
      await(resultA)
      await(resultB)
      println(account.getBalance)
    }

  }
}
