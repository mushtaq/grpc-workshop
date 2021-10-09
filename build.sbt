lazy val akkaVersion     = "2.6.16"
lazy val akkaHttpVersion = "10.2.6"
lazy val akkaGrpcVersion = "2.1.0"

lazy val sharedSettings      = List(
  version      := "1.0",
  scalaVersion := "2.13.6",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http"          % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor-typed"   % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"        % akkaVersion,
    "com.typesafe.akka" %% "akka-discovery"     % akkaVersion,
    "com.typesafe.akka" %% "akka-pki"           % akkaVersion,

    // The Akka HTTP overwrites are required because Akka-gRPC depends on 10.1.x
    "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http2-support"       % akkaHttpVersion,
    "ch.qos.logback"     % "logback-classic"          % "1.2.3",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit"      % akkaVersion % Test,
    "org.scalatest"     %% "scalatest"                % "3.1.1"     % Test
  )
)

lazy val `grpc-workshop`     = project
  .in(file("."))
  .aggregate(`sample-quickstart`, `pub-sub`)

lazy val `sample-quickstart` = project
  .enablePlugins(AkkaGrpcPlugin)
  .settings(sharedSettings)

lazy val `pub-sub`           = project
  .enablePlugins(AkkaGrpcPlugin)
  .settings(sharedSettings)
