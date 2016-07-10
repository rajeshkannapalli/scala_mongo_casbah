name := "SBTDemo"

version := "1.0"

scalaVersion := "2.11.8"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1"
libraryDependencies += "org.mongodb" %% "casbah" % "3.1.1"
resolvers += "justwrote" at "http://repo.justwrote.it/releases/"
libraryDependencies += "it.justwrote" %% "scala-faker" % "0.3"