
import java.util.Random;

/**
 * Enemy robot that moves every 4 time units.
 * 50% chance green (random movement), 50% chance red (targeted toward nearest logic symbol).
 * Deals 5 life damage per time unit when adjacent to the player.
 */
public class Robot {

    private static final String LOGIC_SYMBOLS = "ABCDabcd~^v+>=";
    private int moveCount = 0;
    int x, y;
    boolean targeted; // false = green/random, true = red/targeted
    char[][] maze;
    Random rnd = new Random();

    public Robot(char[][] maze, int x, int y) {
        this.maze = maze;
        this.x = x;
        this.y = y;
        this.targeted = rnd.nextBoolean();
        // mark position (may already be 'X' if loaded from maze file)
        maze[x][y] = 'X';
    }

    /** Called every 4 time units from the game loop. */
    public void move() {
        if (targeted) moveTargeted();
        else          moveRandom();

        moveCount++;
        if (moveCount >= 10) {
            moveCount = 0;
            if (rnd.nextBoolean()) targeted = !targeted;  // %50 ihtimalle mod değiştir
        }
    }

    private void moveRandom() {
        int[] dx = { 1, -1, 0,  0};
        int[] dy = { 0,  0, 1, -1};
        for (int tries = 0; tries < 10; tries++) {
            int dir = rnd.nextInt(4);
            int nx = x + dx[dir];
            int ny = y + dy[dir];
            if (canMoveTo(nx, ny)) {
                relocate(nx, ny);
                return;
            }
        }
    }

    private void moveTargeted() {
        int bestX = -1, bestY = -1, bestDist = Integer.MAX_VALUE;
        for (int i = 1; i < Maze.COLS - 1; i++) {
            for (int j = 1; j < Maze.ROWS - 1; j++) {
                if (LOGIC_SYMBOLS.indexOf(maze[i][j]) >= 0) {
                    int dist = Math.abs(i - x) + Math.abs(j - y);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestX = i;
                        bestY = j;
                    }
                }
            }
        }
        if (bestX == -1) { moveRandom(); return; }

        int sx = Integer.signum(bestX - x);
        int sy = Integer.signum(bestY - y);

        if (sx != 0 && canMoveTo(x + sx, y)) {
            relocate(x + sx, y);
        } else if (sy != 0 && canMoveTo(x, y + sy)) {
            relocate(x, y + sy);
        } else {
            moveRandom();
        }
    }

    private boolean canMoveTo(int nx, int ny) {
        if (nx <= 0 || nx >= Maze.COLS - 1) return false;
        if (ny <= 0 || ny >= Maze.ROWS - 1) return false;
        char c = maze[nx][ny];
        return c != '#' && c != 'X' && c != 'P';
    }

    private void relocate(int nx, int ny) {
        maze[x][y] = ' ';
        x = nx;
        y = ny;
        maze[x][y] = 'X';
    }

    /** Returns true if this robot is directly adjacent (Manhattan distance == 1) to the player. */
    public boolean isAdjacentTo(int px, int py) {
        return Math.abs(x - px) + Math.abs(y - py) == 1;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isTargeted() { return targeted; }
}