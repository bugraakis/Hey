package src;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
import enigma.console.TextAttributes;

public class Menu {

    private static final int COLS          = 80;
    private static final int ROWS          = 30;
    private static final int NUM_ROBOTS    = 8;
    private static final int NUM_ITEMS     = 15;

    // Pre-built faded background colours — created once, reused every frame
    private static final TextAttributes BG_WALL  = new TextAttributes(new Color(48, 48, 48),  Color.BLACK);
    private static final TextAttributes BG_ITEM  = new TextAttributes(new Color(60, 60, 60),  Color.BLACK);
    private static final TextAttributes BG_ROBOT = new TextAttributes(new Color(70, 70, 70),  Color.BLACK);

    private volatile int selection = 0;
    private volatile int cursor    = 0;

    // Background state — generated once, robots persist between menu re-entries
    private final Random rng = new Random(7);
    private char[][] bgMaze;
    private int[][]  bgRobots;   // [i][0]=col  [i][1]=row
    private char[]   bgSymbols;
    private int[][]  bgItemPos;  // [i][0]=col  [i][1]=row
    private int      animFrame = 0;

    // ── Pixel font (5 rows tall, same size for both words) ─────────────────────
    // TREE  : T(6) gap(2) R(6) gap(2) E(6) gap(2) E(6)  = 30 wide
    private static final String[] TREE_ROWS = {
        "######  #####   ######  ######",
        "  ##    ##  ##  ##      ##    ",
        "  ##    #####   ####    ####  ",
        "  ##    ## ##   ##      ##    ",
        "  ##    ##  ##  ######  ######"
    };
    // TABLE : T(6) gap(2) A(6) gap(2) B(6) gap(2) L(6) gap(2) E(6) = 38 wide
    private static final String[] TABLE_ROWS = {
        "######   ####   #####   ##      ######",
        "  ##    ##  ##  ##  ##  ##      ##    ",
        "  ##    ######  #####   ##      ####  ",
        "  ##    ##  ##  ##  ##  ##      ##    ",
        "  ##    ##  ##  #####   ######  ######"
    };

    private static final String[] OPTIONS  = {
        "S T A R T   G A M E",
        "H I G H   S C O R E S",
        "H E L P",
        "E X I T"
    };
    private static final int[] OPT_ROWS = {14, 17, 20, 23};

    // ── Public entry point ─────────────────────────────────────────────────────

