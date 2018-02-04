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
import akka.http.scaladsl.server.{ Directives, Route }
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object HttpEndpoint extends PlayJsonSupport {
  def routes(router: ActorRef, clusterMembership: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route = {
    import Directives._

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
        })
  }
}
