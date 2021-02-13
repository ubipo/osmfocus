package net.pfiers.osmfocus.view.support

import android.content.Context
import android.graphics.Color
import android.util.Log
import net.pfiers.osmfocus.R

enum class PaletteId(val id: Int) {
    PALETTE_VIBRANT(R.array.paletteVibrant),
    PALETTE_MATERIAL(R.array.paletteMaterial)
}

const val PALETTE_SIZE = 8

typealias Palettes = Map<PaletteId, List<Int>>
fun generatePalettes(context: Context): Palettes =
    PaletteId.values().map { paletteId ->
        val arr = context.resources.obtainTypedArray(paletteId.id)
        if (arr.length() != PALETTE_SIZE) error("Bad palette size")
        val colors = mutableListOf<Int>()
        for (index in 0 until arr.length()) {
            colors.add(arr.getColor(index, Color.BLACK))
        }
        arr.recycle()
        paletteId to colors
    }.toMap()
