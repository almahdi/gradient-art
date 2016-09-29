android.Plugin.androidBuild

name := "gradient-art"
scalaVersion := "2.11.6"
platformTarget in Android := "android-21"

run <<= run in Android

resolvers += Resolver.jcenterRepo

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
scalacOptions ++= Seq("-feature", "-deprecation", "-target:jvm-1.7")
