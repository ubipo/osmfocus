package net.pfiers.osmfocus.view.support

import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import androidx.annotation.ColorInt
import net.pfiers.osmfocus.service.extensions.toDecimalDegrees
import net.pfiers.osmfocus.service.osm.Element
import net.pfiers.osmfocus.service.osm.name
import net.pfiers.osmfocus.service.tagboxlocations.TbLoc
import org.locationtech.jts.geom.Coordinate
import org.ocpsoft.prettytime.PrettyTime
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToInt
import kotlin.reflect.KClass

@ExperimentalStdlibApi
class ViewFunctions {
    companion object {
        private val prettyTime = PrettyTime()

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
        fun createStrokeFrameBg(strokeWidthDp: Float, @ColorInt strokeColor: Int, @ColorInt bgColor: Int) =
            GradientDrawable().apply {
                setStroke(dpToPx(strokeWidthDp), strokeColor)
                setColor(bgColor)
            }

        private val pxToDpCache = HashMap<Float, Int>()
        @JvmStatic
        fun dpToPx(dp: Float): Int = pxToDpCache.getOrPut(dp) {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                Resources.getSystem().displayMetrics
            ).toInt()
        }

        @JvmStatic
        fun prettyTime(instant: Instant): String = prettyTime.format(instant)

        @JvmStatic
        fun decimalDegrees(coordinate: Coordinate) = coordinate.toDecimalDegrees()

        @JvmStatic
        fun capitalized(elementType: KClass<out Element>) = elementType.name.replaceFirstChar {
            it.titlecase(Locale.ROOT)
        }
    }
}
