package com.alex.tetris;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class TetrisGame extends Game {
    public static final float VIRTUAL_WIDTH = 480;
    public static final float VIRTUAL_HEIGHT = 800;
    // Game Default Settings
    public static final int BOARD_COLUMNS = 10;
    public static final int BOARD_ROWS = 20;
    public static final int CELL_SIZE = 35; // Tamaño de cada celda en píxeles

    public static final float DEFAULT_DROP_INTERVAL = 0.3f;

    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
