package com.alex.tetris;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MainMenuScreen implements Screen {
    private final TetrisGame game;
    private Stage stage;

    public MainMenuScreen(TetrisGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(TetrisGame.VIRTUAL_WIDTH, TetrisGame.VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        createButtons();
    }

    private void createButtons() {
        // Crea una skin básica (blanca)
        Skin skin = createBasicSkin();

        // Botón "Nueva Partida"
        TextButton newGameBtn = new TextButton("Nueva Partida", skin);
        newGameBtn.setPosition(
            TetrisGame.VIRTUAL_WIDTH/2 - 100,
            TetrisGame.VIRTUAL_HEIGHT/2 + 40
        );
        newGameBtn.setSize(200, 60);

        // Botón "Ajustes"
        TextButton settingsBtn = new TextButton("Ajustes", skin);
        settingsBtn.setPosition(
            TetrisGame.VIRTUAL_WIDTH/2 - 100,
            TetrisGame.VIRTUAL_HEIGHT/2 - 40
        );
        settingsBtn.setSize(200, 60);

        // Listeners
        newGameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
            }
        });

        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
//                game.setScreen(new SettingsScreen(game));
            }
        });

        stage.addActor(newGameBtn);
        stage.addActor(settingsBtn);
    }

    // Skin básica blanca
    private Skin createBasicSkin() {
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = skin.getFont("default-font");

        // Usa la textura blanca que acabamos de crear
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.GRAY);
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);

        skin.add("default", textButtonStyle);

        return skin;
    }

    @Override
    public void render(float delta) {
        // Fondo negro
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Dibuja los botones
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    // Métodos no usados
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
