package net.pfiers.osmfocus.service.tagboxlocation

import android.os.Parcel
import android.os.Parcelable


/**
 * Tagbox Location
 *
 * Represents the location of a "tagbox" (white
 * box with element's tags) on-screen. So the
 * TbLoc of the top-left tagbox is
 * TbLoc(X.LEFT, Y.TOP).
 */
data class TbLoc(
    val x: X,
    val y: Y
) : Parcelable {
    // Order of these enums matters, TbLoc.applyConstraints uses it to sort
    enum class X {
        LEFT, MIDDLE, RIGHT;

        companion object {
            val values by lazy { values() }
        }
    }

    enum class Y {
        TOP, MIDDLE, BOTTOM;

        companion object {
            val values by lazy { values() }
        }
    }

    constructor(parcel: Parcel) : this(
        X.values[parcel.readInt()],
        Y.values[parcel.readInt()]
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(x.ordinal)
        dest.writeInt(y.ordinal)
    }

    companion object CREATOR : Parcelable.Creator<TbLoc> {
        override fun createFromParcel(parcel: Parcel): TbLoc = TbLoc(parcel)
        override fun newArray(size: Int): Array<TbLoc?> = List(size) {
            TbLoc(X.LEFT, Y.TOP)
        }.toTypedArray()
    }
}
