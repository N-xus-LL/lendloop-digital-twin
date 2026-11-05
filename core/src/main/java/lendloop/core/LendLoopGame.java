package lendloop.core;

import com.badlogic.gdx.ApplicationAdapter;
import lendloop.maps.raster.RasterMap;

public class LendLoopGame extends ApplicationAdapter {
    private RasterMap rasterMap;

    @Override
    public void create() {
        rasterMap = new RasterMap();
        rasterMap.create();
    }

    @Override
    public void render() {
        rasterMap.render();
    }

    @Override
    public void resize(int width, int height) {
        rasterMap.resize(width, height);
    }

    @Override
    public void dispose() {
        rasterMap.dispose();
    }
}
