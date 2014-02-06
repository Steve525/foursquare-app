name := "foursquare-app"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache
)

libraryDependencies += "foursquare4j" % "foursquare4j" % "0.1.3"

resolvers += "foursquare4j" at "http://foursquare4j.googlecode.com/svn/maven2"

play.Project.playJavaSettings