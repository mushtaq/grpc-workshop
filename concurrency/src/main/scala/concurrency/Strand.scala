package concurrency

import akka.actor.typed.ActorSystem

abstract class Strand(implicit actorSystem: ActorSystem[?]) {
  final protected implicit lazy val strandEC: StrandEC = StrandEC.create()
  def stop(): Unit                                     = strandEC.stop()
}
