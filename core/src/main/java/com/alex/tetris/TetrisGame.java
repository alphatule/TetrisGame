package com.alex.tetris;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Arrays;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class TetrisGame extends ApplicationAdapter {

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;

    public static final float VIRTUAL_WIDTH = 480;
    public static final float VIRTUAL_HEIGHT = 800;
    // Game Default Settings
    public static final int BOARD_COLUMNS = 10;
    public static final int BOARD_ROWS = 20;
    public static final int CELL_SIZE = 35; // Tamaño de cada celda en píxeles
    // Otras variables de texturas
    private Texture cellTexture;
    private Texture[] pieceTextures;

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
    private final float dropInterval = 0.3f;

    // Control táctil
    private final Vector3 initialTouchPos = new Vector3();
    private float touchTime = 0;
    private final float MAX_TAP_DISTANCE = 15f;
    private final float TAP_MAX_DURATION = 0.3f;
    private final float MIN_SWIPE_DISTANCE = 40f;

    private boolean rotationPerformed = false;
    private long lastRotationTime = 0;
    private final long ROTATION_COOLDOWN_MS = 250; // 250ms de cooldown

    @Override
    public void create () {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();
        camera.position.set(VIRTUAL_WIDTH / 2, VIRTUAL_HEIGHT / 2, 0);
        camera.update();

        initTextures();
        board = new int[BOARD_ROWS][BOARD_COLUMNS]; // Inicializar tablero

        spawnNewPiece();
    }

    private void initTextures() {
        cellTexture = createCellTexture(CELL_SIZE);
        pieceTextures = new Texture[PIECE_COLORS.length];
        for (int i = 0; i < PIECE_COLORS.length; i++) {
            pieceTextures[i] = createColoredTexture(CELL_SIZE, PIECE_COLORS[i]);
        }
    }

    @Override
    public void render() {
        handleInput();
        updateGame();
        draw();
    }

    private void handleInput() {
        handleTouchInput();
    }

    private void updateGame() {
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
        drawCurrentPiece();
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

            // Detectar deslizamiento (prioridad sobre rotación)
            if (Math.abs(deltaX) > MIN_SWIPE_DISTANCE) {
                movePiece((int)Math.signum(deltaX));
                initialTouchPos.set(currentPos);
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


    // Metodos para crear piezas nuevas y colores

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
//            Gdx.input.vibrate(10); // Feedback táctil opcional
        }
    }

    public void spawnNewPiece() {
        int shapeIndex = MathUtils.random(0, SHAPES.length - 1);
        currentPiece = new TetrisPiece(SHAPES[shapeIndex], PIECE_COLORS[shapeIndex]);
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
            }
            spawnNewPiece();

            // Game over si la nueva pieza colisiona inmediatamente
            if (checkCollision(currentPiece)) {
                Gdx.app.log("Game", "Game Over!");
                // resetGame();
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
        checkCompleteLines();
//        debugBoard();
    }

    private void checkCompleteLines() {
        for (int row = 0; row < BOARD_ROWS; row++) {
            boolean lineComplete = true;
            for (int col = 0; col < BOARD_COLUMNS; col++) {
                if (board[row][col] == 0) {
                    lineComplete = false;
                    break;
                }
            }

            if (lineComplete) {
                // Eliminar la línea y mover todo hacia abajo
                for (int r = row; r < BOARD_ROWS - 1; r++) {
                    System.arraycopy(board[r + 1], 0, board[r], 0, BOARD_COLUMNS);
                }
                // Limpiar la línea superior
                Arrays.fill(board[BOARD_ROWS - 1], 0);

                // Volver a verificar la misma fila (ahora con los bloques caídos)
                row--;

                // Aquí podrías aumentar la puntuación
            }
        }
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

}
