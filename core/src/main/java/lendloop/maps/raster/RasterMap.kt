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
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapRenderer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import lendloop.core.locker.LockerLayer
import lendloop.maps.raster.dataclasses.Geolocation
import lendloop.maps.raster.dataclasses.ZoomXY
import lendloop.maps.raster.objects.Constants
import lendloop.maps.raster.objects.MapRasterTiles

class RasterMap : ApplicationAdapter(), GestureDetector.GestureListener, InputProcessor {
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var touchPosition: Vector3
    private lateinit var tiledMap: TiledMap
    private lateinit var tiledMapRenderer: TiledMapRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var mapTiles: List<Texture>
    private lateinit var beginTile: ZoomXY
    private val centerGeolocation = Geolocation(46.557314, 15.637771)
    private val markerGeolocation = Geolocation(46.559070, 15.638100)
    private val markerPosition = Vector2()  // Reused across frames to avoid per-frame allocation in drawMarkers().
    private lateinit var lockerLayer: LockerLayer

    val zoomSpeed = 2f
    val moveSpeed = 500f
    var dragging = false

    override fun create() {
        shapeRenderer = ShapeRenderer()

        camera = OrthographicCamera().apply {
            setToOrtho(false, Constants.MAP_WIDTH.toFloat(), Constants.MAP_HEIGHT.toFloat())
            position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f, 0f)
            viewportWidth = Constants.MAP_WIDTH / 2f
            viewportHeight = Constants.MAP_HEIGHT / 2f
            zoom = 2f
            update()
        }

        touchPosition = Vector3()

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
            // Java version swallowed the exception and left mapTiles/beginTile null, which then crashed in render().
            // Fail fast instead — same visual outcome (no render) but with a clear stack trace.
            throw RuntimeException("[EXCEPTION] Failed to load initial tile zone.", e)
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
        handleInput()
        camera.update()
        tiledMapRenderer.setView(camera)
        tiledMapRenderer.render()
        drawMarkers()
        lockerLayer.render(camera, beginTile)
    }

    private fun drawMarkers() {
        MapRasterTiles.getPixelPosition(
            markerGeolocation.lat, markerGeolocation.lng,
            beginTile.x, beginTile.y
        ).also { markerPosition.set(it) }

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.color = Color.RED
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.circle(markerPosition.x, markerPosition.y, 10f)
        shapeRenderer.end()
    }

    override fun dispose() { shapeRenderer.dispose() }


    // InputProcessor
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (!dragging) return false
        camera.unproject(touchPosition.set(screenX.toFloat(), screenY.toFloat(), 0f))

        val x = Gdx.input.deltaX.toFloat()
        val y = Gdx.input.deltaY.toFloat()
        camera.translate(-x,y)
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        camera.zoom += amountY * zoomSpeed * Gdx.graphics.deltaTime
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        if (button != Input.Buttons.LEFT || pointer > 0) return false

        val world = Vector3(screenX.toFloat(), screenY.toFloat(), 0f)
        camera.unproject(world)
        dragging = true

        lockerLayer.onTap(world, beginTile)
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button != Input.Buttons.LEFT || pointer > 0) return false

        val world = Vector3(screenX.toFloat(), screenY.toFloat(), 0f)
        camera.unproject(world)
        dragging = false

        lockerLayer.onTap(world, beginTile)
        return true
    }

    override fun touchCancelled(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        return false
    }


    // GestureListener
    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        touchPosition.set(x, y, 0f)
        camera.unproject(touchPosition)
        return false
    }
    override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean = false
    override fun longPress(x: Float, y: Float): Boolean = false
    override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean = false
    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
        camera.translate(-deltaX, deltaY)
        return false
    }
    override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean = false
    override fun zoom(initialDistance: Float, distance: Float): Boolean {
        if (initialDistance >= distance) camera.zoom += zoomSpeed * Gdx.graphics.deltaTime
        else camera.zoom -= zoomSpeed * Gdx.graphics.deltaTime
        return false
    }
    override fun pinch(
        initialPointer1: Vector2, initialPointer2: Vector2,
        pointer1: Vector2, pointer2: Vector2
    ): Boolean = false
    override fun pinchStop() { /* ... */ }

    // Input
    private fun handleInput() {
        val deltaTime = Gdx.graphics.deltaTime
        with(Gdx.input) {
            if (isKeyPressed(Input.Keys.Q)) camera.zoom += zoomSpeed * deltaTime
            if (isKeyPressed(Input.Keys.E)) camera.zoom -= zoomSpeed * deltaTime
            if (isKeyPressed(Input.Keys.LEFT) || isKeyPressed(Input.Keys.A))  camera.translate(-moveSpeed * deltaTime,  0f, 0f)
            if (isKeyPressed(Input.Keys.RIGHT) || isKeyPressed(Input.Keys.D)) camera.translate( moveSpeed * deltaTime,  0f, 0f)
            if (isKeyPressed(Input.Keys.DOWN) || isKeyPressed(Input.Keys.S))  camera.translate( 0f, -moveSpeed * deltaTime, 0f)
            if (isKeyPressed(Input.Keys.UP) || isKeyPressed(Input.Keys.W))    camera.translate( 0f,  moveSpeed * deltaTime, 0f)
            if (isKeyJustPressed(Input.Keys.F)) lockerLayer.toggleAvailabilityFilter()
        }

        camera.zoom = MathUtils.clamp(camera.zoom, 0.5f, 2f)
        val effectiveViewportWidth = camera.viewportWidth * camera.zoom
        val effectiveViewportHeight = camera.viewportHeight * camera.zoom

        camera.position.x = MathUtils.clamp(
            camera.position.x,
            effectiveViewportWidth / 2f,
            Constants.MAP_WIDTH - effectiveViewportWidth / 2f
        )
        camera.position.y = MathUtils.clamp(
            camera.position.y,
            effectiveViewportHeight / 2f,
            Constants.MAP_HEIGHT - effectiveViewportHeight / 2f
        )
    }
}
