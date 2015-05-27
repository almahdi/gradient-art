import android.Keys._

android.Plugin.androidBuild

platformTarget in Android := "android-22"

name := "wallpaper"


scalaVersion := "2.11.6"

run <<= run in Android

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "jcenter" at "http://jcenter.bintray.com"
)

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
scalacOptions ++= Seq("-feature", "-deprecation", "-target:jvm-1.7")

libraryDependencies ++= Seq()

proguardScala in Android := true

proguardOptions in Android ++= Seq(
  "-ignorewarnings",
  "-keep class scala.Dynamic"
)
