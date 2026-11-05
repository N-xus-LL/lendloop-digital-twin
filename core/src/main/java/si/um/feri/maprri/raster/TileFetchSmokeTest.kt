package si.um.feri.maprri.raster

import si.um.feri.maprri.raster.enums.MapServiceVendor
import si.um.feri.maprri.readBytes
import java.io.File

object TileFetchSmokeTest {
    private val vendor: MapServiceVendor = MapServiceVendor.GEOAPIFY
    private const val TILESET_ID: String = "dark-matter-purple-roads"
    private const val PATH: String = "/15/17806/11583"

    @JvmStatic
    fun main(args: Array<String>) {
        val byteArray = vendor.buildURL(TILESET_ID, PATH).readBytes()
        val fileOutput = "test.png"
        writeBytesToFile(fileOutput, byteArray)
    }

    private fun writeBytesToFile(fileOutput: String, bytes: ByteArray) =
        File(fileOutput).writeBytes(bytes)
}
