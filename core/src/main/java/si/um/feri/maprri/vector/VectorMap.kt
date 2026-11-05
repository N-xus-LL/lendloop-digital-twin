package si.um.feri.maprri.vector

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class VectorMap : ApplicationAdapter() {

    private lateinit var map: VectorMapRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var shapeRenderer: ShapeRenderer

    private val zoomSpeed = 0.02f

    override fun create() {
        shapeRenderer = ShapeRenderer()
        camera = OrthographicCamera(
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat()
        )
        map = VectorMapRenderer(camera, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 0f, 0f)
        camera.update()
    }

    override fun render() {
        handleInput()

        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Note: VectorMapRenderer.render() already sets the projection matrix
        // on its own ShapeRenderer; this line targets *this* class's shapeRenderer,
        // which is never used for drawing. Kept for parity, harmless.
        shapeRenderer.projectionMatrix = camera.combined

        map.render()
    }

    private fun handleInput() {
        val moveSpeed = 5f / map.scale
        with(Gdx.input) {
            if (isKeyPressed(Input.Keys.LEFT))  map.offsetX -= moveSpeed
            if (isKeyPressed(Input.Keys.RIGHT)) map.offsetX += moveSpeed
            if (isKeyPressed(Input.Keys.UP))    map.offsetY += moveSpeed
            if (isKeyPressed(Input.Keys.DOWN))  map.offsetY -= moveSpeed
            if (isKeyPressed(Input.Keys.A))     map.scale *= (1 + zoomSpeed)
            if (isKeyPressed(Input.Keys.S))     map.scale *= (1 - zoomSpeed)
        }
    }

    override fun resize(width: Int, height: Int) {
        map.viewport.update(width, height)
        map.viewport.apply()            // FitViewport needs apply() after update()
    }

    override fun dispose() {
        map.dispose()
        shapeRenderer.dispose()
    }
}
