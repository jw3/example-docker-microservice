resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.0")
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.0.6")

logLevel := Level.Warn