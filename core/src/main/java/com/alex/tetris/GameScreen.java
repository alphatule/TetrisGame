package com.alex.tetris;

import static com.alex.tetris.TetrisGame.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Arrays;

public class GameScreen implements Screen {
    private final TetrisGame game;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Stage stage;

    private Texture pauseOverlay;
    private Stage pauseStage;
    private boolean isPaused = false;
    private Skin skin;


    // Otras variables de texturas
    private Texture cellTexture;
    private Texture[] pieceTextures;
    // ghost position
    private Texture ghostPieceTexture; // Textura semitransparente
    private TetrisPiece ghostPiece;   // Pieza fantasma
    // next piece
    private TetrisPiece nextPiece;

    // Sistema de puntuación
    private int score = 0;
    private BitmapFont font;
    private Preferences prefs;
    private int highScore = 0;


    // Piezas y colores
    private static final Color[] PIECE_COLORS = {
        Color.CYAN,
        Color.BLUE,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.RED,
        Color.GOLD
    };

    private static final int[][][] SHAPES = {
        {{1, 1, 1, 1}},             // I (índice 0)
        {{1, 1, 1}, {1, 0, 0}},     // L (índice 1)
        {{1, 1, 1}, {0, 0, 1}},     // J (índice 2)
        {{1, 1}, {1, 1}},           // O (índice 3)
        {{1, 1, 0}, {0, 1, 1}},     // S (índice 4)
        {{0, 1, 1}, {1, 1, 0}},     // Z (índice 5)
        {{1, 1, 1}, {0, 1, 0}}     // .|. (índice 6)
    };

    // Estado del juego
    private TetrisPiece currentPiece;
    private int[][] board = new int[BOARD_ROWS][BOARD_COLUMNS];
    private float dropTimer = 0;
    private float dropInterval = DEFAULT_DROP_INTERVAL;

    // Control táctil
    private final Vector3 initialTouchPos = new Vector3();
    private float touchTime = 0;
    private boolean rotationPerformed = false;
    // Desliz
    private final float MAX_TAP_DISTANCE = 15f;
    private final float TAP_MAX_DURATION = 0.3f;
    // Desliz horizontal
    private final float MIN_SWIPE_DISTANCE = 40f;
    // Desliz vertical
    private boolean fastDropActive = false;
    private final float MIN_SWIPE_VERTICAL_DISTANCE = 60f; // Píxeles para activar caída rápida
    private final float FAST_DROP_SPEED = 0.02f; // Intervalo de caída rápida (más pequeño = más rápido)

    // Sonidos
    private Sound placeSound;
    private Sound clearLineSound;

    private void initSounds() {
        placeSound = Gdx.audio.newSound(Gdx.files.internal("place.mp3"));
        clearLineSound = Gdx.audio.newSound(Gdx.files.internal("clear.mp3"));
    }

    private void initTextures() {
        cellTexture = createCellTexture(CELL_SIZE);
        ghostPieceTexture = createGhostTexture(CELL_SIZE);
        pieceTextures = new Texture[PIECE_COLORS.length];
        for (int i = 0; i < PIECE_COLORS.length; i++) {
            pieceTextures[i] = createColoredTexture(CELL_SIZE, PIECE_COLORS[i]);
        }
    }

    private void handleInput() {
        handleTouchInput();
    }

    private void updateGame() {
        updateGhostPiece();
        dropTimer += Gdx.graphics.getDeltaTime();
        if (dropTimer >= dropInterval) {
            movePieceDown();
            dropTimer = 0;
        }
    }

    private void draw() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        drawBoard();
        drawGhostPiece();
        drawCurrentPiece();
        drawNextPiece();

        // draw puntuacion
        font.draw(batch, "Puntos: " + score, 20, VIRTUAL_HEIGHT);
        font.draw(batch, "Récord: " + highScore, 20, VIRTUAL_HEIGHT - 25);

