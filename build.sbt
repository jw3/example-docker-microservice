enablePlugins(JavaAppPackaging)

lazy val commonSettings = Seq(
    organization := "com.github.jw3.examples",
    scalaVersion := "2.11.7",
    scalacOptions += "-target:jvm-1.8",
    resolvers += "jw3 at bintray" at "https://dl.bintray.com/jw3/maven",
    licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    publishMavenStyle := true,

    dockerBaseImage := "anapsix/alpine-java:jre8",
    dockerExposedPorts := Seq(8080, 8088),

    libraryDependencies ++= {
        val akkaVersion = "2.4.1"
        val akkaStreamVersion = "2.0.1"

        Seq(
            "wiii" %% "awebapi" % "0.3",

            "io.spray" %% "spray-json" % "1.3.2",
            "com.typesafe" % "config" % "1.3.0",
            "net.ceedubs" %% "ficus" % "1.1.2",
            "org.twitter4j" % "twitter4j-core" % "4.0.4",
            "org.twitter4j" % "twitter4j-stream" % "4.0.4",
            "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.9.0",

            "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
            "ch.qos.logback" % "logback-classic" % "1.1.3",

            "com.typesafe.akka" %% "akka-actor" % akkaVersion,
            "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamVersion,
            "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamVersion,
            "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamVersion,
            "com.typesafe.akka" %% "akka-http-xml-experimental" % akkaStreamVersion,
            "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamVersion,
            "com.typesafe.akka" %% "akka-slf4j" % akkaVersion % Runtime,

            "org.scalatest" %% "scalatest" % "2.2.5" % Test,
            "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
            "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaStreamVersion % Test
        )
    }
)

lazy val root = project.in(file(".")).aggregate(basic, twitter_kafka, twitter_webhooks)

lazy val basic = project.in(file("basic-service")).
                 settings(commonSettings: _*).
                 settings(
                     mainClass in assembly := Option("simple.Bootstrap")
                 ).enablePlugins(JavaAppPackaging)

lazy val twitter_kafka = project.in(file("twitter-kafka")).
                         settings(commonSettings: _*).
                         settings(
                             mainClass in assembly := Option("twitter.kafka.FeedToKafka")
                         ).enablePlugins(JavaAppPackaging)

lazy val twitter_webhooks = project.in(file("twitter-webhooks")).
                            settings(commonSettings: _*).
                            settings(
                                mainClass in assembly := Option("twitter.webhooks.Producer")
                            ).enablePlugins(JavaAppPackaging)

test in assembly := {}
assembleArtifact in assemblyPackageScala := true
assemblyMergeStrategy in assembly := {
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
    case "reference.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
}

// artifact repo config
//val host = sys.env.getOrElse("ARTIFACT_REPO_HOST", "localhost")
//val port = sys.env.getOrElse("ARTIFACT_REPO_PORT", "8081")
//val repo = s"http://$host:$port/artifactory"
//val user = sys.env.getOrElse("ARTIFACT_REPO_USER", "admin")
//val pass = sys.env.getOrElse("ARTIFACT_REPO_PASS", "")
//
//resolvers += "Artifactory Realm" at s"$repo/libs-snapshot-local/"
//credentials += Credentials("Artifactory Realm", host, user, pass)
//
//publishTo := {
//    if (isSnapshot.value)
//        Some("Artifactory Realm" at s"$repo/libs-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
//    else
//        Some("Artifactory Realm" at s"$repo/libs-release-local")
//}
