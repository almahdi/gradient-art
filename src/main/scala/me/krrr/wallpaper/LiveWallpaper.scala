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

class LiveWallpaper extends WallpaperService {
    private val handler = new Handler

    def onCreateEngine() = new GraEngine

    class GraEngine extends Engine with OnSharedPreferenceChangeListener {
        private val pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext)
        private var drawTask: Runnable = null
        private val (view, nameLabel, subLabel) = {  // inflate view and set text shadows
            val view = LayoutInflater.from(
                getApplicationContext).inflate(R.layout.main, null)
            val nameLabel = view.findViewById(R.id.gra_name).asInstanceOf[TextView]
            val subLabel = view.findViewById(R.id.gra_subname).asInstanceOf[TextView]

            val metrics = new DisplayMetrics
            val wm = getSystemService(Context.WINDOW_SERVICE).asInstanceOf[WindowManager]
            wm.getDefaultDisplay.getMetrics(metrics)
            val radius = metrics.density * 2
            Log.d("LWPService", "Shadow radius: " + radius)
            nameLabel.setShadowLayer(radius, 0, 0, 0xEE111111)
            subLabel.setShadowLayer(radius, 0, 0, 0xEE222222)

            (view, nameLabel, subLabel)
        }
        private var period = 0  // millisecond

        override def onSurfaceCreated(holder: SurfaceHolder) {
            holder.setFormat(PixelFormat.RGBA_8888)
            fromSettings()
            pref.registerOnSharedPreferenceChangeListener(this)
        }

        override def onSurfaceChanged(holder: SurfaceHolder, format: Int,
                                      width: Int, height: Int) = layoutView(width, height)

        override def onSurfaceDestroyed(holder: SurfaceHolder) {
            pref.unregisterOnSharedPreferenceChangeListener(this)
            handler.removeCallbacks(drawTask)
        }

        override def onVisibilityChanged(visible: Boolean) {
            if (visible) doDrawing()
            else handler.removeCallbacks(drawTask)
        }

        private def layoutView(w: Int, h: Int) {
            view.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
        }

        def doDrawing() {
            val holder = getSurfaceHolder
            val canvas = holder.lockCanvas()

            if (canvas != null) {
                view.draw(canvas)
                holder.unlockCanvasAndPost(canvas)
            }

            // reschedule the next draw
            handler.removeCallbacks(drawTask)
            handler.postDelayed(drawTask, period)
        }

        def fromSettings() = {
            val graView = view.findViewById(R.id.gra_view).asInstanceOf[GradientArtView]
            val filterIdx = pref.getString("filter", "0").toInt
            val randomFilterEnabled = filterIdx == -1
            period = pref.getString("period", "1800000").toInt

            val i_stream = getResources.openRawResource(R.raw.uigradients)
            val json_s = io.Source.fromInputStream(i_stream).mkString
            val entries = try new JSONArray(json_s) catch {case e: JSONException => null}

            def randomFilter() =
                graView.gra.filter = Filter(Random.nextInt(Filter.maxId))

            def randomColor() {
                var (name, subName) = ("", "")
                try {
                    val entry = entries.getJSONObject(Random.nextInt(entries.length))
                    if (entry.has("color"))
                        graView.gra.setColor(entry.getString("color"))
                    else
                        graView.gra.setColors(Array("color1", "color2").map(entry.getString))

                    if (pref.getBoolean("show_name", true)) {
                        name = entry.getString("name")
                        subName = if (entry.has("sub_name")) entry.getString("sub_name") else ""
                    }
                    nameLabel.setText(name)
                    subLabel.setText(subName)
                } catch {
                    case e @ (_: JSONException | _: NullPointerException) =>
                        nameLabel.setText("Failed to parse JSON")
                        subLabel.setText(e.toString) // it has text, just for layout editing
                }
                layoutView(view.getWidth, view.getHeight)
            }

            if (randomFilterEnabled) randomFilter()  // do first time
            else graView.gra.filter = Filter(filterIdx)
            randomColor()
            drawTask = new Runnable {
                def run() = {
                    if (randomFilterEnabled) randomFilter()
                    randomColor()
                    doDrawing()
                }
            }
        }

        def onSharedPreferenceChanged(pref: SharedPreferences, key: String) = fromSettings()
    }
}
