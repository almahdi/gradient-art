package work.krrr.androidhelper

import scala.language.implicitConversions

object Conversions {

    implicit def funcAsRunnable(f: () => Unit): Runnable = new Runnable {
        def run(): Unit = f()
    }
}
