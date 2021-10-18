package concurrency.account

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import concurrency.{Strand, StrandEC}

import scala.async.Async.*
import scala.concurrent.Future

object AccountAsync {
  class Account(implicit actorSystem: ActorSystem[?]) extends Strand {
    private var balance = 0

    def deposit(amount: Int): Future[Unit] = async {
      balance += amount
    }

    def withdraw(amount: Int): Future[Unit] = async {
      balance -= amount
    }

    def getBalance: Future[Int] = async {
      balance
    }
  }

  def main(args: Array[String]): Unit = {
    implicit lazy val actorSystem: ActorSystem[?] = ActorSystem(Behaviors.empty, "demo")
    import actorSystem.executionContext

    val account = new Account()

    val resultA = Future.traverse((1 to 10000).toList) { _ =>
      account.deposit(100)
    }

    val resultB = Future.traverse((1 to 10000).toList) { _ =>
      account.withdraw(100)
    }

    async {
      await(resultA)
      await(resultB)
      println(await(account.getBalance))
    }

  }
}
