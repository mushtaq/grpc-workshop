package pubsub

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class PubSubSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers with ScalaFutures {

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  val testKit = ActorTestKit()

  val serverSystem: ActorSystem[_] = testKit.system
  val bound                        = new PubSubServer(serverSystem).run()

  // make sure server is bound before using client
  bound.futureValue

  implicit val clientSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "GreeterClient")
  val client                                = PubSubServiceClient(GrpcClientSettings.fromConfig("PubSubService"))

  override def afterAll(): Unit = {
    ActorTestKit.shutdown(clientSystem)
    testKit.shutdownTestKit()
  }

  "PubSubService" should {
    "reply to single request" in {
      val reply = client.publish(PublishRequest("topic-1", "message-1"))
      reply.futureValue should ===(PublishReply("done"))
    }
  }
}
