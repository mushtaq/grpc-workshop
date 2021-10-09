package pubsub

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class PubSubServiceImplSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers with ScalaFutures {

  val testKit = ActorTestKit()

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  implicit val system: ActorSystem[_] = testKit.system

  val service = new PubSubServiceImpl(system)

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }

  "PubSubServiceImpl" should {
    "reply to single request" in {
      val reply = service.publish(PublishRequest("topic-1", "message-1"))
      reply.futureValue should ===(PublishReply("done"))
    }
  }
}
