package si.um.feri.maprri.raster.objects

import com.badlogic.gdx.Gdx

object Constants {
    const val NUM_TILES: Int = 3
    const val ZOOM: Int = 15
    const val MAP_WIDTH: Int  = MapRasterTiles.TILE_SIZE * NUM_TILES
    const val MAP_HEIGHT: Int = MapRasterTiles.TILE_SIZE * NUM_TILES
    val HUD_WIDTH: Int  = Gdx.graphics.width
    val HUD_HEIGHT: Int = Gdx.graphics.height
}
