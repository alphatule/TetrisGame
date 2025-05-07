package com.alex.tetris;

import com.badlogic.gdx.graphics.Color;

import java.util.Arrays;

public class TetrisPiece {
    public int[][] shape;
    public int x, y;
    public Color color;

    public TetrisPiece(int[][] shape, Color color) {
        this.shape = shape;
        this.color = color;
        this.x = TetrisGame.BOARD_COLUMNS / 2 - shape[0].length / 2;
        this.y = TetrisGame.BOARD_ROWS - shape.length;
    }

    public TetrisPiece copy() {
        int[][] newShape = new int[this.shape.length][];
        for (int i = 0; i < this.shape.length; i++) {
            newShape[i] = Arrays.copyOf(this.shape[i], this.shape[i].length);
        }
        return new TetrisPiece(newShape, this.color);
    }
}
