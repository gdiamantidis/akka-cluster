/*
 * Copyright 2017 Lightbend, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.example.cluster

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.lightbend.example.cluster.Protocol.{Response, WebRtcRequest}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object HttpEndpoint extends PlayJsonSupport {


  def routes(router: ActorRef, clusterMembership: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route = {
    import Directives._
    implicit val jsonSerializer = JsonSerializer.responseJsonSerializer

    def handler: Flow[Message, Message, Any] = {
      val query: String => Future[Message] =
        s => router.ask(WebRtcRequest(s))(askTimeout).mapTo[Response].map(r => TextMessage.Strict(r.message))

      var flow: Flow[Message, Message, _] =
        Flow[Message].map {
          case tm: TextMessage.Strict => tm.getStrictText
        }.mapAsync[Message](1)(query)

      flow
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
                implicit val jsonSerializer = JsonSerializer.responseJsonSerializer
                router.ask(Protocol.Request(UUID.randomUUID().toString))(askTimeout)
                  .mapTo[Protocol.Response]
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