        batch.end();
    }

    // Metodos de interacciones
    private void handleTouchInput() {
        if (Gdx.input.justTouched()) {
            initialTouchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(initialTouchPos);
            touchTime = 0;
            rotationPerformed = false; // Resetear al nuevo toque
        }

        if (Gdx.input.isTouched()) {
            touchTime += Gdx.graphics.getDeltaTime();

            Vector3 currentPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(currentPos);

            float deltaX = currentPos.x - initialTouchPos.x;
            float deltaY = currentPos.y - initialTouchPos.y;

            // Detectar deslizamiento (prioridad sobre rotación)
            if (Math.abs(deltaX) > MIN_SWIPE_DISTANCE) {
                movePiece((int)Math.signum(deltaX));
                initialTouchPos.set(currentPos);
                rotationPerformed = true;
            } else if (deltaY < -MIN_SWIPE_VERTICAL_DISTANCE) { // deltaY negativo = hacia abajo
                activateFastDrop();
                rotationPerformed = true;
            }
        }

        // Rotación al finalizar toque (sin deslizamiento)
        if (!Gdx.input.isTouched() &&
            touchTime > 0 &&
            touchTime < TAP_MAX_DURATION &&
            !rotationPerformed) {

            Vector3 finalPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(finalPos);

            if (initialTouchPos.dst(finalPos) < MAX_TAP_DISTANCE) {
                rotatePiece();
                rotationPerformed = true;
            }
        }
    }

    // Metodos para dibujar piezas en el tablero

    private void activateFastDrop() {
        fastDropActive = true;
        dropInterval = FAST_DROP_SPEED; // Cambia la velocidad de caída
    }

    private void deactivateFastDrop() {
        fastDropActive = false;
        dropInterval = DEFAULT_DROP_INTERVAL; // Vuelve a la velocidad normal
    }

    private void rotatePiece() {
        Gdx.app.log("DEBUG", "ROTAMOS");
        if (currentPiece == null) return;

        // 1. Rotar la matriz de la pieza
        int[][] rotated = new int[currentPiece.shape[0].length][currentPiece.shape.length];
        for (int i = 0; i < currentPiece.shape.length; i++) {
            for (int j = 0; j < currentPiece.shape[0].length; j++) {
                rotated[j][currentPiece.shape.length - 1 - i] = currentPiece.shape[i][j];
            }
        }

        // 2. Probar rotación en 5 posiciones diferentes (wall kicks)
        int[][] testPositions = {
            {currentPiece.x, currentPiece.y},     // Posición original
            {currentPiece.x + 1, currentPiece.y}, // Derecha
            {currentPiece.x - 1, currentPiece.y}, // Izquierda
            {currentPiece.x, currentPiece.y - 1}, // Abajo
            {currentPiece.x, currentPiece.y + 1}  // Arriba
        };

        for (int[] position : testPositions) {
            TetrisPiece testPiece = new TetrisPiece(rotated, currentPiece.color);
            testPiece.x = position[0];
            testPiece.y = position[1];

            if (!checkCollision(testPiece)) {
                currentPiece.shape = rotated;
                currentPiece.x = position[0];
                currentPiece.y = position[1];
                Gdx.app.log("ROTATION", "Rotated successfully");
                return;
            }
        }
        updateGhostPiece();

        Gdx.app.log("ROTATION", "Rotation failed - no valid position");
    }

    private void drawBoard() {
        float startX = (VIRTUAL_WIDTH - BOARD_COLUMNS * CELL_SIZE) / 2;
        float startY = (VIRTUAL_HEIGHT - BOARD_ROWS * CELL_SIZE) / 2;

        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLUMNS; col++) {
                int cellValue = board[row][col];
                Texture texture;

                if (cellValue == 0) {
                    texture = cellTexture; // Celda vacía
                } else {
                    texture = pieceTextures[cellValue - 1]; // Restamos 1 para obtener el índice correcto
                }

                batch.draw(texture,
                    startX + col * CELL_SIZE,
                    startY + row * CELL_SIZE);
            }
        }
    }

    private void drawCurrentPiece() {
        if (currentPiece != null) {
            float startX = (VIRTUAL_WIDTH - BOARD_COLUMNS * CELL_SIZE) / 2;
            float startY = (VIRTUAL_HEIGHT - BOARD_ROWS * CELL_SIZE) / 2;

            // Encuentra el índice del color para obtener la textura correcta
            int colorIndex = Arrays.asList(PIECE_COLORS).indexOf(currentPiece.color);
            Texture pieceTexture = pieceTextures[colorIndex];

            for (int row = 0; row < currentPiece.shape.length; row++) {
                for (int col = 0; col < currentPiece.shape[row].length; col++) {
                    if (currentPiece.shape[row][col] == 1) {
                        int drawX = currentPiece.x + col;
                        int drawY = currentPiece.y + row;

                        if (drawY >= 0) {
                            float posX = startX + drawX * CELL_SIZE;
                            float posY = startY + drawY * CELL_SIZE;
                            batch.draw(pieceTexture, posX, posY);
                        }
                    }
                }
            }
        }
    }

    private void drawGhostPiece() {
        if (ghostPiece == null) return;

        float startX = (VIRTUAL_WIDTH - BOARD_COLUMNS * CELL_SIZE) / 2;
        float startY = (VIRTUAL_HEIGHT - BOARD_ROWS * CELL_SIZE) / 2;

        for (int row = 0; row < ghostPiece.shape.length; row++) {
            for (int col = 0; col < ghostPiece.shape[row].length; col++) {
                if (ghostPiece.shape[row][col] != 0) {
                    int drawX = ghostPiece.x + col;
                    int drawY = ghostPiece.y + row;

                    if (drawY >= 0) {
                        float posX = startX + drawX * CELL_SIZE;
                        float posY = startY + drawY * CELL_SIZE;
                        batch.draw(ghostPieceTexture, posX, posY);
                    }
                }
            }
        }
        if (fastDropActive) {
            batch.setColor(1, 0.5f, 0.5f, 0.7f); // Tono rojizo durante caída rápida
            // Dibuja la pieza fantasma
            batch.setColor(Color.WHITE); // Restablece el color
        }
    }

    private void drawNextPiece() {
        if (nextPiece == null) return;

        float previewX = camera.position.x + viewport.getWorldWidth() / 2f - 100;
        float previewY = camera.position.y + viewport.getWorldHeight() / 2f - 150;
        skin.getFont("default-font").draw(batch, "Siguiente:", previewX, previewY + 65);

        int colorIndex = Arrays.asList(PIECE_COLORS).indexOf(nextPiece.color);
        Texture texture = pieceTextures[colorIndex];

        for (int row = 0; row < nextPiece.shape.length; row++) {
            for (int col = 0; col < nextPiece.shape[row].length; col++) {
                if (nextPiece.shape[row][col] != 0) {
                    float x = previewX + col * (CELL_SIZE / 1.5f);
                    float y = previewY + (nextPiece.shape.length - 1 - row) * (CELL_SIZE / 1.5f); // Dibuja desde arriba

                    batch.draw(texture, x, y, CELL_SIZE / 1.5f, CELL_SIZE / 1.5f);
                }
            }
        }
    }

    // Metodos para crear piezas nuevas y colores

    private Texture createGhostTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 0.3f); // Blanco semitransparente
        pixmap.fill();
        pixmap.setColor(1, 1, 1, 0.6f);
        pixmap.drawRectangle(0, 0, size, size);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void createPauseButton() {
        TextButton pauseBtn = new TextButton("II", skin, "pause");
        pauseBtn.getLabel().setFontScale(1.5f);

        // Posición en esquina superior derecha
        pauseBtn.setPosition(
            VIRTUAL_WIDTH - 70,
            VIRTUAL_HEIGHT - 70
        );
        pauseBtn.setSize(60, 60);

        pauseBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                togglePause();
            }
        });

        stage.addActor(pauseBtn);
    }

    private void updateGhostPiece() {
        if (currentPiece == null) return;

        // Copia la pieza actual
        ghostPiece = new TetrisPiece(currentPiece.shape, currentPiece.color);
        ghostPiece.x = currentPiece.x;
        ghostPiece.y = currentPiece.y;

        // Simula la caída
        while (!checkCollision(ghostPiece)) {
            ghostPiece.y--;
        }
        ghostPiece.y++; // Retrocede un paso al detectar colisión
    }

    private Texture createCellTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Rellenar con color gris (R=0.5, G=0.5, B=0.5, Alpha=1)
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1f);
        pixmap.fill();

        // Dibujar borde blanco
        pixmap.setColor(1, 1, 1, 1); // Blanco sólido
        pixmap.drawRectangle(0, 0, size, size);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private Texture createColoredTexture(int size, Color color) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        pixmap.drawRectangle(0, 0, size, size);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    // Metodos privados para el juego

    private void movePiece(int direction) {
        if (currentPiece == null) return;

        TetrisPiece testPiece = new TetrisPiece(currentPiece.shape, currentPiece.color);
        testPiece.x = currentPiece.x + direction;
        testPiece.y = currentPiece.y;

        if (!checkCollision(testPiece)) {
            currentPiece.x += direction;
        }
        updateGhostPiece();
    }

    private TetrisPiece generateRandomPiece() {
        int shapeIndex = MathUtils.random(0, SHAPES.length - 1);
        return new TetrisPiece(SHAPES[shapeIndex], PIECE_COLORS[shapeIndex]);
    }

    public void spawnNewPiece() {
        if (nextPiece == null) {
            // Si es la primera vez, inicializa ambas
            nextPiece = generateRandomPiece();
        }

        currentPiece = nextPiece;
        nextPiece = generateRandomPiece();
        updateGhostPiece();
    }

    private boolean checkCollision(TetrisPiece piece) {
        for (int row = 0; row < piece.shape.length; row++) {
            for (int col = 0; col < piece.shape[row].length; col++) {
                if (piece.shape[row][col] != 0) {  // Cambiado a != 0 para mayor flexibilidad
                    int boardX = piece.x + col;
                    int boardY = piece.y + row;

                    // Verificar límites del tablero
                    if (boardX < 0 || boardX >= BOARD_COLUMNS || boardY < 0) {
                        return true;
                    }

                    // Verificar colisión con piezas ya colocadas
                    if (boardY < BOARD_ROWS && board[boardY][boardX] != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void movePieceDown() {
        if (currentPiece == null) return;

        // Crear copia para prueba
        TetrisPiece testPiece = new TetrisPiece(
            Arrays.copyOf(currentPiece.shape, currentPiece.shape.length),
            currentPiece.color
        );
        testPiece.x = currentPiece.x;
        testPiece.y = currentPiece.y - 1;

        if (!checkCollision(testPiece)) {
            currentPiece.y--;
        } else {
            // Solo colocar la pieza si al menos parte de ella está dentro del tablero
            boolean shouldPlace = false;
            for (int row = 0; row < currentPiece.shape.length; row++) {
                for (int col = 0; col < currentPiece.shape[row].length; col++) {
                    if (currentPiece.shape[row][col] != 0 &&
                        (currentPiece.y + row) >= 0) {
                        shouldPlace = true;
                        break;
                    }
                }
                if (shouldPlace) break;
            }

            if (shouldPlace) {
                placePiece();
                deactivateFastDrop();
            }
            spawnNewPiece();

            // Game over si la nueva pieza colisiona inmediatamente
            if (checkCollision(currentPiece)) {
                if (score > highScore) {
                    highScore = score;
                    prefs.putInteger("highScore", highScore);
                    prefs.flush();
                }
                Gdx.app.log("Game", "Game Over!");
                game.setScreen(new MainMenuScreen(game));
            }
        }
    }

    private void placePiece() {
        int colorIndex = Arrays.asList(PIECE_COLORS).indexOf(currentPiece.color) + 1; // +1 aquí

        for (int row = 0; row < currentPiece.shape.length; row++) {
            for (int col = 0; col < currentPiece.shape[row].length; col++) {
                if (currentPiece.shape[row][col] != 0) {
                    int boardX = currentPiece.x + col;
                    int boardY = currentPiece.y + row;

                    if (boardY >= 0 && boardY < BOARD_ROWS && boardX >= 0 && boardX < BOARD_COLUMNS) {
                        board[boardY][boardX] = colorIndex; // Ya no necesitas +1 aquí
                    }
                }
            }
        }
        placeSound.play(0.5f);
        checkCompleteLines();
//        debugBoard();
    }

    private void checkCompleteLines() {
        int linesCleared = 0;

        for (int row = 0; row < BOARD_ROWS; row++) {
            boolean lineComplete = true;
            for (int col = 0; col < BOARD_COLUMNS; col++) {
                if (board[row][col] == 0) {
                    lineComplete = false;
                    break;
                }
            }

            if (lineComplete) {
                for (int r = row; r < BOARD_ROWS - 1; r++) {
                    System.arraycopy(board[r + 1], 0, board[r], 0, BOARD_COLUMNS);
                }
                Arrays.fill(board[BOARD_ROWS - 1], 0);
                row--;
                linesCleared++;
            }
        }

        // 🎯 Aumentar puntuación
        if (linesCleared > 0) {
            clearLineSound.play(0.8f);
            switch (linesCleared) {
                case 1: score += 100; break;
                case 2: score += 300; break;
                case 3: score += 500; break;
                case 4: score += 800; break; // Tetris
                default: score += linesCleared * 200;
            }
        }
    }

    // Menu de pausa

    private void drawPauseMenu() {
        // Dibuja overlay oscuro
        batch.begin();
        batch.setColor(1, 1, 1, 1);
        batch.draw(pauseOverlay, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch.end();

        // Dibuja el menú de pausa
        pauseStage.act(Gdx.graphics.getDeltaTime());
        pauseStage.draw();
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.up = skin.newDrawable("white", new Color(0.25f, 0.25f, 0.25f, 0.8f));
        buttonStyle.down = skin.newDrawable("white", new Color(0.4f, 0.4f, 0.4f, 0.8f));
        buttonStyle.over = skin.newDrawable("white", new Color(0.35f, 0.35f, 0.35f, 0.8f));
        skin.add("default", buttonStyle);

        TextButton.TextButtonStyle pauseButtonStyle = new TextButton.TextButtonStyle();
        pauseButtonStyle.font = font;
        pauseButtonStyle.up = null;
        pauseButtonStyle.down = null;
        pauseButtonStyle.over = null;
        skin.add("pause", pauseButtonStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        return skin;
    }

    private void createPauseMenu() {
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // Title
        Label title = new Label("RESUMEN", skin);
        title.setFontScale(1.5f);
        table.add(title).padBottom(40f).row();

        // boton continuar
        TextButton resumeBtn = new TextButton("Continuar", skin);
        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                resumeGame();
            }
        });
        table.add(resumeBtn).width(200).height(60).padBottom(20f).row();

        // boton menu pp
        TextButton menuBtn = new TextButton("Menú Principal", skin);
        menuBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        table.add(menuBtn).width(200).height(60);

        pauseStage.addActor(table);
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            Gdx.input.setInputProcessor(pauseStage);
        } else {
            Gdx.input.setInputProcessor(stage); // O tu input processor principal
        }
    }

    private void resumeGame() {
        isPaused = false;
        Gdx.input.setInputProcessor(stage); // O tu input processor principal
    }

    // Debugs
    private void debugBoard() {
        StringBuilder sb = new StringBuilder("\nBoard State:\n");
        for (int y = BOARD_ROWS-1; y >= 0; y--) {
            sb.append("|");
            for (int x = 0; x < BOARD_COLUMNS; x++) {
                sb.append(board[y][x] == 0 ? " " : "X");
            }
            sb.append("|\n");
        }
        Gdx.app.log("DEBUG", sb.toString());
    }

    // Constructor y metodos de SCREEN

    public GameScreen(TetrisGame game) {
        this.game = game;
        batch = new SpriteBatch();
    }

    @Override
    public void show() {
        // Configuración cuando se muestra la pantalla

        // Borrar maxima puntuación
//        prefs.remove("highScore");
//        prefs.flush();

        prefs = Gdx.app.getPreferences("tetris_prefs");
        highScore = prefs.getInteger("highScore", 0); // valor por defecto 0

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();
        camera.position.set(VIRTUAL_WIDTH / 2, VIRTUAL_HEIGHT / 2, 0);
        camera.update();

        board = new int[BOARD_ROWS][BOARD_COLUMNS]; // Inicializar tablero
        initTextures();
        initSounds();

        // Genera la primera pieza y el bucle que va generando las piezas.
        spawnNewPiece();

        // Cosas del menu de pausa
        pauseStage = new Stage(new FitViewport(TetrisGame.VIRTUAL_WIDTH, TetrisGame.VIRTUAL_HEIGHT));

        // Crea la skin (reutiliza la que ya tienes o crea una nueva)
        skin = createBasicSkin();

        // Crea textura para el overlay de pausa
        Pixmap overlayPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        overlayPixmap.setColor(0, 0, 0, 0.7f);
        overlayPixmap.fill();
        pauseOverlay = new Texture(overlayPixmap);
        overlayPixmap.dispose();

        pauseStage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        createPauseMenu();

        stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        createPauseButton();
        Gdx.input.setInputProcessor(stage);

        font = new BitmapFont(); // Fuente por defecto
        font.getData().setScale(1.5f);

        createPauseMenu();
    }

    @Override
    public void render(float delta) {
        if (!isPaused) {
            handleInput();
            updateGame();
        }
        draw();
        stage.act(delta);
        stage.draw();
        if (isPaused) {
            drawPauseMenu();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        stage.getViewport().update(width, height, true);
        pauseStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose () {
        batch.dispose();
        placeSound.dispose();
        clearLineSound.dispose();
        if (stage != null) stage.dispose();
        if (pauseStage != null) pauseStage.dispose();
        if (pauseOverlay != null) pauseOverlay.dispose();
        if (skin != null) skin.dispose();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }
}
