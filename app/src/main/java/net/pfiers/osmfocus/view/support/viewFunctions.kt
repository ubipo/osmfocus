package net.pfiers.osmfocus.view.support

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc
import org.ocpsoft.prettytime.PrettyTime
import java.time.Instant

@ExperimentalStdlibApi
class ViewFunctions {
    companion object {
        val prettyTime = PrettyTime()

        @JvmStatic
        fun gravityFromTbLoc(tbLoc: TbLoc): Int {
            val gravityRow = when (tbLoc.y) {
                TbLoc.Y.TOP -> Gravity.TOP
                TbLoc.Y.MIDDLE -> Gravity.CENTER_VERTICAL
                TbLoc.Y.BOTTOM -> Gravity.BOTTOM
            }
            val gravityColumn = when (tbLoc.x) {
                TbLoc.X.LEFT -> Gravity.START
                TbLoc.X.MIDDLE -> Gravity.CENTER_HORIZONTAL
                TbLoc.X.RIGHT -> Gravity.END
            }
            return gravityRow or gravityColumn
        }

        @JvmStatic
        fun bgFromParams(strokeWidth: Int, @ColorInt strokeColor: Int, @ColorInt bgColor: Int) =
            GradientDrawable().apply {
                setStroke(strokeWidth, strokeColor)
                setColor(bgColor)
            }

        @JvmStatic
        fun prettyTime(instant: Instant): String = prettyTime.format(instant)
    }
}
