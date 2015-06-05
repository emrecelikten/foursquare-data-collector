package datacollector

import akka.actor.{ Actor, ActorPath, Props, Terminated }
import akka.event.Logging
import akka.routing.{ ActorRefRoutee, Router, SmallestMailboxRoutingLogic }

/**
 * Router for multiple TwitterProcessorActor workers. Receives processing tasks and delegates them to children.
 *
 * @author Emre Çelikten
 */
class TwitterProcessorRouter(unprocessedSaverActorPath: ActorPath, processedSaverActorPath: ActorPath, numWorkers: Int = 5) extends Actor {
  private var router = {
    val workers = Vector.fill(numWorkers) {
      val r = context.actorOf(TwitterProcessorActor.props(unprocessedSaverActorPath, processedSaverActorPath))
      context watch r
      ActorRefRoutee(r)
    }
    Router(SmallestMailboxRoutingLogic(), workers)
  }

  private val logger = Logging(context.system, this)
  logger.info("ProcessorRouter ready with routees.")

  override def receive: Receive = {
    case msg: TweetMessage =>
      router.route(msg, sender())
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(TwitterProcessorActor.props(unprocessedSaverActorPath, processedSaverActorPath))
      context watch r
      router = router.addRoutee(r)
    case other =>
      logger.warning(s"Invalid message received from $sender:\n$other")
  }
}

object TwitterProcessorRouter {
  def props(unprocessedSaverActorPath: ActorPath, processedSaverActorPath: ActorPath): Props =
    Props(new TwitterProcessorRouter(unprocessedSaverActorPath, processedSaverActorPath))
}
