package com.lightbend.example.cluster

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.lightbend.example.cluster.Protocol._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object SignallingCoordinator {
  def props(router: ActorRef): Props = Props(new SignallingCoordinator(router))

}

class SignallingCoordinator(router: ActorRef) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = ExecutionContext.global
  val askTimeout: Timeout = FiniteDuration(500, TimeUnit.MILLISECONDS)
  var handlers: Map[String, ActorRef] = Map.empty[String, ActorRef]
  var setup: Map[String, ActorRef] = Map.empty[String, ActorRef]

  override def receive = {
    case m: SdpOffer => {
      val s = sender()
      setup += (m.cameraId -> s)
      router ! SetupStreamRequest(m.cameraId, m, self)
      //      val future = router.ask(SdpOfferCoordinator(m.cameraId, m.offer, self))(askTimeout)
      //      future.recover {
      //        case ex => {
      //          log.warning("Catching exception", ex)
      ////          Failure(ex)
      //        }
      //      } pipeTo self
      //      future pipeTo self
    }

    case a:SdpAnswer =>
      setup.get(a.cameraId) match {
        case Some(s) => s ! SdpAnswer(a.cameraId, a.answer)
        case None => log.info("Nowhere to send")
      }

    case m: WebRtcMessage =>
      handlers.get(m.cameraId) match {
        case Some(handler) => handler.ask(m)(askTimeout) pipeTo sender()
        case None => sender() ! Error("No handler found")
      }

    case a: SetupStreamResponse => {
      log.info(s"Received answer $a")

      handlers.get(a.cameraId) match {
        case Some(h) => log.info("Ignoring message")
        case None =>
          a.handler ! SetupStream(a.cameraId, a.offer, self)
          handlers += (a.cameraId -> a.handler)
      }
    }

    case u@_ =>
      log.info(s"${u} Not a webRtcMessage from ${sender()}")
    //      sender() ! Error("Not a webRtcMessage")
  }
}
