enablePlugins(DockerPlugin)

organization := "com.github.jw3.examples"
name := "simple-docker-microservice"
version := "0.1-SNAPSHOT"
licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion := "2.11.7"
scalacOptions += "-target:jvm-1.8"

libraryDependencies ++= {
    val akkaVersion = "2.4.0"
    val akkaStreamVersion = "1.0"

    Seq(
        "wiii" %% "awebapi" % "0.2",

        "io.spray" %% "spray-json" % "1.3.2",
        "com.typesafe" % "config" % "1.3.0",
        "net.ceedubs" %% "ficus" % "1.1.2",

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

docker <<= (docker dependsOn assembly)
dockerfile in docker := {
    val artifact = assemblyOutputPath in assembly value
    val artifactTargetPath = s"/app/${artifact.name}"
    new sbtdocker.mutable.Dockerfile {
        from("java:8")
        add(artifact, artifactTargetPath)
        copy(artifact, artifactTargetPath)
        expose(2222)  // todo; read port from app.config
        entryPoint("java", "-jar", artifactTargetPath)
    }
}

val dockerWrite = taskKey[Unit]("Write the Docker file")
dockerWrite <<= dockerWrite.dependsOn(compile in Compile, dockerfile in docker)
dockerWrite := {
    val dockerDir = target.value / "docker"
    val dockerFile = (dockerfile in docker).value
    IO.write(dockerDir / "Dockerfile", sbtdocker.staging.DefaultDockerfileProcessor(dockerFile, dockerDir).instructionsString)
}


mainClass in assembly := Option("simple.Bootstrap")
test in assembly := {}
assembleArtifact in assemblyPackageScala := true
assemblyMergeStrategy in assembly := {
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
    case "reference.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
}


// artifact repo config

val host = sys.env.getOrElse("ARTIFACT_REPO_HOST", "localhost")
val port = sys.env.getOrElse("ARTIFACT_REPO_PORT", "8081")
val repo = s"http://$host:$port/artifactory"
val user = sys.env.getOrElse("ARTIFACT_REPO_USER", "admin")
val pass = sys.env.getOrElse("ARTIFACT_REPO_PASS", "")

resolvers += "jw3 at bintray" at "https://dl.bintray.com/jw3/maven"
resolvers += "Artifactory Realm" at s"$repo/libs-snapshot-local/"
credentials += Credentials("Artifactory Realm", host, user, pass)

publishTo := {
    if (isSnapshot.value)
        Some("Artifactory Realm" at s"$repo/libs-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
    else
        Some("Artifactory Realm" at s"$repo/libs-release-local")
}

publishMavenStyle := true
