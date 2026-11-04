package si.um.feri.maprri.lendloop;

import com.badlogic.gdx.ApplicationAdapter;
import si.um.feri.maprri.lendloop.map.RasterMap;

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
