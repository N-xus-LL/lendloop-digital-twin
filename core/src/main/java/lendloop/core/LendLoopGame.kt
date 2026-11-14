package lendloop.core

import com.badlogic.gdx.ApplicationAdapter
import lendloop.maps.raster.RasterMap

class LendLoopGame : ApplicationAdapter() {
    private var rasterMap: RasterMap = RasterMap()

    override fun create() { rasterMap.create() }
    override fun render() { rasterMap.render() }
    override fun resize(width: Int, height: Int) { rasterMap.resize(width, height) }
    override fun dispose() { rasterMap.dispose() }
}
