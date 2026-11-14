package lendloop.core.locker

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import lendloop.maps.raster.dataclasses.ZoomXY
import lendloop.maps.raster.objects.MapRasterTiles

class LockerMarkerRenderer(private val shapes: ShapeRenderer) {

    var stations: List<LockerStation> = emptyList()
    private set

    fun setStations(newStations: List<LockerStation>?) {
        stations = newStations ?: emptyList()
    }

    fun render(beginTile: ZoomXY) {
        if (stations.isEmpty()) return

            shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (s in stations) {
            val pos = MapRasterTiles.getPixelPosition(
                s.latitude, s.longitude, beginTile.x, beginTile.y
            )

            // Radius grows with size, capped so a mega-station doesn't dominate.
            val radius = 6f + s.lockerCount.coerceAtMost(20) * 0.5f

            // Subtle white outline
            shapes.color = Color.WHITE
            shapes.circle(pos.x, pos.y, radius + 1.5f)

            // Fill color = status
            shapes.color = s.status.color
            shapes.circle(pos.x, pos.y, radius)

            // "Free count" pip in the center for stations that have any
            if (s.availableCount > 0) {
                shapes.color = Color.WHITE
                shapes.circle(pos.x, pos.y, 1.5f.coerceAtLeast(radius * 0.25f))
            }
        }
        shapes.end()
    }
}
