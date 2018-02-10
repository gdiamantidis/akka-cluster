package com.gdiama.example.cluster

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import Protocol._

object SignalingActor {
  def props(): Props = Props(new SignalingActor)
}

object Protocol {

  sealed trait SignallingMessage

  abstract class WebRtcMessage extends SignallingMessage {
    def cameraId: String
  }

  case class SdpOffer(cameraId: String, offer: String) extends WebRtcMessage
  case class SdpAnswer(cameraId: String, answer: String) extends WebRtcMessage
  case class SetupStreamRequest(cameraId: String, offer: SdpOffer, coordinator: ActorRef)
  case class SetupStreamResponse(cameraId: String, offer: SdpOffer, handler: ActorRef)
  case class SetupStream(cameraId: String,  offer: SdpOffer, coordinator: ActorRef)
  case class IceCandidate(cameraId: String, candidate: String) extends WebRtcMessage
  final case class TempMessage(cameraId: String, message: String) extends WebRtcMessage
  case class Error(errorMessage: String) extends SignallingMessage

}

class SignalingActor extends Actor with ActorLogging {

  var coordinator: ActorRef = _
  var cameraId: String = _

  @scala.throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info("Signalling actor starting up")
  }

  def busy: Receive = {
    case m: SetupStreamRequest => {
      log.info(s"Already handling camera $coordinator")
    }

    case i: IceCandidate => {
      val message = s"Actor [${self.path.name}] got iceCandidate: $i"
      log.info(message)
      sender() ! TempMessage(i.cameraId, message)
    }

    // temp for http get /hello
    case TempMessage(x, y) => sender() ! TempMessage(self.path.name, s"hello from ${self.path.name}")

    case _ => {
      log.info("Didn't understand")
      sender() ! Error(s"I didn't understand that [${self.path.name}]")
    }
  }

  def processOffer(offer: SdpOffer): SdpAnswer = SdpAnswer(offer.cameraId, s"Answer from ${self.path.name}")

  def ready: Receive = {
    case m: SetupStreamRequest => {
      m.coordinator ! SetupStreamResponse(cameraId = m.cameraId, SdpOffer(m.cameraId, s"Returning my answer for ${m.offer} (from ${self.path.name})"), handler = self)
    }

    case m: SetupStream => {
      coordinator = m.coordinator
      cameraId = m.cameraId
      coordinator ! processOffer(m.offer)
      context.become(busy)
    }

    // temp for http get /hello
    case TempMessage(x, y) => sender() ! TempMessage(self.path.name, s"hello from ${self.path.name}")

    case u @ _ => {
      log.info(s"Didn't understand $u")
      sender() ! Error(s"I didn't understand that [${self.path.name}]")
    }
  }

  override def receive: Receive = ready
}
