package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Maze {

    public static final int COLS = 45;
    public static final int ROWS = 21;

    /** Load a 21x45 maze from a text file. Falls back to outer-walls-only on error. */
    public static char[][] loadFromFile(String filename) {
        char[][] maze = new char[COLS][ROWS];
        for (int i = 0; i < COLS; i++)
            for (int j = 0; j < ROWS; j++)
                maze[i][j] = ' ';

        try (Scanner sc = new Scanner(new File(filename))) {
            int row = 0;
            while (sc.hasNextLine() && row < ROWS) {
                String line = sc.nextLine();
                for (int col = 0; col < line.length() && col < COLS; col++) {
                    maze[col][row] = line.charAt(col);
                }
                row++;
            }
        } catch (FileNotFoundException e) {
            buildOuterWalls(maze);
        }
        return maze;
    }

    public static void buildOuterWalls(char[][] maze) {
        for (int i = 0; i < COLS; i++) {
            maze[i][0]      = '#';
            maze[i][ROWS-1] = '#';
        }
        for (int j = 0; j < ROWS; j++) {
            maze[0][j]      = '#';
            maze[COLS-1][j] = '#';
        }
    }

    public static boolean isPassable(char[][] maze, int x, int y) {
        return x > 0 && x < COLS - 1 && y > 0 && y < ROWS - 1 && maze[x][y] != '#';
    }

    public static boolean isFree(char[][] maze, int x, int y) {
        return isPassable(maze, x, y) && maze[x][y] == ' ';
    }
}
