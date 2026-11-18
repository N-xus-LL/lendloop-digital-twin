package lendloop.maps.raster

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import lendloop.core.locker.LockerLayer
import lendloop.maps.input.CameraController
import lendloop.maps.raster.dataclasses.Geolocation
import lendloop.maps.raster.dataclasses.ZoomXY
import lendloop.maps.raster.objects.Constants
import lendloop.maps.raster.objects.MapRasterTiles

class RasterMap : ApplicationAdapter(), InputProcessor {
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var tiledMap: TiledMap
    private lateinit var tiledMapRenderer: OrthogonalTiledMapRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var cameraController: CameraController
    private lateinit var mapTiles: List<Texture>
    private lateinit var beginTile: ZoomXY
    private val centerGeolocation = Geolocation(46.557636388845836, 15.64567869106685)
    private val markerPosition = Vector2() // Reused per frame to avoid allocation in drawMarkers().
    private lateinit var lockerLayer: LockerLayer

    override fun create() {
        shapeRenderer = ShapeRenderer()
        camera = OrthographicCamera().apply {
            setToOrtho(false, Constants.MAP_WIDTH.toFloat(), Constants.MAP_HEIGHT.toFloat())
            position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f, 0f)
            zoom = 0.75f
            update()
        }
        cameraController = CameraController(camera)
        Gdx.input.inputProcessor = this

        try {
            val centerTile = MapRasterTiles.getTileNumber(
                centerGeolocation.lat, centerGeolocation.lng, Constants.ZOOM
            )
            mapTiles = MapRasterTiles.getRasterTileZone(centerTile, Constants.NUM_TILES)
            beginTile = ZoomXY(
                Constants.ZOOM,
                centerTile.x - (Constants.NUM_TILES - 1) / 2,
                centerTile.y - (Constants.NUM_TILES - 1) / 2
            )
        } catch (e: Exception) {
            // Fail fast with context; swallowing here previously left mapTiles/
            // beginTile uninitialized and crashed in render().
            throw RuntimeException(
                "[RasterMap] Failed to load initial tile zone " +
                    "(center lat=${centerGeolocation.lat}, lng=${centerGeolocation.lng}, " +
                    "zoom=${Constants.ZOOM}, numTiles=${Constants.NUM_TILES})",
                e
            )
        }

        tiledMap = TiledMap()
        val layer = TiledMapTileLayer(
            Constants.NUM_TILES, Constants.NUM_TILES,
            MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE
        )

        var index = 0
        for (j in Constants.NUM_TILES - 1 downTo 0) {
            for (i in 0 until Constants.NUM_TILES) {
                val cell = TiledMapTileLayer.Cell().apply {
                    tile = StaticTiledMapTile(
                        TextureRegion(mapTiles[index], MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE)
                    )
                }
                layer.setCell(i, j, cell)
                index++
            }
        }
        tiledMap.layers.add(layer)

        tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap)

        lockerLayer = LockerLayer(shapeRenderer, "http://127.0.0.1:8080")
        lockerLayer.load()
    }

    override fun render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f)

        cameraController.update(Gdx.graphics.deltaTime)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) lockerLayer.toggleAvailabilityFilter()

        camera.update()
        tiledMapRenderer.setView(camera)
        tiledMapRenderer.render()

        drawMarkers()
        lockerLayer.render(camera, beginTile)
    }

    private fun drawMarkers() {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.color = Color.RED
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.circle(markerPosition.x, markerPosition.y, 10f)
        shapeRenderer.end()
    }

    override fun dispose() {
        shapeRenderer.dispose()
        tiledMapRenderer.dispose()
        tiledMap.dispose()
        mapTiles.forEach { it.dispose() }
    }


    // InputProcessor

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button != Input.Buttons.LEFT || pointer > 0) return false
        cameraController.beginDrag(screenX, screenY)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        cameraController.dragTo(screenX, screenY)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button != Input.Buttons.LEFT || pointer > 0) return false

        if (cameraController.endDrag()) {
            // Gesture was a click, not a pan.
            val world = camera.unproject(
                Vector3(screenX.toFloat(), screenY.toFloat(), 0f)
            )
            lockerLayer.onTap(world, beginTile)
        }
        return true
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        // Discard the endDrag() result — a canceled gesture must never count as a tap.
        cameraController.endDrag()
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (amountY == 0f) return false
        // One notch up (amountY > 0) should zoom in, i.e. decrease camera.zoom.
        val factor = if (amountY > 0f) 1f / 1.2f else 1.2f
        cameraController.zoomAt(Gdx.input.x, Gdx.input.y, factor)
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false
    override fun keyDown(keycode: Int): Boolean = false
    override fun keyUp(keycode: Int): Boolean = false
    override fun keyTyped(character: Char): Boolean = false
}
