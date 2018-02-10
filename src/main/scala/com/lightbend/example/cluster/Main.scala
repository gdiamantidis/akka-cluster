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
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.settings.RoutingSettings
import akka.routing.ScatterGatherFirstCompletedPool
import akka.stream.ActorMaterializer

import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    val httpHost =
      sys.props.getOrElse(
        "httpHost",
        throw new IllegalArgumentException("HTTP bind host must be defined by the httpHost property"))

    val httpPort =
      sys.props
        .get("httpPort")
        .map(_.toInt)
        .getOrElse(throw new IllegalArgumentException("HTTP bind port must be defined by the httpPort property"))

    val clusterMembershipAskTimeout =
      sys.props
        .get("clusterMembershipAskTimeout")
        .map(v => FiniteDuration(v.toLong, TimeUnit.MILLISECONDS))
        .getOrElse(throw new IllegalArgumentException("ClusterMembership ask timeout must be defined by the clusterMembershipAskTimeout property"))

    val actorSystemName =
      sys.props.getOrElse(
        "akkaActorSystemName",
        throw new IllegalArgumentException("Actor system name must be defined by the actorSystemName property"))

    implicit val actorSystem = ActorSystem(actorSystemName)
    implicit val mat = ActorMaterializer()
    import actorSystem.dispatcher
    implicit val http = Http(actorSystem)
    implicit val routingSettings = RoutingSettings(actorSystem)

    val clusterMembership = actorSystem.actorOf(ClusterMembership.props, ClusterMembership.Name)

    lazy val router: ActorRef = actorSystem.actorOf(
      ClusterRouterPool(
        new ScatterGatherFirstCompletedPool(6, 500 seconds),
        new ClusterRouterPoolSettings(10, 10, true, useRoles = Set.empty[String])).props(SignalingActor.props()), "signalling-" + UUID.randomUUID().toString)

    val signallingCoordinator: ActorRef = actorSystem.actorOf(SignallingCoordinator.props(router), "coordinator-" +UUID.randomUUID().toString)

    val route = HttpEndpoint.routes(router, signallingCoordinator, clusterMembership, clusterMembershipAskTimeout)

    http.bindAndHandle(route, httpHost, httpPort)
  }
}
