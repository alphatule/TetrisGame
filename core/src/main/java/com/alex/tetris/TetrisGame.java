package com.alex.tetris;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class TetrisGame extends ApplicationAdapter {

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;

    public static final float VIRTUAL_WIDTH = 480;
    public static final float VIRTUAL_HEIGHT = 800;


    // Game Default Settings
    private static final int BOARD_COLUMNS = 10;
    private static final int BOARD_ROWS = 20;
    private static final int CELL_SIZE = 32; // Tamaño de cada celda en píxeles


    // Otras variables de texturas
    private Texture cellTexture;

    @Override
    public void create () {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();
        camera.position.set(VIRTUAL_WIDTH / 2, VIRTUAL_HEIGHT / 2, 0);
        camera.update();
        cellTexture = createCellTexture(CELL_SIZE);
    }

    @Override
    public void render () {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        // Aquí dibujaremos la grilla, las piezas, etc.
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose () {
        batch.dispose();
    }


    // Metodos privados para el juego


    private Texture createCellTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 200); // Blanco

        // Dibujar borde
        pixmap.drawRectangle(0, 0, size, size);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }



}
