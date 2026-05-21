package src;

import java.util.List;

/**
 * Active fireball ('o'). Moves 1 cell per time unit.
 * Destroys any robot it hits and continues travelling.
 * Stops when it hits a wall or non-robot, non-space obstacle.
 */
public class Fireball {

    private int x, y;
    private final int dx, dy;
    private final char[][] maze;
    private boolean active;

    /**
     * @param direction 1=right, -1=left, 2=up, -2=down
     */
    public Fireball(char[][] maze, int startX, int startY, int direction) {
        this.maze = maze;
        this.x = startX;
        this.y = startY;
        this.active = true;
        switch (direction) {
            case  1: dx =  1; dy =  0; break;
            case -1: dx = -1; dy =  0; break;
            case  2: dx =  0; dy = -1; break;
            case -2: dx =  0; dy =  1; break;
            default: dx =  1; dy =  0; break;
        }
        maze[x][y] = 'o';
    }

    /**
     * Advance the fireball one cell.
     * Removes any robot at the new cell and awards 50 pts; fireball keeps going.
     * Deactivates on wall or other obstacle.
     * @param robots mutable robot list managed by Main
     */
    public void move(List<Robot> robots) {
        if (!active) return;

        maze[x][y] = ' ';
        x += dx;
        y += dy;

        // Out-of-bounds or wall → stop
        if (x <= 0 || x >= Maze.COLS - 1 || y <= 0 || y >= Maze.ROWS - 1
                || maze[x][y] == '#') {
            active = false;
            return;
        }

        // Robot hit → destroy, continue
        for (int i = robots.size() - 1; i >= 0; i--) {
            Robot r = robots.get(i);
            if (r.getX() == x && r.getY() == y) {
                maze[x][y] = ' ';
                robots.remove(i);
                Main.Score += 50;
            }
        }

        // If the cell is now something other than empty or another fireball, stop
        if (maze[x][y] != ' ' && maze[x][y] != 'o') {
            active = false;
            return;
        }

        maze[x][y] = 'o';
    }

    public boolean isActive() { return active; }
    public int getX()         { return x; }
    public int getY()         { return y; }
}
