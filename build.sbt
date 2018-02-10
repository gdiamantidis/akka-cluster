import sbt._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences.AlignSingleLineCaseStatements
import com.typesafe.sbt.packager.docker._

lazy val akkaVersion = "2.5.4"

name := "akka-cluster"
scalaVersion := "2.12.3"

enablePlugins(JavaServerAppPackaging)

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

libraryDependencies ++= List(
  "com.typesafe.akka" %% "akka-actor"          % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster"        % akkaVersion,
  "com.typesafe.akka" %% "akka-http"           % "10.0.10",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.19.0-M2",
  "com.typesafe.play" %% "play-json"           % "2.6.5",
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "org.json4s" %% "json4s-jackson" % "3.5.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

  "com.typesafe.akka" %% "akka-http" % "10.0.9",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.9",
  "org.scala-lang" % "scala-library" % "2.11.7",

  "org.scalatest"     %% "scalatest"           % "3.0.1"     % "test",
  "com.typesafe.akka" %% "akka-testkit"        % akkaVersion % "test"
)

dockerEntrypoint ++= Seq(
  """-DakkaActorSystemName="$AKKA_ACTOR_SYSTEM_NAME"""",
  """-Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")"""",
  """-Dakka.remote.netty.tcp.port="$AKKA_REMOTING_BIND_PORT"""",
  """$(IFS=','; I=0; for NODE in $AKKA_SEED_NODES; do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://$AKKA_ACTOR_SYSTEM_NAME@$NODE"; I=$(expr $I + 1); done)""",
  "-Dakka.io.dns.resolver=async-dns",
  "-Dakka.io.dns.async-dns.resolve-srv=true",
  "-Dakka.io.dns.async-dns.resolv-conf=on",
  """-DhttpHost="$HTTP_HOST"""",
  """-DhttpPort="$HTTP_PORT"""",
  """-DclusterMembershipAskTimeout="$CLUSTER_MEMBERSHIP_ASK_TIMEOUT""""
)
dockerCommands :=
  dockerCommands.value.flatMap {
    case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
    case v => Seq(v)
  }
dockerRepository := Some("gdiamantidis")
dockerUpdateLatest := true
dockerBaseImage := "gdiamantidis/openjdk-jre-8-bash"
version in Docker := "0.2"

ScalariformKeys.preferences :=
  ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
