package com.lightbend.example.cluster

import akka.actor.{Actor, ActorLogging, Props}
import com.lightbend.example.cluster.Protocol.{Response, WebRtcRequest}

object TestActor {
  def props(): Props = Props(new TestActor)
}

object Protocol {

  final case class Response(message: String)

  final case class Request(requestId: String)
  final case class WebRtcRequest(message: String)

}

class TestActor extends Actor with ActorLogging {

  import scala.concurrent.duration._

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  @scala.throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info("TestActor starting up")
  }

  override def receive: Receive = {
    case Protocol.Request(requestId) => {
      val delay = scala.util.Random.nextInt(5)
      log.info(s"Request [$requestId] received. Replying in [$delay]")
      context.system.scheduler.scheduleOnce(delay seconds, sender(), Response(s"hello from akka-http. [${self.path.name}]"))
    }

    case m:WebRtcRequest => {
        sender() ! Response(s"Echoed ${m.message} from ${self.path.name}")
    }

    case _ => {
      log.info("Didn't understand")
      sender() ! Response(s"I didn't understand that [${self.path.name}]")
    }
  }
}
