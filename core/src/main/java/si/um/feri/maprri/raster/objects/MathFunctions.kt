package si.um.feri.maprri.raster.objects

import kotlin.math.PI

object MathFunctions {
    fun toRadians(deg: Double): Double = deg / 180.0 * PI
    fun toDegrees(rad: Double): Double = rad * 180.0 / PI
}
