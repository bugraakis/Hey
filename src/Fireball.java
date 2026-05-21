

import java.util.List;

public class Fireball {

    private int x, y;
    private final int dx, dy;
    private final char[][] maze;
    private boolean active;

    // direction encoding matches Player.facing
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

    public void move(List<Robot> robots) {
        if (!active) return;

        maze[x][y] = ' '; // clear old position
        x += dx;
        y += dy;

        // hit a wall or edge
        if (x <= 0 || x >= Maze.COLS - 1 || y <= 0 || y >= Maze.ROWS - 1 || maze[x][y] == '#') {
            active = false;
            return;
        }

        // punch through any robot at this cell, keep going
        for (int i = robots.size() - 1; i >= 0; i--) {
            Robot r = robots.get(i);
            if (r.getX() == x && r.getY() == y) {
                maze[x][y] = ' ';
                robots.remove(i);
                Game.Score += 50;
            }
        }

        // stops on symbols, player, other non-empty tiles
        if (maze[x][y] != ' ' && maze[x][y] != 'o') {
            active = false;
            return;
        }

        maze[x][y] = 'o';
    }

    public boolean isActive() { return active; }
    public int     getX()     { return x; }
    public int     getY()     { return y; }
}