package me.krrr.wallpaper

import android.graphics._
import android.graphics.drawable.GradientDrawable

import scala.collection.mutable.ArrayBuffer
import scala.util.Random


class GradientArtDrawable {
    // simple Drawable that only support draw method, no setBounds
    private var colors = List(0xFFE6DADA, 0xFF274046)  // default color named "Metal"
    private var _degree = 30f
    private var cache: Bitmap = null

    import GradientArtDrawable.GradientGen
    import GradientArtDrawable.Filter._
    var filter = NO_FILTER

    def degree = _degree
    def degree_=(deg: Float) {
        if (!(0 <= deg && deg <= 90))
            throw new Exception("Invalid degree")
        _degree = deg
        emptyCache()
    }

    def draw(canvas: Canvas) {
        // for performance, some filter do not create a temp bitmap first
        val (w, h) = (canvas.getWidth, canvas.getHeight)
        if (cache != null && cache.getWidth == w && cache.getHeight == h) {
            drawCache(canvas)
        } else {
            cache = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            canvas.save()
            filter match {
                case NO_FILTER =>
                    drawRotatedGra(new Canvas(cache))
                case TAQUIN =>
                    val temp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    drawRotatedGra(new Canvas(temp))
                    taquin(temp, new Canvas(cache))
                    // temp will not be gc before it be created again without the two calls
                    temp.recycle()
                    System.gc()
                case BANDING =>
                    drawBanding(new Canvas(cache))
            }
            canvas.restore()
            drawCache(canvas)
        }
    }

    def emptyCache() =
        cache = null

    private def drawCache(canvas: Canvas) {
        val rect = new Rect(0, 0, cache.getWidth, cache.getHeight)
        canvas.drawBitmap(cache, rect, rect, null)
    }

    private def drawRotatedGra(canvas: Canvas) {
        // this method change state of canvas
        val (new_w, new_h) = rotateCanvas(canvas)
        val gd = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            colors.toArray)
        gd.setBounds(0, 0, new_w, new_h)
        gd.draw(canvas)
    }

    private def drawBanding(canvas: Canvas) {
        val n = 7
        val (w, h) = rotateCanvas(canvas)
        val block_h = h.toFloat / n
        val paint = new Paint
        for ((y, c) <- (0F until h by block_h).iterator zip
                GradientGen(colors(0), colors(1), n)) {
            paint.setColor(c)
            canvas.drawRect(0, y, w, y+block_h, paint)
        }
    }

    private def rotateCanvas(canvas: Canvas): (Int, Int) = {
        // rotate canvas and return tuple of new width and width
        val (w, h) = (canvas.getWidth, canvas.getHeight)
        val _deg = _degree / 180.0 * math.Pi
        val (deg_sin, deg_cos) = (math.sin(_deg), math.cos(_deg))

        canvas.translate((deg_sin * deg_sin * w).toFloat,
            -(deg_sin * deg_cos * w).toFloat)
        canvas.rotate(_degree)
        ((deg_sin * h + deg_cos * w).toInt, (deg_cos * h + deg_sin * w).toInt)
    }

    private def taquin(src: Bitmap, dst: Canvas) {
        val n = 7
        val (block_w, block_h) = (src.getWidth.toFloat / n, src.getHeight.toFloat / n)
        val paint = new Paint
        val (src_rect, dst_rect) = (new Rect, new RectF)
        val border_rect = new Rect(0, 0, block_w.toInt, block_h.toInt)
        val border_w = Array(block_w, block_h).min * 0.04f

        val border_bmp = {
            val bmp = Bitmap.createBitmap(block_w.toInt, block_h.toInt,
                Bitmap.Config.ARGB_8888)
            val c = new Canvas(bmp)
            // use path to draw parallelogram, light part first.
            // there should be simpler way to do this...
            paint.setARGB(30, 255, 255, 255)
            val path = new Path
            path.lineTo(block_w, 0)
            path.lineTo(block_w, block_h*0.7f)
            path.lineTo(block_w-border_w, block_h*0.7f-border_w)
            path.lineTo(block_w-border_w, border_w)
            path.lineTo(border_w, border_w)
            path.moveTo(0, 0)
            c.drawPath(path, paint)
            path.reset()
            // dark part
            paint.setARGB(30, 0, 0, 0)
            path.lineTo(0, block_h)
            path.lineTo(block_w, block_h)
            path.lineTo(block_w, block_h*0.7f)
            path.lineTo(block_w-border_w, block_h*0.7f-border_w)
            path.lineTo(block_w-border_w, block_h-border_w)
            path.lineTo(border_w, block_h-border_w)
            path.lineTo(border_w, border_w)
            path.lineTo(0, 0)
            c.drawPath(path, paint)
            paint.reset()
            bmp
        }

        for (dst_i <- 0 until n * n) {
            // copy block
            val src_i = Random.nextInt(n * n)  // random from [0, n**2]
            val (src_x, src_y) = ((src_i % n) * block_w, src_i / n * block_h)
            val (dst_x, dst_y) = ((dst_i % n) * block_w, dst_i / n * block_h)
            src_rect.set(src_x.toInt, src_y.toInt, (src_x+block_w).toInt, (src_y+block_h).toInt)
            dst_rect.set(dst_x, dst_y, dst_x+block_w, dst_y+block_h)
            dst.drawBitmap(src, src_rect, dst_rect, paint)
            // draw border
            dst.drawBitmap(border_bmp, border_rect, dst_rect, paint)
        }
    }

    private def colorStrToInt(s: String): Int =
        // eg. convert "#FFCCFF" to 0xFFFFCCFF (argb)
        Integer.parseInt(s.drop(1), 16) + 0xFF000000

    def setColor(color: String) {
        val c = colorStrToInt(color)
        this.colors = List(c, c)
        emptyCache()
    }

    def setColors(colors: Array[String]) {
        this.colors = colors.map(colorStrToInt).toList
        emptyCache()
    }
}


object GradientArtDrawable {
    object Filter extends Enumeration {
        val NO_FILTER, TAQUIN, BANDING = Value
    }

    /**
     * Make a color gradient with banding effect, which has PART partitions.
     */
    class GradientGen(from: Int, to: Int, parts: Int) extends Iterator[Int] {
        private val steps = Array(
            (getA(to) - getA(from)).toFloat / parts,
            (getR(to) - getR(from)).toFloat / parts,
            (getG(to) - getG(from)).toFloat / parts,
            (getB(to) - getB(from)).toFloat / parts)
        private val argb = ArrayBuffer(getA(from).toFloat, getR(from).toFloat,
            getG(from).toFloat, getB(from).toFloat)
        private var i = 0

        def hasNext = i < parts

        def next() = {
            i += 1
            for (k <- 0 to 3)
                argb(k) += steps(k)
            toARGB(argb(0).toInt, argb(1).toInt, argb(2).toInt, argb(3).toInt)
        }

        def getA(argb: Int) = (0xFF000000 & argb) >>> 24
        def getR(argb: Int) = (0x00FF0000 & argb) >>> 16
        def getG(argb: Int) = (0x0000FF00 & argb) >>> 8
        def getB(argb: Int) = 0x000000FF & argb
        def toARGB(a: Int, r: Int, g: Int, b: Int) =
            (a << 24) + (r << 16) + (g << 8) + b
    }

    object GradientGen {
        def apply(a: Int, b: Int, c: Int) = new GradientGen(a, b, c)
    }
}
