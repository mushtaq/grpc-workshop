package pubsub

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.BoundedSourceQueue
import akka.stream.scaladsl.{BroadcastHub, Keep, Source}

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class PubSubServiceImpl(system: ActorSystem[_]) extends PubSubService {
  private implicit val sys: ActorSystem[_] = system

  private val subscriptionMap = new ConcurrentHashMap[String, Subscription](1024)

  def createSubscription(): Subscription =
    Source
      .queue[PublishRequest](1024)
      .map(request => SubscribeReply(request.message))
      .toMat(BroadcastHub.sink)(Keep.both)
      .mapMaterializedValue { case (q, hub) => Subscription(q, hub) }
      .run()

  override def publish(in: PublishRequest): Future[PublishReply] = {
    val subscription = subscriptionMap.computeIfAbsent(in.topic, _ => createSubscription())
    val string       = subscription.queue.offer(in).toString
    Future.successful(PublishReply(string))
  }

  override def subscribe(in: SubscribeRequest): Source[SubscribeReply, NotUsed] = {
    val subscription = subscriptionMap.computeIfAbsent(in.topic, _ => createSubscription())
    subscription.outboundHub
  }

  override def slow(in: SlowRequest): Source[SlowReply, NotUsed] = {
    Source
      .tick(0.second, 1.second, ())
      .scan(0)((acc, _) => acc + 1)
      .map(x => SlowReply(s"------------> $x"))
      .mapMaterializedValue(_ => NotUsed)
  }

  override def fast(in: FastRequest): Source[FastReply, NotUsed] = {
    Source
      .tick(0.second, 100.millis, ())
      .scan(0)((acc, _) => acc + 1)
      .map(x => FastReply(s"$x"))
      .mapMaterializedValue(_ => NotUsed)
  }
}

case class Subscription(queue: BoundedSourceQueue[PublishRequest], outboundHub: Source[SubscribeReply, NotUsed])
