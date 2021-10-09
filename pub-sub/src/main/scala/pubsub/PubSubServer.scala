package pubsub

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future
import scala.concurrent.duration._

object PubSubServer {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "PubSubServer")
    new PubSubServer(system).run()
  }
}

class PubSubServer(system: ActorSystem[_]) {

  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem[_] = system
    import system.executionContext

    val service: HttpRequest => Future[HttpResponse] =
      PubSubServiceHandler(new PubSubServiceImpl(system))

    val bound: Future[Http.ServerBinding] = Http(system)
      .newServerAt(interface = "0.0.0.0", port = 8080)
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete(println)

    bound
  }
}
