package me.krrr.wallpaper

import android.app.Activity
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.{DisplayMetrics, Log}
import android.view.{WindowManager, Window}
import android.widget.TextView
import org.json.{JSONArray, JSONException, JSONObject}

import scala.util.Random


class MainActivity extends Activity {

    override def onAttachedToWindow() {
        super.onAttachedToWindow()
        // let it work on old device. setDither on Drawable is another way
        getWindow.setFormat(PixelFormat.RGBA_8888)
    }

    override def onCreate(savedInstanceState: Bundle) = {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        getWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.main)

        val nameLabel = findViewById(R.id.gra_name).asInstanceOf[TextView]
        val subLabel = findViewById(R.id.gra_subname).asInstanceOf[TextView]

        // set text shadow (related to screen density)
        val metrics = new DisplayMetrics
        getWindowManager.getDefaultDisplay.getMetrics(metrics)
        val radius = metrics.density * 2
        Log.d("Main", "Shadow radius: " + radius)
        nameLabel.setShadowLayer(radius, 0, 0, 0xEE111111)
        subLabel.setShadowLayer(radius, 0, 0, 0xEE222222)

        // read color settings from JSON
        val gra = findViewById(R.id.gra_view).asInstanceOf[GradientView]
        gra.filter = GradientView.Filter.TAQUIN
        try {
            val i_stream = getResources.openRawResource(R.raw.uigradients)
            val json_s = io.Source.fromInputStream(i_stream).mkString

            val ary = new JSONArray(json_s)

            val entry = ary.getJSONObject(Random.nextInt(ary.length))

            val name = entry.getString("name")
            val subName = if (entry.has("sub_name")) entry.getString("sub_name") else ""
            nameLabel.setText(name)
            subLabel.setText(subName)

            if (entry.has("color"))
                gra.setColor(entry.getString("color"))
            else
                gra.setColors(Array("color1", "color2").map(entry.getString))
        } catch {
            case e: JSONException => {
                nameLabel.setText("JSON Failed")
                subLabel.setText(e.toString) // it has text, just for layout editing
            }
        }

    }

}

