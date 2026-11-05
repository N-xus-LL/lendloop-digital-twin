package si.um.feri.maprri.raster.objects

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import kotlin.math.*
// import org.json.JSONArray
// import org.json.JSONObject
import si.um.feri.maprri.isDebug
import si.um.feri.maprri.raster.dataclasses.ZoomXY
import si.um.feri.maprri.raster.enums.MapServiceVendor

object MapRasterTiles {
    private       val vendor: MapServiceVendor = MapServiceVendor.GEOAPIFY
            const val TILE_SIZE: Int = 512  // @2x in MapService.FORMAT means higher DPI; image is 512px, else 256px

    private fun fetchTileByteArray(path: String): ByteArray = vendor.buildURL(path).readBytes()
    private fun getTexture(array: ByteArray): Texture = Texture(Pixmap(array, 0, array.size))
    private fun getRasterTile(zoom: Int, x: Int, y: Int): Texture = getTexture(fetchTileByteArray("$zoom/$x/$y"))
    // fun getRasterTile(zoom: Int): Texture = getTexture(fetchTitleByteArray("$zoom"))

    /**
     * Returns tiles for a size * size zone around the provided center tile.
     * Row-major, top-left first.
     */
    fun getRasterTileZone(zoomXY: ZoomXY, size: Int): List<Texture> {
        val start = -((size - 1) / 2)
        return buildList(size * size) {
            for (row in 0 until size) {
                for (col in 0 until size) {
                    val x = zoomXY.x + start + col
                    val y = zoomXY.y + start + row
                    if (isDebug) println("${zoomXY.zoom}/$x/$y")
                    add(getRasterTile(zoomXY.zoom, x, y))
                }
            }
        }
    }

    // https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java
    fun getTileNumber(lat: Double, lng: Double, zoom: Int): ZoomXY {
        val n = 1 shl zoom

        val xTile = floor((lng + 180) / 360 * n).toInt().coerceIn(0, n - 1)
        val yTile = floor(
            (1 - ln(
                tan(MathFunctions.toRadians(lat))
                    + 1 / cos(MathFunctions.toRadians(lat))
            ) / PI) / 2 * n
        ).toInt().coerceIn(0, n - 1)

        return ZoomXY(zoom, xTile, yTile)
    }

    // https://www.maptiler.com/google-maps-coordinates-tile-bounds-projection/#15/15.63/46.56
    /* fun tile2lng(tileNumberX: Int, zoom: Int): Double = tileNumberX / (2.0).pow(zoom) * 360 - 180

    fun tile2lat(tileNumberY: Int, zoom: Int): Double {
        val n = PI - 2 * PI * tileNumberY / (2.0).pow(zoom)
        return MathFunctions.toRadians(atan(0.5 * (exp(n) - exp(-n))))
    } */

    fun project(lat: Double, lng: Double, tileSize: Int): DoubleArray {
        val sinY = sin(MathFunctions.toRadians(lat))
            .coerceIn(-0.9999, 0.9999)   // Clamp to 0.9999 → effective max latitude ≈ 89.189°

        return doubleArrayOf(
            tileSize * (0.5 + lng / 360),
            tileSize * (0.5 - ln((1 + sinY) / (1 - sinY)) / (4 * PI))
        )
    }

    fun getPixelPosition(
        lat: Double, lng: Double,
        beginTileX: Int, beginTileY: Int,
        tileSize: Int = TILE_SIZE, height: Int = Constants.MAP_HEIGHT, zoom: Int = Constants.ZOOM
    ): Vector2 {
        val world = project(lat, lng, tileSize)
        val scale = (2.0).pow(zoom)
        return Vector2(
            (floor(world[0] * scale) - beginTileX * tileSize).toInt().toFloat(),
            (height - (floor(world[1] * scale) - beginTileY * tileSize - 1)).toInt().toFloat()
        )
    }

    /* fun fetchPath(
        geolocations: Array<Geolocation>
    ): Array<Array<Geolocation>>? {
        val coordinates = Array(geolocations.size) { i ->
            doubleArrayOf(geolocations[i].lat, geolocations[i].lng)
        }
        return try {
            getRouteFromCoordinates(coordinates)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    } */

    /* fun getRouteFromCoordinates(coordinates: Array<DoubleArray>): Array<Array<Geolocation>>? {
        val coordinatesPath = coordinates.joinToString("|") { "${it[0]},${it[1]}" }
        val urlString =
            "https://api.geoapify.com/v1/routing?waypoints=$coordinatesPath&mode=drive&apiKey=${Keys.GEOAPIFY}"

        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
            if (connection.responseCode != HttpURLConnection.HTTP_OK)
                throw RuntimeException("[ERROR] HTTP error code: ${connection.responseCode}")

        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val features = JSONObject(response).getJSONArray("features")
        if (features.length() == 0) return null

        val coordinatesArray: JSONArray = features
            .getJSONObject(0)
            .getJSONObject("geometry")
            .getJSONArray("coordinates")

        return Array(coordinatesArray.length()) { i ->
            val segment = coordinatesArray.getJSONArray(i)
            Array(segment.length()) { j ->
                val c = segment.getJSONArray(j)
                val lng = c.getDouble(0)
                val lat = c.getDouble(1)
                if (isDebug) println("Longitude: $lng, Latitude: $lat")
                Geolocation(lat, lng)
            }
        }
    } */

}
