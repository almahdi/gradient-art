package me.krrr.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class GradientArtView(ctx: Context, attrs: AttributeSet) extends View(ctx, attrs) {
    val gra = new GradientArtDrawable

    override def onDraw(canvas: Canvas) = gra.draw(canvas)
}
