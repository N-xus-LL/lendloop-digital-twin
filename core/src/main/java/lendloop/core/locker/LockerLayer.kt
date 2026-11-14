package lendloop.core.locker

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import lendloop.core.api.LockerApiClient
import lendloop.maps.raster.dataclasses.ZoomXY
import lendloop.maps.raster.objects.MapRasterTiles

class LockerLayer(
    shapeRenderer: ShapeRenderer,
    baseUrl: String
) {
    private val api = LockerApiClient(baseUrl)
    private val renderer = LockerMarkerRenderer(shapeRenderer)

    /** Cache of pixel positions for the current beginTile, so hit tests don't recompute. */
    private val positions = ArrayList<Vector2>()
    private var lastBeginTile: ZoomXY? = null
    private var availableOnlyFilter = false

    val stations: List<LockerStation> get() = renderer.stations

    /** Callback for the UI when a marker is clicked. */
    var onStationSelected: ((LockerStation) -> Unit)? = null


    fun load(availableOnly: Boolean = availableOnlyFilter) {
        availableOnlyFilter = availableOnly
        api.fetchStations(availableOnly, onSuccess = { fetched ->
            renderer.setStations(fetched)
            lastBeginTile = null
            Gdx.app.log("LockerLayer", "loaded ${fetched.size} stations")
        })
    }

    fun toggleAvailabilityFilter() {
        load(!availableOnlyFilter)
    }


    fun render(camera: OrthographicCamera, beginTile: ZoomXY) {
        ensurePositions(beginTile)
        renderer.render(beginTile)
    }


    fun onTap(world: Vector3, beginTile: ZoomXY) {
        ensurePositions(beginTile)
        var best: LockerStation? = null
        var bestDist = 30f * 30f
        for (i in stations.indices) {
            val p = positions[i]
            val dx = p.x - world.x
            val dy = p.y - world.y
            val d2 = dx * dx + dy * dy
            if (d2 < bestDist) { bestDist = d2; best = stations[i] }
        }
        best?.let { station ->
            api.fetchDetail(station.id, onSuccess = { boxes ->
                Gdx.app.log(
                    "LockerLayer",
                    "${station.stationName} — ${boxes.count { it.available }}/${boxes.size} free"
                )
                onStationSelected?.invoke(station)
            })
        }
    }


    private fun ensurePositions(beginTile: ZoomXY) {
        if (lastBeginTile == beginTile) return
        positions.clear()
        for (s in stations) {
            positions += MapRasterTiles.getPixelPosition(
                s.latitude, s.longitude, beginTile.x, beginTile.y
            )
        }
        lastBeginTile = beginTile
    }
}
