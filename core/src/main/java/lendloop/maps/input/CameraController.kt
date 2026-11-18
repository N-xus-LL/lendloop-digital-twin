package lendloop.maps.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import lendloop.isDebug
import lendloop.maps.raster.objects.Constants

/**
 * Owns all camera navigation: keyboard movement, drag-pan, and cursor-anchored zoom.
 *
 * Kept free of libGDX input-processor overrides so it is unit-testable and reusable.
 * [lendloop.maps.raster.RasterMap] forwards its [com.badlogic.gdx.InputProcessor]
 * callbacks here.
 *
 * Conventions:
 *  - [OrthographicCamera.zoom] follows libGDX semantics: larger = zoomed OUT.
 *  - All public screen coordinates are libGDX screen pixels (y-down).
 *  - All drag math is done in world space via [OrthographicCamera.unproject], so
 *    panning is exact 1:1 with the cursor at any zoom.
 */
class CameraController(
    private val camera: OrthographicCamera,
    private val worldWidth: Float = Constants.MAP_WIDTH.toFloat(),
    private val worldHeight: Float = Constants.MAP_HEIGHT.toFloat(),
    private val minZoom: Float = 0.5f,
    private val maxZoom: Float = 1f,
    private val moveSpeed: Float = 500f,
    private val moveSpeedupFactor: Float = 5f,
    private val zoomSpeed: Float = 0.5f,
    private val zoomSpeedupFactor: Float = 0.5f,
    private val scrollZoomStep: Float = 1.2f,
    private val clickDragThresholdPx: Float = 8f * Gdx.graphics.density,
) {
    private val tmp = Vector3()
    private val dragStartScreen = Vector2()
    private val lastWorld = Vector2()
    private var dragging = false
    private var dragMoved = false

    val isDragging: Boolean get() = dragging

    /** Call once per frame before [OrthographicCamera.update]. */
    fun update(deltaTime: Float) {
        // Scale by zoom so on-screen movement speed is constant regardless of zoom level.
        val movePerFrame = moveSpeed * deltaTime * camera.zoom
        val zoomPerFrame = zoomSpeed * deltaTime

        with(Gdx.input) {
            var shiftPressed = false
            if (isKeyPressed(Input.Keys.SHIFT_LEFT)
                || isKeyPressed(Input.Keys.SHIFT_RIGHT))
                shiftPressed = true

            val moveSpeedupPerFrame = moveSpeedupFactor * shiftPressed.toInt()
            val zoomSpeedupPerFrame = zoomSpeedupFactor * shiftPressed.toInt()

            if (isKeyPressed(Input.Keys.MINUS))
                setZoom(camera.zoom + (zoomPerFrame * (1 + zoomSpeedupPerFrame) ) )
            if (isKeyPressed(Input.Keys.PLUS)
                || (shiftPressed && isKeyPressed(Input.Keys.EQUALS) ))
                setZoom(camera.zoom - (zoomPerFrame * (1 + zoomSpeedupPerFrame) ) )

            if (isKeyPressed(Input.Keys.LEFT) || isKeyPressed(Input.Keys.A))
                camera.translate(-(movePerFrame * (1 + moveSpeedupPerFrame)), 0f, 0f)
            if (isKeyPressed(Input.Keys.RIGHT) || isKeyPressed(Input.Keys.D))
                camera.translate( (movePerFrame * (1 + moveSpeedupPerFrame) ), 0f, 0f)
            if (isKeyPressed(Input.Keys.DOWN) || isKeyPressed(Input.Keys.S))
                camera.translate(0f, -(movePerFrame * (1 + moveSpeedupPerFrame)), 0f)
            if (isKeyPressed(Input.Keys.UP) || isKeyPressed(Input.Keys.W))
                camera.translate(0f,  (movePerFrame * (1 + moveSpeedupPerFrame)), 0f)
        }

        clampPosition()
    }

    /** Begin a drag gesture. Call from [com.badlogic.gdx.InputProcessor.touchDown]. */
    fun beginDrag(screenX: Int, screenY: Int) {
        dragging = true
        dragMoved = false
        dragStartScreen.set(screenX.toFloat(), screenY.toFloat())
        camera.unproject(tmp.set(screenX.toFloat(), screenY.toFloat(), 0f))
        lastWorld.set(tmp.x, tmp.y)
    }

    /** Continue a drag. No-op if [beginDrag] was not called. */
    fun dragTo(screenX: Int, screenY: Int) {
        if (!dragging) return

        if (dragStartScreen.dst(screenX.toFloat(), screenY.toFloat()) > clickDragThresholdPx) {
            dragMoved = true
        }

        camera.unproject(tmp.set(screenX.toFloat(), screenY.toFloat(), 0f))
        // Translate by the world-space delta between cursor positions.
        camera.translate(lastWorld.x - tmp.x, lastWorld.y - tmp.y, 0f)
        lastWorld.set(tmp.x, tmp.y)

        clampPosition()
    }

    /**
     * End a drag. Returns `true` if the gesture never exceeded the click/drag
     * threshold — i.e. it was a click, not a pan. Callers use this to gate taps.
     */
    fun endDrag(): Boolean {
        val wasClick = dragging && !dragMoved
        dragging = false
        dragMoved = false
        return wasClick
    }

    /**
     * Multiply the current zoom by [factor], anchored on the given screen point.
     * `factor < 1` zooms in, `factor > 1` zooms out (libGDX zoom semantics).
     */
    fun zoomAt(screenX: Int, screenY: Int, factor: Float) {
        // World point under the cursor before zooming.
        camera.unproject(tmp.set(screenX.toFloat(), screenY.toFloat(), 0f))
        val anchorWorldX = tmp.x
        val anchorWorldY = tmp.y

        setZoom(camera.zoom * factor)

        // Recompute where that screen point now lands in the world, and shift the
        // camera so the anchor world point stays under the cursor.
        camera.update()
        camera.unproject(tmp.set(screenX.toFloat(), screenY.toFloat(), 0f))
        camera.translate(anchorWorldX - tmp.x, anchorWorldY - tmp.y, 0f)

        clampPosition()
    }

    private fun setZoom(target: Float) {
        val clamped = target.coerceIn(minZoom, maxZoom)
        if (camera.zoom == clamped) return
        camera.zoom = clamped
        clampPosition()
        if (!isDebug) Gdx.app.log("CameraController", "cameraZoom = ${camera.zoom}")
    }

    private fun clampPosition() {
        val halfW = camera.viewportWidth  * camera.zoom / 2f
        val halfH = camera.viewportHeight * camera.zoom / 2f

        // If the viewport is wider/taller than the world at this zoom, the
        // `coerceIn(min, max)` range would be inverted; center instead.
        camera.position.x =
            if (halfW * 2f >= worldWidth)  worldWidth / 2f
            else camera.position.x.coerceIn(halfW, worldWidth - halfW)

        camera.position.y =
            if (halfH * 2f >= worldHeight) worldHeight / 2f
            else camera.position.y.coerceIn(halfH, worldHeight - halfH)
    }
}

fun Boolean.toInt() = if (this) 1 else 0
