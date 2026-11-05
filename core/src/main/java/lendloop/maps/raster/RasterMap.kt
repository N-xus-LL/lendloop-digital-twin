package lendloop.maps.raster

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
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
import lendloop.maps.raster.objects.Constants
import lendloop.maps.raster.dataclasses.Geolocation
import lendloop.maps.raster.objects.MapRasterTiles
import lendloop.maps.raster.dataclasses.ZoomXY

class RasterMap : ApplicationAdapter(), GestureDetector.GestureListener {
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
    }

    override fun render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        handleInput()
        camera.update()
        tiledMapRenderer.setView(camera)
        tiledMapRenderer.render()
        drawMarkers()
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
        if (initialDistance >= distance) camera.zoom += 0.02f
        else camera.zoom -= 0.02f
        return false
    }
    override fun pinch(
        initialPointer1: Vector2, initialPointer2: Vector2,
        pointer1: Vector2, pointer2: Vector2
    ): Boolean = false
    override fun pinchStop() { /* ... */ }

    // Input
    private fun handleInput() {
        with(Gdx.input) {
            if (isKeyPressed(Input.Keys.PLUS))  camera.zoom += 0.02f
            if (isKeyPressed(Input.Keys.MINUS)) camera.zoom -= 0.02f
            if (isKeyPressed(Input.Keys.LEFT))  camera.translate(-3f,  0f, 0f)
            if (isKeyPressed(Input.Keys.RIGHT)) camera.translate( 3f,  0f, 0f)
            if (isKeyPressed(Input.Keys.DOWN))  camera.translate( 0f, -3f, 0f)
            if (isKeyPressed(Input.Keys.UP))    camera.translate( 0f,  3f, 0f)
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
