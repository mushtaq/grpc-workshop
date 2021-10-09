package pubsub

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.BoundedSourceQueue
import akka.stream.scaladsl.{BroadcastHub, Keep, Source}

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.Future

class PubSubServiceImpl(system: ActorSystem[_]) extends PubSubService {
  private implicit val sys: ActorSystem[_] = system

  private val subscriptionMap = new ConcurrentHashMap[String, Subscription](1024)

  def createSubscription(): Subscription                                        =
    Source
      .queue[PublishRequest](1024)
      .map(request => SubscribeReply(request.message))
      .toMat(BroadcastHub.sink)(Keep.both)
      .mapMaterializedValue { case (q, hub) => Subscription(q, hub) }
      .run()

  override def publish(in: PublishRequest): Future[PublishReply]                = {
    val subscription = subscriptionMap.computeIfAbsent(in.topic, _ => createSubscription())
    val string       = subscription.queue.offer(in).toString
    Future.successful(PublishReply(string))
  }

  override def subscribe(in: SubscribeRequest): Source[SubscribeReply, NotUsed] = {
    val subscription = subscriptionMap.computeIfAbsent(in.topic, _ => createSubscription())
    subscription.outboundHub
  }
}

case class Subscription(queue: BoundedSourceQueue[PublishRequest], outboundHub: Source[SubscribeReply, NotUsed])