    public int show(enigma.console.Console cn, HighScoreList scores) {
        cursor    = 0;
        selection = 0;
        animFrame = 0;

        if (bgMaze == null) initBackground();

        final boolean[] running = {true};
        Thread anim = new Thread(() -> {
            while (running[0]) {
                animFrame++;
                if (animFrame % 2 == 0) moveRobots(); // robots move every other frame
                drawFrame(cn);
                try { Thread.sleep(180); } catch (InterruptedException e) { break; }
            }
        });
        anim.setDaemon(true);

        drawFrame(cn); // paint immediately so there is no blank-screen flash
        anim.start();

        KeyListener kl = new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_1) { selection = 1; return; }
                if (k == KeyEvent.VK_2) { selection = 2; return; }
                if (k == KeyEvent.VK_3) { selection = 3; return; }
                if (k == KeyEvent.VK_4) { selection = 4; return; }
                if (k == KeyEvent.VK_UP)    cursor = (cursor + 3) % 4;
                if (k == KeyEvent.VK_DOWN)  cursor = (cursor + 1) % 4;
                if (k == KeyEvent.VK_ENTER) selection = cursor + 1;
            }
        };

        cn.getTextWindow().addKeyListener(kl);
        while (selection == 0) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        running[0] = false;
        cn.getTextWindow().removeKeyListener(kl);
        try { anim.join(400); } catch (InterruptedException ignored) {}

        if (selection == 2) { showHighScores(cn, scores); selection = 0; return show(cn, scores); }
        if (selection == 3) { showHelp(cn);                selection = 0; return show(cn, scores); }
        return selection; // 1 = start, 4 = exit
    }

    // ── Maze generation (iterative DFS) ───────────────────────────────────────

    private void initBackground() {
        bgMaze    = generateMaze();
        bgRobots  = new int[NUM_ROBOTS][2];
        bgSymbols = new char[NUM_ITEMS];
        bgItemPos = new int[NUM_ITEMS][2];

        for (int i = 0; i < NUM_ROBOTS; i++) {
            int[] p = randomPassable();
            bgRobots[i][0] = p[0]; bgRobots[i][1] = p[1];
        }
        char[] syms = {'A','B','C','D','~','^','v','+','>','='};
        for (int i = 0; i < NUM_ITEMS; i++) {
            bgSymbols[i] = syms[rng.nextInt(syms.length)];
            int[] p = randomPassable();
            bgItemPos[i][0] = p[0]; bgItemPos[i][1] = p[1];
        }
    }

    private char[][] generateMaze() {
        int CW = (COLS - 1) / 2; // number of cells wide
        int CH = (ROWS - 1) / 2; // number of cells tall
        char[][] grid = new char[COLS][ROWS];
        for (int c = 0; c < COLS; c++)
            for (int r = 0; r < ROWS; r++)
                grid[c][r] = '#';

        carve(grid, new boolean[CW][CH], CW, CH, 0, 0);

        // Clear the outer border so the maze appears to extend beyond the screen
        for (int c = 0; c < COLS; c++) { grid[c][0] = ' '; grid[c][ROWS - 1] = ' '; }
        for (int r = 0; r < ROWS; r++) { grid[0][r] = ' '; grid[COLS - 1][r]  = ' '; }
        return grid;
    }

    // Recursive DFS: visit cell (cx,cy), open it, then try all 4 directions in random order
    private void carve(char[][] grid, boolean[][] vis, int CW, int CH, int cx, int cy) {
        vis[cx][cy] = true;
        grid[cx * 2 + 1][cy * 2 + 1] = ' ';

        int[] dC = {0, 0, 1, -1};
        int[] dR = {1, -1, 0, 0};
        int[] dirs = {0, 1, 2, 3};
        for (int i = 3; i > 0; i--) {          // Fisher-Yates shuffle
            int j = rng.nextInt(i + 1);
            int tmp = dirs[i]; dirs[i] = dirs[j]; dirs[j] = tmp;
        }
        for (int d : dirs) {
            int nx = cx + dC[d], ny = cy + dR[d];
            if (nx >= 0 && nx < CW && ny >= 0 && ny < CH && !vis[nx][ny]) {
                grid[cx * 2 + 1 + dC[d]][cy * 2 + 1 + dR[d]] = ' '; // knock down wall
                carve(grid, vis, CW, CH, nx, ny);
            }
        }
    }

    private int[] randomPassable() {
        int c, r;
        do { c = rng.nextInt(COLS); r = rng.nextInt(ROWS); }
        while (bgMaze[c][r] != ' ');
        return new int[]{c, r};
    }

    private void moveRobots() {
        int[] dC = {1, -1, 0, 0};
        int[] dR = {0, 0, 1, -1};
        for (int[] robot : bgRobots) {
            int dir = rng.nextInt(4);
            int nc  = robot[0] + dC[dir];
            int nr  = robot[1] + dR[dir];
            if (nc >= 0 && nc < COLS && nr >= 0 && nr < ROWS && bgMaze[nc][nr] == ' ') {
                robot[0] = nc; robot[1] = nr;
            }
        }
    }

    // ── Frame rendering ────────────────────────────────────────────────────────

    private void drawFrame(enigma.console.Console cn) {
        // 1. Background: maze
        for (int c = 0; c < COLS; c++)
            for (int r = 0; r < ROWS; r++)
                if (bgMaze[c][r] == '#')
                    cn.getTextWindow().output(c, r, '#', BG_WALL);
                else
                    cn.getTextWindow().output(c, r, ' ');

        // 2. Background: faded items
        for (int i = 0; i < NUM_ITEMS; i++)
            cn.getTextWindow().output(bgItemPos[i][0], bgItemPos[i][1], bgSymbols[i], BG_ITEM);

        // 3. Background: faded robots
        for (int[] robot : bgRobots)
            cn.getTextWindow().output(robot[0], robot[1], 'X', BG_ROBOT);

        // 4. Foreground: menu overlay on top of everything
        drawPixelTitle(cn, TREE_ROWS,  new Color(80, 100, 255), 1);
        drawPixelTitle(cn, TABLE_ROWS, new Color(210, 80, 10),  7);
        drawMenuItems(cn);
        drawFooter(cn);
    }

    private void drawPixelTitle(enigma.console.Console cn, String[] rows, Color color, int startRow) {
        TextAttributes ta       = new TextAttributes(color, Color.BLACK);
        int            startCol = (COLS - rows[0].length()) / 2;
        for (int r = 0; r < rows.length; r++) {
            String line = rows[r];
            for (int c = 0; c < line.length(); c++)
                if (line.charAt(c) != ' ')
                    cn.getTextWindow().output(startCol + c, startRow + r, line.charAt(c), ta);
        }
    }

    private void drawMenuItems(enigma.console.Console cn) {
        TextAttributes sel   = new TextAttributes(Color.WHITE,            Color.BLACK);
        TextAttributes unsel = new TextAttributes(new Color(80, 80, 160), Color.BLACK);
        TextAttributes arr   = new TextAttributes(Color.WHITE,            Color.BLACK);

        for (int i = 0; i < OPTIONS.length; i++) {
            String  opt      = OPTIONS[i];
            int     row      = OPT_ROWS[i];
            int     col      = (COLS - opt.length()) / 2;
            boolean selected = (i == cursor);

            if (selected) {
                cn.getTextWindow().output(col - 3, row, '>', arr);
                cn.getTextWindow().output(col - 2, row, '>', arr);
                cn.getTextWindow().output(col + opt.length() + 1, row, '<', arr);
                cn.getTextWindow().output(col + opt.length() + 2, row, '<', arr);
            }
            TextAttributes color = selected ? sel : unsel;
            for (int c = 0; c < opt.length(); c++)
                cn.getTextWindow().output(col + c, row, opt.charAt(c), color);
        }
    }

    private void drawFooter(enigma.console.Console cn) {
        TextAttributes gray = new TextAttributes(Color.GRAY, Color.BLACK);
        putCentered(cn, 27, "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", gray);
        putCentered(cn, 28, "UP / DOWN  navigate      ENTER  select      1 / 2 / 3 / 4  direct",    gray);
    }

    // ── High Scores screen ─────────────────────────────────────────────────────

    private void showHighScores(enigma.console.Console cn, HighScoreList scores) {
        clearScreen(cn);
        TextAttributes titleCol = new TextAttributes(new Color(80, 100, 255), Color.BLACK);
        TextAttributes textCol  = new TextAttributes(Color.WHITE,              Color.BLACK);
        TextAttributes grayCol  = new TextAttributes(Color.GRAY,               Color.BLACK);

        putCentered(cn, 3, "H I G H   S C O R E S", titleCol);

        if (scores != null && scores.getSize() > 0)
            scores.display(cn, 20, 6);
        else
            putCentered(cn, 8, "( no scores yet )", textCol);

        putCentered(cn, 28, "Press any key to return...", grayCol);
        waitForKey(cn);
    }

    // ── Help screen ────────────────────────────────────────────────────────────

    private void showHelp(enigma.console.Console cn) {
        clearScreen(cn);
        TextAttributes titleCol   = new TextAttributes(new Color(80, 100, 255), Color.BLACK);
        TextAttributes sectionCol = new TextAttributes(new Color(210, 150,  0), Color.BLACK);
        TextAttributes textCol    = new TextAttributes(Color.WHITE,              Color.BLACK);
        TextAttributes dimCol     = new TextAttributes(Color.LIGHT_GRAY,         Color.BLACK);
        TextAttributes grayCol    = new TextAttributes(Color.GRAY,               Color.BLACK);

        putCentered(cn, 1, "G A M E   F E A T U R E S   &   M E C H A N I C S", titleCol);
        putCentered(cn, 2, "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", grayCol);

        put(cn, 2,  4, "SCREEN 1  -  MAZE", sectionCol);
        put(cn, 2,  5, "  Arrow Keys : Move player    SPACE : Fire projectile    M : Toggle storage", textCol);
        put(cn, 2,  6, "  Collect logic symbols (A-D, ~, ^, v, +, >) scattered across the maze.", dimCol);
        put(cn, 2,  7, "  Robots deal 5 HP damage per tick when adjacent. Shoot them for +50 pts.", dimCol);

        put(cn, 2,  9, "SCREEN 2  -  EXPRESSION TREE", sectionCol);
        put(cn, 2, 10, "  W / A / D : Navigate nodes    T : Place symbol    R : Remove    F : Finalize", textCol);
        put(cn, 2, 11, "  Tree requires >= 3 distinct variables and a depth of >= 3 to finalize.", dimCol);
        put(cn, 2, 12, "  Finalizing awards: (number of filled nodes) x 10 points.", dimCol);

        put(cn, 2, 14, "SCREEN 3  -  TRUTH TABLE & KARNAUGH MAP", sectionCol);
        put(cn, 2, 15, "  One cell per column is hidden — type the correct 0 or 1 to fill it in.", textCol);
        put(cn, 2, 16, "  Correct cell: +3 pts   |   Wrong cell: -2 pts", dimCol);
        put(cn, 2, 17, "  K-Map solved via Quine-McCluskey. Match the SOP = bonus equal to tree score.", dimCol);

        put(cn, 2, 19, "SCORING SUMMARY", sectionCol);
        put(cn, 2, 20, "  Shoot robot    +50 pts       Tree finalize    nodes x 10 pts", textCol);
        put(cn, 2, 21, "  K-Map match    +tree pts     Tree navigate    -1 pt / move", textCol);
        put(cn, 2, 22, "  Remove symbol  -2 pts        Wrong truth cell  -2 pts", dimCol);

        putCentered(cn, 27, "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", grayCol);
        putCentered(cn, 28, "Press any key to return to menu...", grayCol);
        waitForKey(cn);
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private void clearScreen(enigma.console.Console cn) {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                cn.getTextWindow().output(c, r, ' ');
    }

    private void put(enigma.console.Console cn, int col, int row, String text, TextAttributes ta) {
        for (int i = 0; i < text.length() && col + i < COLS; i++)
            cn.getTextWindow().output(col + i, row, text.charAt(i), ta);
    }

    private void putCentered(enigma.console.Console cn, int row, String text, TextAttributes ta) {
        put(cn, Math.max(0, (COLS - text.length()) / 2), row, text, ta);
    }

    private void waitForKey(enigma.console.Console cn) {
        final boolean[] pressed = {false};
        KeyListener kl = new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
            public void keyPressed(KeyEvent e) { pressed[0] = true; }
        };
        cn.getTextWindow().addKeyListener(kl);
        while (!pressed[0]) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        cn.getTextWindow().removeKeyListener(kl);
    }
}
