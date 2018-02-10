package com.gdiama.example.cluster

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.gdiama.example.cluster.Protocol.WebRtcMessage
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{JsResult, JsSuccess, Json}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object HttpEndpoint extends PlayJsonSupport {

  def routes(router: ActorRef, coordinator: ActorRef, clusterMembership: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route = {
    import Directives._
    implicit val webRtcFmt = JsonSerializer.webRtcFmt

    def handler: Flow[Message, Message, Any] = {
      val query: String => Future[Message] =
        payload => {
          val value: JsResult[WebRtcMessage] = Json.fromJson[WebRtcMessage](Json.parse(payload))
          value match {
            case v: JsSuccess[WebRtcMessage] =>
              coordinator.ask(v.value)(askTimeout)
                .mapTo[WebRtcMessage]
                .map(r => TextMessage.Strict(Json.toJson(r).toString()))
            case _ => Future.successful(TextMessage.Strict("error"))
          }
        }

      Flow[Message].map {
        case tm: TextMessage.Strict => tm.getStrictText
      }.mapAsync[Message](1)(query)
    }


    Route.seal(
      path("members") {
        pathEndOrSingleSlash {
          get {
            complete {
              implicit val jsonSerializer = JsonSerializer.membershipInfoJsonSerializer
              clusterMembership.ask(ClusterMembership.GetMembershipInfo)(askTimeout)
                .mapTo[ClusterMembership.MembershipInfo]
            }
          }
        }
      } ~
        path("hello") {
          pathEndOrSingleSlash {
            get {
              complete {
//                implicit val jsonSerializer = JsonSerializer.tempFmt
                router.ask(Protocol.TempMessage(UUID.randomUUID().toString, ""))(askTimeout)
                  .mapTo[Protocol.TempMessage]
              }
            }
          }
        } ~
        path("livestream") {
          handleWebSocketMessages(handler)
        }
    )
  }
}
