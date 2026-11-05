package si.um.feri.maprri.raster.dataclasses

data class ZoomXY(var zoom: Int, var x: Int, var y: Int) {
    override fun toString(): String = "$zoom/$x/$y"
}
