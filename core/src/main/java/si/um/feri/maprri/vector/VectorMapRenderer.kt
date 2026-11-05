package si.um.feri.maprri.vector

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan
import org.json.JSONArray
import org.json.JSONObject

class VectorMapRenderer(
    var camera: OrthographicCamera,
    screenWidth: Float,
    screenHeight: Float
) {

    private var minX = Float.MAX_VALUE
    private var maxX = -Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxY = -Float.MAX_VALUE

    val shapeRenderer: ShapeRenderer = ShapeRenderer()
    private val roads = mutableListOf<FloatArray>()
    private val buildings = mutableListOf<FloatArray>()

    val viewport: Viewport = FitViewport(screenWidth, screenHeight, camera).apply { apply() }

    var scale: Float = 0.5f
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    init {
        loadGeoJSON("maribor_center.geojson")
    }

    private fun loadGeoJSON(filename: String) {
        try {
            val text = Gdx.files.internal(filename).readString("UTF-8")

            val geojson = JSONObject(text)
            val features = geojson.getJSONArray("features")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val geometry = feature.getJSONObject("geometry")
                when (geometry.getString("type")) {
                    "LineString" -> roads.add(
                        parseCoordinates(geometry.getJSONArray("coordinates"))
                    )
                    "Polygon" -> buildings.add(
                        parsePolygon(geometry.getJSONArray("coordinates"))
                    )
                    "MultiPolygon" -> {
                        val polys = geometry.getJSONArray("coordinates")
                        for (j in 0 until polys.length()) {
                            buildings.add(parsePolygon(polys.getJSONArray(j)))
                        }
                    }
                }
            }

            // Center the map on load
            offsetX = (minX + maxX) / 2f
            offsetY = (minY + maxY) / 2f

            println("Map centered at offset ($offsetX, $offsetY), scale=$scale")
            println("Loaded: ${roads.size} roads, ${buildings.size} buildings")
            println("Bounds:")
            println("X range: $minX → $maxX")
            println("Y range: $minY → $maxY")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseCoordinates(coords: JSONArray): FloatArray {
        val vertices = FloatArray(coords.length() * 2)
        for (i in 0 until coords.length()) {
            val c = coords.getJSONArray(i)
            val p = lngLatToMeters(c.getDouble(0), c.getDouble(1))
            vertices[i * 2] = p.x
            vertices[i * 2 + 1] = p.y

            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        return vertices
    }

    private fun parsePolygon(polyCoords: JSONArray): FloatArray =
        // Polygons in GeoJSON are an array of linear rings; only outer ring used.
        parseCoordinates(polyCoords.getJSONArray(0))

    fun render() {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        shapeRenderer.color = Color.RED
        for (vertices in buildings) drawPolygon(vertices)

        shapeRenderer.color = Color.DARK_GRAY
        for (vertices in roads) drawLineString(vertices)

        shapeRenderer.end()
    }

    // Note: drawPolygon and drawLineString are identical in the original —
    // kept separate for parity; collapse in the redesign phase.
    private fun drawPolygon(vertices: FloatArray) = drawPolyline(vertices)

    private fun drawLineString(vertices: FloatArray) = drawPolyline(vertices)

    private fun drawPolyline(vertices: FloatArray) {
        var i = 0
        while (i < vertices.size - 2) {
            val x1 = (vertices[i] - offsetX) * scale
            val y1 = (vertices[i + 1] - offsetY) * scale
            val x2 = (vertices[i + 2] - offsetX) * scale
            val y2 = (vertices[i + 3] - offsetY) * scale
            shapeRenderer.line(x1, y1, x2, y2)
            i += 2
        }
    }

    fun dispose() {
        shapeRenderer.dispose()
    }

    companion object {
        // Web Mercator projection (EPSG:3857)
        fun lngLatToMeters(lng: Double, lat: Double): Vector2 {
            val x = lng * 20037508.34 / 180.0
            var y = ln(tan((90.0 + lat) * PI / 360.0)) / (PI / 180.0)
            y = y * 20037508.34 / 180.0
            return Vector2(x.toFloat(), y.toFloat())
        }
    }
}
