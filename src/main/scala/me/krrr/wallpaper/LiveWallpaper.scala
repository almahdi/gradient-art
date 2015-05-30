package me.krrr.wallpaper

import android.content.{SharedPreferences, Context}
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.PixelFormat
import android.os.Handler
import android.preference.PreferenceManager
import android.service.wallpaper.WallpaperService
import android.util.{Log, DisplayMetrics}
import android.view.View.MeasureSpec
import android.view.{WindowManager, LayoutInflater, SurfaceHolder}
import android.widget.TextView
import org.json.{JSONArray, JSONException}
import GradientArtDrawable.Filter

import scala.util.Random
import scala.language.reflectiveCalls


class LiveWallpaper extends WallpaperService {
    private val handler = new Handler

    def onCreateEngine() = new GraEngine

    class GraEngine extends Engine with OnSharedPreferenceChangeListener {
        private val pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext)
        private var hasDelayedChange = false
        private val changeTask = new Runnable {
            def run() = {
                if (isVisible) {
                    fromSettings()
                    doDrawing()
                } else {
                    hasDelayedChange = true
                }
                schedule()
            }

            def schedule() = handler.postDelayed(this, pref.getString("period", "1800000").toInt)
        }
        private val (view, nameLabel, subLabel, gra) = {
            // inflate view and set text shadows
            val view = LayoutInflater.from(
                getApplicationContext).inflate(R.layout.main, null)
            val nameLabel = view.findViewById(R.id.gra_name).asInstanceOf[TextView]
            val subLabel = view.findViewById(R.id.gra_subname).asInstanceOf[TextView]
            val gra = view.findViewById(R.id.gra_view).asInstanceOf[GradientArtView].gra

            val metrics = new DisplayMetrics
            val wm = getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager]
            wm.getDefaultDisplay.getMetrics(metrics)
            val radius = metrics.density * 2
            Log.d("LWPService", "Shadow radius: " + radius)
            nameLabel.setShadowLayer(radius, 0, 0, 0xEE111111)
            subLabel.setShadowLayer(radius, 0, 0, 0xEE222222)

            (view, nameLabel, subLabel, gra)
        }
        val patterns = {
            val i_stream = getResources.openRawResource(R.raw.uigradients)
            val json_s = io.Source.fromInputStream(i_stream).mkString
            try new JSONArray(json_s) catch { case e: JSONException => null }
        }

        override def onSurfaceCreated(holder: SurfaceHolder) {
            holder.setFormat(PixelFormat.RGBA_8888)
            pref.registerOnSharedPreferenceChangeListener(this)
            // for some unknown reasons, onVisibilityChanged will be called three
            // times initially: show, hide, show. This is a workaround.
            fromSettings()
            changeTask.schedule()
        }

        override def onSurfaceChanged(holder: SurfaceHolder, format: Int,
                                      width: Int, height: Int) {
            layoutView(width, height)
            doDrawing()
        }

        override def onSurfaceDestroyed(holder: SurfaceHolder) {
            handler.removeCallbacks(changeTask)
            pref.unregisterOnSharedPreferenceChangeListener(this)
        }

        override def onVisibilityChanged(visible: Boolean) {
            if (visible && hasDelayedChange) {
                fromSettings()
                doDrawing()
                hasDelayedChange = false
            }
        }

        private def layoutView(w: Int, h: Int) {
            view.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
        }

        // Only called after this engine created or settings
        // changed or called by changeTask repeatedly.
        def doDrawing() {
            val holder = getSurfaceHolder
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                view.draw(canvas)
                holder.unlockCanvasAndPost(canvas)
            }
        }

        def fromSettings() = {
            val idx = pref.getString("filter", "0").toInt
            gra.filter = Filter(if (idx == -1) Random.nextInt(Filter.maxId) else idx)

            try {
                val entry = patterns.getJSONObject(Random.nextInt(patterns.length))
                if (entry.has("color"))
                    gra.setColor(entry.getString("color"))
                else
                    gra.setColors(Array("color1", "color2").map(entry.getString))

                var (name, subName) = ("", "")
                if (pref.getBoolean("show_name", true)) {
                    name = entry.getString("name")
                    subName = if (entry.has("sub_name")) entry.getString("sub_name") else ""
                }
                nameLabel.setText(name)
                subLabel.setText(subName)
            } catch {
                case e@(_: JSONException | _: NullPointerException) =>
                    nameLabel.setText("Failed to parse JSON")
                    subLabel.setText(e.toString)
            }
            layoutView(view.getWidth, view.getHeight)
        }

        def onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
            hasDelayedChange = true
        }
    }

}
