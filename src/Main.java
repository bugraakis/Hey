package src;
import enigma.core.Enigma;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import enigma.console.TextAttributes;

public class Main {

    public static enigma.console.Console cn = Enigma.getConsole("Tree & Table", 80, 30, 20, 30);


    public static final int Columns = 45;
    public static final int Rows    = 21;

    public static char[][]       maze;
    public static CircularQueue  input_q    = new CircularQueue(10);
    public static Stack backpack   = new Stack(8);
    public static Player         play;
    public static ExpressionTree exprTree   = new ExpressionTree();
    public static HighScoreList  highScores;

    public static List<Robot>    robots    = new ArrayList<>();
    public static List<Fireball> fireballs = new ArrayList<>();

    public static int  time_unit        = 0;
    public static int  display_time     = 0;
    public static int  Score            = 0;
    public static int  Fireball         = 0;
    public static int  finalizedTreeScore = 0;
    public static int  Life             = 100;
    public static boolean death         = false;
    public static boolean move          = false;

    // buffered player actions — set on EDT, consumed on main thread to avoid race conditions
    public static volatile int     pendingMove   = 0;
    public static volatile boolean pendingFire   = false;
    public static volatile boolean pendingToggle = false;

    // green = wandering, red = chasing player
    public static TextAttributes randommColor   = new TextAttributes(Color.GREEN, Color.BLACK);
    public static TextAttributes targetedmColor = new TextAttributes(Color.RED,   Color.BLACK);
    // orange fireball
    public static TextAttributes fireballColor  = new TextAttributes(Color.ORANGE, Color.BLACK);

    static final TextAttributes VAR_COLOR  = new TextAttributes(Color.CYAN,                Color.BLACK);
    static final TextAttributes NVAR_COLOR = new TextAttributes(new Color(0, 190, 190),    Color.BLACK);
    static final TextAttributes OP_COLOR   = new TextAttributes(Color.YELLOW,              Color.BLACK);
    static final TextAttributes WALL_COLOR = new TextAttributes(new Color(70, 90, 130),    Color.BLACK);
    static final TextAttributes PLYR_COLOR = new TextAttributes(new Color(255, 220, 100),  Color.BLACK);
    static final TextAttributes HUD_BORDER = new TextAttributes(new Color(80, 100, 200),   Color.BLACK);
    static final TextAttributes HUD_TITLE  = new TextAttributes(new Color(255, 210, 60),   Color.BLACK);
    static final TextAttributes HUD_SECT   = new TextAttributes(new Color(160, 160, 255),  Color.BLACK);
    static final TextAttributes HUD_LABEL  = new TextAttributes(Color.LIGHT_GRAY,          Color.BLACK);
    static final TextAttributes HUD_VALUE  = new TextAttributes(Color.WHITE,               Color.BLACK);
    static final TextAttributes HUD_HINT   = new TextAttributes(Color.GRAY,                Color.BLACK);
    static final TextAttributes HUD_SLOT   = new TextAttributes(new Color(130, 130, 130),  Color.BLACK);
    static final TextAttributes HUD_EMPTY  = new TextAttributes(new Color(55, 55, 55),     Color.BLACK);

    public static int     currentScreen   = 1;
    public static boolean treeFinalized   = false;
    public static boolean truthTableShown = false;
    public static String  finalizedInfix  = null;
    public static boolean treeInvalidMsg  = false;

    public static volatile int rkey = 0;

    // pixel positions for the 15 tree nodes on screen 2
    public static final int[] treeX = {0,
            22,
            12, 32,
            6, 18, 26, 38,
            3, 9, 15, 21, 23, 29, 35, 41
    };
    public static final int[] treeY = {0,
            2,
            4, 4,
            6, 6, 6, 6,
            8, 8, 8, 8, 8, 8, 8, 8
    };

    public static void main(String[] args) throws InterruptedException {

        highScores = HighScoreList.loadFromFile("highscore.txt");

        Menu menu = new Menu();
        int choice = menu.show(cn, highScores);
        if (choice == 4) System.exit(0);

        maze = Maze.loadFromFile("maze.txt");
        scanRobotsFromMaze();

        Input_System in = new Input_System(maze, input_q);

        play = new Player(maze, backpack);
        Main.Life = play.getLife();

        for (int i = 0; i < 10; i++) {
            in.Generator();
            in.Writer();
        }

        play.Starting_Place();

        Maze_Drawer(maze);
        drawHUD();

        registerKeyListener();

        while (!death) {
            long start = System.currentTimeMillis();
            time_unit++;

            if (currentScreen == 1) {
                while (!input_q.isFull()) in.Generator(); // keep queue topped up

                // apply buffered player actions here on the main thread — safe from race conditions
                if (pendingMove != 0) {
                    play.Movement(pendingMove);
                    pendingMove = 0;
                }
                if (pendingFire) {
                    firePlayerBall();
                    pendingFire = false;
                }
                if (pendingToggle) {
                    play.toggleStorage();
                    pendingToggle = false;
                    refreshStats();
                }

                moveFireballs();
                applyNeighbourDamage();

                if (time_unit % 4 == 0) moveRobots();   // robots move slower than player
                if (time_unit % 20 == 0) in.Writer();   // new item on the map every 2 seconds

                Maze_Drawer(maze);
                input_list();
                Backpack_Writer();
            } else if (currentScreen == 3 && !truthTableShown && finalizedInfix != null) {
                // TruthTable blocks on Scanner — must run on main thread, not EDT
                truthTableShown = true;
                // give the player 2 seconds to read the finalize summary in the right panel
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                clearFullScreen(); // wipe everything — right panel must not bleed into the table
                cn.getTextWindow().setCursorPosition(0, 0);
                TruthTable(finalizedInfix);
            }

            // 1 display-second = 10 time units = 1000 ms
            if (time_unit % 10 == 0) {
                display_time++;
                if (currentScreen != 3) {
                    refreshStats();
                }
            }

            // target 100 ms per tick
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed < 100) {
                try { Thread.sleep(100 - elapsed); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }

        handleDeath();
    }

    private static void scanRobotsFromMaze() {
        for (int x = 1; x < Columns - 1; x++)
            for (int y = 1; y < Rows - 1; y++)
                if (maze[x][y] == 'X') robots.add(new Robot(maze, x, y));
    }

    private static void registerKeyListener() {
        cn.getTextWindow().addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) { rkey = 0; }
            public void keyPressed(KeyEvent e) {
                if (death) return;
                handleKey(e.getKeyCode());
            }
        });
    }

    public static void handleKey(int k) {
        if (k == KeyEvent.VK_1) {
            if (currentScreen == 3) return;
            currentScreen = 1;
            clearMazeArea();
            Maze_Drawer(maze);
            drawHUD(); // full right-panel redraw clears any tree/finalize leftovers
            return;
        }
        if (k == KeyEvent.VK_2) {
            if (currentScreen == 3) return;
            currentScreen = 2;
            clearMazeArea();
            drawHUD(); // full right-panel redraw before tree draws on top
            treeDrawer();
            return;
        }
        if (k == KeyEvent.VK_3) { if (treeFinalized) currentScreen = 3; return; }

        if (currentScreen == 1) {
            switch (k) {
                case KeyEvent.VK_UP:    pendingMove = 2;     break;
                case KeyEvent.VK_DOWN:  pendingMove = -2;    break;
                case KeyEvent.VK_LEFT:  pendingMove = -1;    break;
                case KeyEvent.VK_RIGHT: pendingMove = 1;     break;
                case KeyEvent.VK_SPACE: pendingFire = true;  break;
                case KeyEvent.VK_M:     pendingToggle = true; break;
            }
        } else if (currentScreen == 2) {
            handleTreeKey(k);
            if (currentScreen == 2) treeDrawer();
        }
        if (currentScreen != 3) refreshStats();
    }

    private static void firePlayerBall() {
        Fireball fb = play.fire();
        if (fb != null) fireballs.add(fb);
    }

    private static void handleTreeKey(int k) {
        switch (k) {
            case KeyEvent.VK_W:
                exprTree.moveCursorUp();
                break;
            case KeyEvent.VK_A:
                exprTree.moveCursorLeft();
                break;
            case KeyEvent.VK_D:
                exprTree.moveCursorRight();
                break;
            case KeyEvent.VK_T:
                if (!backpack.isEmpty()) {
                    char sym = (char) backpack.pop();
                    exprTree.placeSymbol(sym);
                }
                break;
            case KeyEvent.VK_R:
                char removed = exprTree.removeSymbol();
                if (removed != 0 && !backpack.isFull()) backpack.push(removed);
                break;
            case KeyEvent.VK_F:
                handleFinalize();
                break;
        }
    }

    private static void handleFinalize() {
        ExpressionTree.FinalizeResult result = exprTree.finalizeTree();

        if (result == null) {
            put(2, 16, "Tree invalid! Need >=3 variables and depth >=3. -10 pts");
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            put(2, 16, "                                                        ");
            refreshStats();
        } else {
            treeFinalized = true;
            currentScreen = 3;

            new Thread(() -> {
                clearMazeArea();
                put(5,  7, "Infix  : " + result.infix);
                put(5,  8, "Postfix: " + result.postfix);
                put(5, 10, "Tree Score: +" + result.treeScore + " pts");

                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                finalizedTreeScore = result.treeScore;
                clearMazeArea();
                cn.getTextWindow().setCursorPosition(0, 0);
                TruthTable(result.infix);

                currentScreen = 3;
                showKmap(result.infix);
                refreshStats();
            }).start();
        }
    }

    private static void moveFireballs() {
        for (int i = fireballs.size() - 1; i >= 0; i--) {
            Fireball fb = fireballs.get(i);
            fb.move(robots);
            if (!fb.isActive()) fireballs.remove(i);
        }
    }

    private static void applyNeighbourDamage() {
        // 5 damage per tick per adjacent robot — stacks if cornered
        for (Robot r : robots) {
            if (r.isAdjacentTo(play.getX(), play.getY())) {
                play.takeDamage(5);
            }
        }
    }

    private static void moveRobots() {
        for (Robot r : robots) r.move();
    }

    private static void handleDeath() {
        clearMazeArea();
        int col = Columns / 2 - 5, row = Rows / 2;
        put(col, row - 1, "  GAME OVER  ");
        put(col, row,     " Score: " + Score + " ");
        put(col, row + 1, " Press any key ");

        final boolean[] pressed = {false};
        cn.getTextWindow().addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
            public void keyPressed(KeyEvent e) { pressed[0] = true; }
        });
        while (!pressed[0]) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }

        cn.getTextWindow().setCursorPosition(0, Rows - 1);
        System.out.print("Enter your name: ");
        Scanner sc = new Scanner(System.in);
        if (sc.hasNextLine()) {
            String name = sc.nextLine().trim();
            if (!name.isEmpty()) {
                highScores.insert(name, Score);
                highScores.saveToFile("highscore.txt");
            }
        }
        System.exit(0);
    }

    public static void Maze_Drawer(char[][] maze) {
        for (int x = 0; x < Columns; x++) {
            for (int y = 0; y < Rows; y++) {
                char cell = maze[x][y];
                if (cell == 'X') {
                    boolean drawn = false;
                    for (Robot r : robots) {
                        if (r.x == x && r.y == y) {
                            cn.getTextWindow().output(x, y, 'X',
                                    r.isTargeted() ? targetedmColor : randommColor);
                            drawn = true;
                            break;
                        }
                    }
                    // robot was destroyed but maze still had stale X — clear it
                    if (!drawn) {
                        maze[x][y] = ' ';
                        cn.getTextWindow().output(x, y, ' ');
                    }
                } else if (cell == 'o') {
                    cn.getTextWindow().output(x, y, 'o', fireballColor);
                } else if (cell == '@') {
                    cn.getTextWindow().output(x, y, '@', fireballColor);
                } else if (cell == '#') {
                    cn.getTextWindow().output(x, y, '#', WALL_COLOR);
                } else if (cell == 'P') {
                    cn.getTextWindow().output(x, y, 'P', PLYR_COLOR);
                } else if ("ABCD".indexOf(cell) >= 0) {
                    cn.getTextWindow().output(x, y, cell, VAR_COLOR);
                } else if ("abcd".indexOf(cell) >= 0) {
                    cn.getTextWindow().output(x, y, cell, NVAR_COLOR);
                } else if ("^v+>=~".indexOf(cell) >= 0) {
                    cn.getTextWindow().output(x, y, cell, OP_COLOR);
                } else {
                    cn.getTextWindow().output(x, y, cell);
                }
            }
        }
    }


    /** Full right-panel redraw — call on startup and every screen switch. */
    public static void drawHUD() {
        // Wipe all 30 rows of the right panel so no menu or old screen content bleeds in
        for (int x = 45; x < 80; x++)
            for (int y = 0; y < 30; y++)
                cn.getTextWindow().output(x, y, ' ');

        // Box: rows 0-16
        hudTopRow(0);
        hudSideRow(1);
        putC(51, 1, "T R E E   &   T A B L E", HUD_TITLE);
        hudMidRow(2);
        hudSideRow(3);
        putC(47, 3, "INPUT QUEUE", HUD_SECT);
        hudSideRow(4);
        hudMidRow(5);
        for (int r = 6; r <= 11; r++) hudSideRow(r);
        hudMidRow(12);
        hudSideRow(13);
        putC(47, 13, "1=Maze  2=Tree  3=K-Map", HUD_HINT);
        hudSideRow(14);
        putC(47, 14, "M=Toggle    SPACE=Fire", HUD_HINT);
        hudSideRow(15);
        hudBotRow(16);

        input_list();
        refreshStats();
        Backpack_Writer();
    }

    public static void refreshStats() {
        putC(47,  6, "Time    : ", HUD_LABEL);
        putC(57,  6, pad(String.valueOf(display_time), 21), HUD_VALUE);
        putC(47,  7, "Score   : ", HUD_LABEL);
        putC(57,  7, pad(String.valueOf(Score),        21), HUD_VALUE);
        putC(47,  8, "Balls   : ", HUD_LABEL);
        putC(57,  8, pad(String.valueOf(Fireball),     21), HUD_VALUE);
        putC(47,  9, "Life    : ", HUD_LABEL);
        putC(57,  9, pad(String.valueOf(Life),         21), HUD_VALUE);

        int barLen = 20;
        int filled = Math.max(0, (int)((float) Life / 100f * barLen));
        Color barColor = Life > 50 ? Color.GREEN : (Life > 25 ? Color.YELLOW : Color.RED);
        for (int i = 0; i < barLen; i++) {
            char ch = i < filled ? '|' : '-';
            TextAttributes ta = i < filled
                ? new TextAttributes(barColor, Color.BLACK)
                : new TextAttributes(Color.DARK_GRAY, Color.BLACK);
            cn.getTextWindow().output(47 + i, 10, ch, ta);
        }
        for (int c = 47 + barLen; c <= 78; c++) cn.getTextWindow().output(c, 10, ' ');

        String mode = (play != null && play.isStorageTree()) ? "Tree    " : "Backpack";
        putC(47, 11, "Mode    : ", HUD_LABEL);
        putC(57, 11, pad(mode, 21), HUD_VALUE);
    }

    /** Writes text character-by-character at absolute (col, row) — no shared cursor, thread-safe. */
    public static void put(int col, int row, String text) {
        for (int i = 0; i < text.length() && col + i < 80; i++)
            cn.getTextWindow().output(col + i, row, text.charAt(i));
    }

    /** Like put() but applies a TextAttributes color to every character. */
    public static void putC(int col, int row, String text, TextAttributes ta) {
        for (int i = 0; i < text.length() && col + i < 80; i++)
            cn.getTextWindow().output(col + i, row, text.charAt(i), ta);
    }

    /** Right-pads s with spaces to length n, or truncates if longer. */
    private static String pad(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    public static void input_list() {
        for (int i = 0; i < 10; i++) cn.getTextWindow().output(47 + i, 4, ' ');
        CircularQueue temp = new CircularQueue(10);
        int k = input_q.Size();
        for (int i = 0; i < k; i++) {
            char sym = (char) input_q.Peek();
            cn.getTextWindow().output(47 + i, 4, sym, itemColor(sym));
            temp.Enqueue(input_q.Dequeue());
        }
        int kl = temp.Size();
        for (int i = 0; i < kl; i++) input_q.Enqueue(temp.Dequeue());
    }

    public static void Backpack_Writer() {
        // Label centered above the stack (col 58 = center of right panel minus half of "BACKPACK")
        putC(58, 17, "BACKPACK", HUD_SECT);
        Stack temp = new Stack(8);
        // Draw 8 slots top→bottom: row 18 = top of stack (most recent), row 25 = bottom (first collected)
        for (int i = 0; i < 8; i++) {
            int row = 18 + i;
            if (!backpack.isEmpty()) {
                char sym = (char) backpack.pop();
                temp.push(sym);
                cn.getTextWindow().output(60, row, '|', HUD_SLOT);
                cn.getTextWindow().output(61, row, ' ', HUD_SLOT);
                cn.getTextWindow().output(62, row, sym,  itemColor(sym));
                cn.getTextWindow().output(63, row, ' ', HUD_SLOT);
                cn.getTextWindow().output(64, row, '|', HUD_SLOT);
            } else {
                cn.getTextWindow().output(60, row, '|',  HUD_SLOT);
                cn.getTextWindow().output(61, row, ' ',  HUD_EMPTY);
                cn.getTextWindow().output(62, row, ' ',  HUD_EMPTY);
                cn.getTextWindow().output(63, row, ' ',  HUD_EMPTY);
                cn.getTextWindow().output(64, row, '|',  HUD_SLOT);
            }
        }
        // Closed bottom
        put(60, 26, "+---+");
        // Restore backpack (double-reversal preserves original order)
        int k = temp.size();
        for (int i = 0; i < k; i++) backpack.push(temp.pop());
    }

    private static TextAttributes itemColor(char c) {
        if ("ABCD".indexOf(c) >= 0) return VAR_COLOR;
        if ("abcd".indexOf(c) >= 0) return NVAR_COLOR;
        if ("^v+>=~".indexOf(c) >= 0) return OP_COLOR;
        return HUD_VALUE;
    }

    private static void hudTopRow(int row) {
        cn.getTextWindow().output(45, row, '+', HUD_BORDER);
        for (int c = 46; c <= 78; c++) cn.getTextWindow().output(c, row, '-', HUD_BORDER);
        cn.getTextWindow().output(79, row, '+', HUD_BORDER);
    }
    private static void hudMidRow(int row) {
        cn.getTextWindow().output(45, row, '+', HUD_BORDER);
        for (int c = 46; c <= 78; c++) cn.getTextWindow().output(c, row, '-', HUD_BORDER);
        cn.getTextWindow().output(79, row, '+', HUD_BORDER);
    }
    private static void hudBotRow(int row) {
        cn.getTextWindow().output(45, row, '+', HUD_BORDER);
        for (int c = 46; c <= 78; c++) cn.getTextWindow().output(c, row, '-', HUD_BORDER);
        cn.getTextWindow().output(79, row, '+', HUD_BORDER);
    }
    private static void hudSideRow(int row) {
        cn.getTextWindow().output(45, row, '|', HUD_BORDER);
        cn.getTextWindow().output(79, row, '|', HUD_BORDER);
    }

    private static void clearMazeArea() {
        for (int x = 0; x < Columns; x++)
            for (int y = 0; y < Rows; y++)
                cn.getTextWindow().output(x, y, ' ');
    }

    // wipes the full console window — used before truth table so the right panel doesn't bleed in
    private static void clearFullScreen() {
        for (int x = 0; x < 80; x++)
            for (int y = 0; y < 30; y++)
                cn.getTextWindow().output(x, y, ' ');
    }

    public static int evaluatePostfix(String postfix, int[] values) {
        Stack stackValues = new Stack(50);
        String[] symbols  = postfix.replace(" ", "").split("");

        for (int i = 0; i < symbols.length; i++) {
            String s = symbols[i];
            switch (s) {
                case "A": stackValues.push(values[0]); continue;
                case "a": stackValues.push(1 - values[0]); continue;
                case "B": stackValues.push(values[1]); continue;
                case "b": stackValues.push(1 - values[1]); continue;
                case "C": stackValues.push(values[2]); continue;
                case "c": stackValues.push(1 - values[2]); continue;
                case "D": stackValues.push(values[3]); continue;
                case "d": stackValues.push(1 - values[3]); continue;
            }
            if (s.equals("~")) {
                int v = (int) stackValues.pop();
                stackValues.push(1 - v); // NOT
            } else if(s.matches("[\\^v+>=]")) {
                int b = (int) stackValues.pop();
                int a = (int) stackValues.pop();
                switch (s) {
                    case "^": stackValues.push(a & b);           break; // AND
                    case "v": stackValues.push(a | b);           break; // OR
                    case "+": stackValues.push(a ^ b);           break; // XOR
                    case ">": stackValues.push((1 - a) | b);     break; // implication
                    case "=": stackValues.push(a == b ? 1 : 0);  break; // XNOR
                }
            }
        }
        return (int) stackValues.pop();
    }

    public static boolean isAlreadyAdded(String[] parts, String newPart) {
        for (String s : parts) if (s != null && s.equals(newPart)) return true;
        return false;
    }

    // pulls out each parenthesised sub-expression to use as truth table columns
    public static String[] splitInfix(String infix) {
        Stack temp = new Stack(100);
        String[] parts = new String[10];
        int count = 0;
        String[] symbols = infix.replace(" ", "").split("");

        for (int i = 0; i < symbols.length; i++) {
            String s = symbols[i];
            if (!s.equals(")")) {
                temp.push(s);
            } else {
                String part = "";
                while (!temp.isEmpty() && !temp.peek().equals("("))
                    part = temp.pop() + part;
                if (!isAlreadyAdded(parts, part)) parts[count++] = "(" + part + ")";
                if (!temp.isEmpty()) temp.pop(); // remove "("
                if (!temp.isEmpty() && temp.peek().equals("~")) {
                    temp.pop();
                    part = "~(" + part + ")";
                    parts[count++] = part; }
                if (!part.startsWith("(")) {
                    part = "(" + part + ")";
                }
                temp.push(part);
            }
        }
        return parts;
    }

    public static String infixToPostfix(String infix) {
        Queue queue  = new Queue(50);
        Stack ops    = new Stack(50);
        Stack notOps = new Stack(50);
        String postfix = "";
        String[] symbols = infix.replace(" ", "").split("");

        for (int i = 0; i < symbols.length; i++) {
            String s = symbols[i];
            if (s.matches("[ABCDabcd]")) {
                queue.enqueue(s);
            } else if (s.equals("~")) {
                notOps.push(s);
            } else if (s.matches("[\\^v+>=]")) {
                ops.push(s);
            } else if (s.equals(")")) {
                while (!queue.isEmpty())  postfix += queue.dequeue();
                while (!ops.isEmpty())    postfix += ops.pop();
                while (!notOps.isEmpty()) postfix += notOps.pop();
            }
        }
        return postfix;
    }


    private static String[] trimParts(String[] parts, int max) {
        // collect non-null entries
        int count = 0;
        for (String p : parts) if (p != null) count++;
        if (count <= max) return parts;

        // keep the last `max` non-null entries in a fresh array the same size
        String[] trimmed = new String[parts.length];
        int kept = 0;
        int skip = count - max;            // how many leading entries to drop
        int dropped = 0;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] == null) continue;
            if (dropped < skip) { dropped++; continue; }
            trimmed[kept++] = parts[i];
        }
        return trimmed;
    }

    public static void TruthTable(String infix) {
        String[][] table = new String[17][10]; // row 0 = header, rows 1-16 = ABCD combos
        int[] valuesVars = new int[4];
        String[] parts = trimParts(splitInfix(infix), 4); // cap at 4 columns so table fits in 80 chars
        Random rnd = new Random();

        String firstColumn = " ";
        table[0][0] = " ABCD | ";
        for (int i = 0; i < parts.length; i++)
            if (parts[i] != null) table[0][i + 1] = parts[i] + " | ";

        for (int i = 0; i < 16; i++) {
            valuesVars[0] = (i / 8) % 2;
            valuesVars[1] = (i / 4) % 2;
            valuesVars[2] = (i / 2) % 2;
            valuesVars[3] = i % 2;
            for (int j = 0; j < valuesVars.length; j++) firstColumn += valuesVars[j];
            table[i + 1][0] = firstColumn + " | ";
            firstColumn = " ";
            for (int j = 0; j < parts.length; j++) {
                if (parts[j] != null) {
                    int length = parts[j].length();
                    String value = "";
                    int half = length / 2;
                    for (int k = 0; k < half; k++) value += " ";
                    value += evaluatePostfix(infixToPostfix(parts[j]), valuesVars);
                    if (length % 2 == 0) half--;
                    for (int k = 0; k < half; k++) value += " ";
                    table[i + 1][j + 1] = value + " | ";
                }
            }
        }

        int numCols = 0;
        for (String part : parts) if (part != null) numCols++;
        int colsToHide = numCols; // one hidden cell per column

        int[] hiddenRows = new int[colsToHide];
        String[] answers = new String[colsToHide];
        for (int col = 1; col <= colsToHide; col++) {
            int row = 1 + rnd.nextInt(16);
            hiddenRows[col - 1] = row;
            answers[col - 1]    = table[row][col];
            int partLength = parts[col - 1].length();
            String value = "";
            int half = partLength / 2;
            for (int k = 0; k < half; k++) value += " ";
            value += "?";
            if (partLength % 2 == 0) half--;
            for (int k = 0; k < half; k++) value += " ";
            table[row][col] = value + " | ";
        }

        clearFullScreen();
        TextAttributes hiddenCellColor = new TextAttributes(Color.black, Color.yellow);

        for (int i=0;i<table.length;i++) {
            for (int j=0;j<table[0].length;j++){
                if (table[i][j] != null ){

                    boolean isHidden = false;

                    String cellContent = table[i][j];
                    String displayPart = cellContent;
                    String separatorPart = "";
                    displayPart = cellContent.substring(0, cellContent.length() - 3);
                    separatorPart = " | ";
                    if (j > 0 && j <= colsToHide) {
                        if (i == hiddenRows[j - 1]) {
                            isHidden = true;
                        }
                    }
                    if (isHidden) {
                        cn.getTextWindow().output(displayPart, hiddenCellColor);
                        cn.getTextWindow().output(separatorPart);
                    } else {
                        cn.getTextWindow().output(table[i][j]);
                    }
                }
            }
            System.out.println();
        }

        Scanner sc = new Scanner(System.in);
        System.out.println("Fill the question marks (0 or 1):");
        int currentRow = 18;
        cn.getTextWindow().setCursorPosition(0, 22);
        cn.getTextWindow().output("Score: " + Score);
        for (int col = 0; col < colsToHide; col++) {
            cn.getTextWindow().setCursorPosition(0, currentRow);
            cn.getTextWindow().output("Row " + hiddenRows[col] + ", col " + (col + 1) + ": ");
            cn.getTextWindow().setCursorPosition(18, currentRow);

            int input = sc.nextInt();
            if (answers[col] != null) {
                String expected = answers[col].replace(" | ", "").trim();
                cn.getTextWindow().setCursorPosition(20, currentRow);
                if (String.valueOf(input).equals(expected)) {
                    cn.getTextWindow().output("Correct!");
                    Score += 3;
                } else {
                    cn.getTextWindow().output("Wrong!");
                    Score -= 2;
                }
            }
            currentRow++;
            cn.getTextWindow().setCursorPosition(0, 22);
            cn.getTextWindow().output("Score: " + Score);
        }
    }


    public static void treeDrawer() {
        for (int x = 0; x < Columns; x++)
            for (int y = 0; y < Rows; y++)
                cn.getTextWindow().output(x, y, ' ');

        cn.getTextWindow().output(17, 3, '/');  cn.getTextWindow().output(27, 3, '\\');
        cn.getTextWindow().output(9,  5, '/');  cn.getTextWindow().output(15, 5, '\\');
        cn.getTextWindow().output(29, 5, '/');  cn.getTextWindow().output(35, 5, '\\');
        cn.getTextWindow().output(4,  7, '/');  cn.getTextWindow().output(8,  7, '\\');
        cn.getTextWindow().output(16, 7, '/');  cn.getTextWindow().output(20, 7, '\\');
        cn.getTextWindow().output(24, 7, '/');  cn.getTextWindow().output(28, 7, '\\');
        cn.getTextWindow().output(36, 7, '/');  cn.getTextWindow().output(40, 7, '\\');

        char[] nodes = exprTree.getNodes();
        int cursor   = exprTree.getCursor();

        for (int i = 1; i <= 15; i++) {
            int cx      = treeX[i];
            int cy      = treeY[i];
            char symbol = nodes[i] == 0 ? '_' : nodes[i];
            if (i == cursor)
                put(cx - 1, cy, "[" + symbol + "]");
            else
                cn.getTextWindow().output(cx, cy, symbol);
        }

        put(2, 12, "W/A/D=Move(-1)  T=Place  R=Remove(-2)  ");
        put(2, 13, "F=Finalize  [M]=ToggleMode  [Space]=Fire");

        if (treeInvalidMsg) {
            put(2, 15, "Invalid tree! Need >=3 variables and depth >=3. (-10 pts)");
            treeInvalidMsg = false;
        } else {
            put(2, 15, "                                                          ");
        }

        // Partial HUD update — drawHUD() was already called on the screen switch
        Backpack_Writer();
        refreshStats();
    }






    public static void showKmap(String infix) {
        int[][] mapping = {{0,1,3,2}, {4,5,7,6}, {12,13,15,14}, {8,9,11,10}};
        int[][] grid = new int[4][4];

        int[] vals = new int[4];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int idx = mapping[row][col];
                vals[0] = (idx / 8) % 2;
                vals[1] = (idx / 4) % 2;
                vals[2] = (idx / 2) % 2;
                vals[3] = idx % 2;
                grid[row][col] = evaluatePostfix(infixToPostfix(infix), vals);
            }
        }

        clearFullScreen();

        int startX = 2;
        int startY = 2;

        cn.getTextWindow().setCursorPosition(startX, startY);
        cn.getTextWindow().output("--- KARNAUGH MAP ---");
        cn.getTextWindow().setCursorPosition(startX, startY + 2);
        cn.getTextWindow().output("AB\\CD | 00 | 01 | 11 | 10 |");
        cn.getTextWindow().setCursorPosition(startX, startY + 3);
        cn.getTextWindow().output("------+----+----+----+----+");

        String[] abLabels = {"00", "01", "11", "10"};

        Queue[] groups = new Queue[16];
        String expectedAnswer = solveKMap(grid, groups);

        int numGroups = 0;
        for (int i = 0; i < groups.length; i++) {
            if (groups[i] != null) numGroups++;
        }

        TextAttributes[] groupColors = {
                new TextAttributes(Color.BLACK, Color.CYAN),
                new TextAttributes(Color.BLACK, Color.GREEN),
                new TextAttributes(Color.WHITE, Color.BLUE),
                new TextAttributes(Color.BLACK, Color.MAGENTA),
                new TextAttributes(Color.BLACK, Color.YELLOW),
                new TextAttributes(Color.BLACK, Color.ORANGE),
                new TextAttributes(Color.WHITE, Color.GRAY)
        };
        TextAttributes colorOverlap = new TextAttributes(Color.WHITE, Color.RED);
        TextAttributes colorZero = new TextAttributes(Color.WHITE, Color.BLACK);

        for (int row = 0; row < 4; row++) {
            int curY = startY + 4 + (row * 2);
            cn.getTextWindow().setCursorPosition(startX, curY);
            cn.getTextWindow().output("  " + abLabels[row] + "  |");

            for (int col = 0; col < 4; col++) {
                int val = mapping[row][col];
                int curX = startX + 8 + (col * 5);

                cn.getTextWindow().setCursorPosition(curX, curY);
                if (grid[row][col] == 1) {

                    Stack belongedGroups = new Stack(16);
                    for (int g = 0; g < numGroups; g++) {
                        if (groups[g] != null && groups[g].search(val)) {
                            belongedGroups.push(g);
                        }
                    }

                    TextAttributes cellColor;
                    if (belongedGroups.size() > 1) {
                        cellColor = colorOverlap;
                    } else if (belongedGroups.size() == 1) {
                        int gIdx = (int) belongedGroups.peek();
                        cellColor = groupColors[gIdx % groupColors.length];
                    } else {
                        cellColor = groupColors[0];
                    }

                    cn.getTextWindow().output(" 1 ", cellColor);
                } else {
                    cn.getTextWindow().output(" 0 ", colorZero);
                }
                cn.getTextWindow().output("|");
            }
            cn.getTextWindow().setCursorPosition(startX, curY + 1);
            cn.getTextWindow().output("------+----+----+----+----+");
        }

        cn.getTextWindow().setCursorPosition(startX, startY + 13);
        cn.getTextWindow().output("Note: ", new TextAttributes(Color.WHITE, Color.BLACK));
        cn.getTextWindow().output(" RED ", colorOverlap);
        cn.getTextWindow().output(" cells represent overlapping groups.", new TextAttributes(Color.WHITE, Color.BLACK));

        cn.getTextWindow().setCursorPosition(55, 9);
        System.out.print("Score     : " + Score + "   ");

        int inputY = startY + 15;
        cn.getTextWindow().setCursorPosition(startX, inputY);
        cn.getTextWindow().output("Simplified expression: ");
        cn.getTextWindow().setCursorPosition(startX + 23, inputY);

        String userAnswer = cn.readLine().trim();
        String normalizedUser = userAnswer.replaceAll("\\s+", "").toLowerCase();
        String normalizedExpected = expectedAnswer.replaceAll("\\s+", "").toLowerCase();

        String[] userTerms = normalizedUser.split("\\+");
        String[] expectedTerms = normalizedExpected.split("\\+");
        java.util.Arrays.sort(userTerms);
        java.util.Arrays.sort(expectedTerms);

        cn.getTextWindow().setCursorPosition(startX, inputY + 2);
        if (java.util.Arrays.equals(userTerms, expectedTerms)) {
            Score += finalizedTreeScore;
            cn.getTextWindow().output("Correct! +" + finalizedTreeScore + " points gained.", new TextAttributes(Color.GREEN, Color.BLACK));
        } else {
            cn.getTextWindow().output("Wrong! Expected simplified expression is: ", new TextAttributes(Color.RED, Color.BLACK));
            cn.getTextWindow().setCursorPosition(startX, inputY + 3);
            cn.getTextWindow().output(expectedAnswer, new TextAttributes(Color.YELLOW, Color.BLACK));
        }

        refreshStats();

        cn.getTextWindow().setCursorPosition(startX, inputY + 5);
        cn.getTextWindow().output("Press Enter to continue...");
        cn.readLine();
    }

    public static String solveKMap(int[][] grid, Queue[] finalGroups) {
        Queue minterms = new Queue(16);
        int[][] mapping = {{0,1,3,2}, {4,5,7,6}, {12,13,15,14}, {8,9,11,10}};

        for(int r = 0; r < 4; r++){
            for(int c = 0; c < 4; c++){
                if(grid[r][c] == 1){
                    minterms.enqueue(String.format("%4s", Integer.toBinaryString(mapping[r][c])).replace(' ', '0'));
                }
            }
        }

        if(minterms.isEmpty()) return "0";
        if(minterms.size() == 16) {
            Queue all = new Queue(16);
            for(int i = 0; i < 16; i++) all.enqueue(i);
            finalGroups[0] = all;
            return "1";
        }

        Queue binary = new Queue(100);
        Queue currentLevel = new Queue(100);

        for(int i = 0; i < minterms.size(); i++){
            String m = (String) minterms.dequeue();
            currentLevel.enqueue(m);
            minterms.enqueue(m);
        }

        boolean merged = true;
        while(merged) {
            merged = false;
            int size = currentLevel.size();
            String[] terms = new String[size];
            for(int i = 0; i < size; i++) terms[i] = (String) currentLevel.dequeue();

            boolean[] used = new boolean[size];
            Queue nextLevel = new Queue(100);

            for(int i = 0; i < size; i++){
                for(int j = i + 1; j < size; j++){
                    String combined = combineTerms(terms[i], terms[j]);
                    if(combined != null){
                        if(!nextLevel.search(combined)){
                            nextLevel.enqueue(combined);
                        }
                        used[i] = true;
                        used[j] = true;
                        merged = true;
                    }
                }
            }

            for(int i = 0; i < size; i++){
                if(!used[i]){
                    if(!binary.search(terms[i])){
                        binary.enqueue(terms[i]);
                    }
                }
            }
            currentLevel = nextLevel;
        }

        int bcount = binary.size();
        String[] barray = new String[bcount];
        for(int i = 0; i < bcount; i++) barray[i] = (String) binary.dequeue();

        int mCount = minterms.size();
        String[] mArray = new String[mCount];
        for(int i = 0; i < mCount; i++) mArray[i] = (String) minterms.dequeue();

        boolean[] mCovered = new boolean[mCount];
        boolean[] bselected = new boolean[bcount];
        int coveredCount = 0;

        while(coveredCount < mCount) {
            int maxCover = -1;
            int idx = -1;

            for(int i = 0; i < bcount; i++){
                if(bselected[i]) continue;
                int cover = 0;
                for(int j = 0; j < mCount; j++){
                    if(!mCovered[j] && covers(barray[i], mArray[j])) cover++;
                }
                if(cover > maxCover){
                    maxCover = cover;
                    idx = i;
                }
            }

            if(idx == -1 || maxCover == 0) break;

            bselected[idx] = true;
            for(int j = 0; j < mCount; j++){
                if(!mCovered[j] && covers(barray[idx], mArray[j])){
                    mCovered[j] = true;
                    coveredCount++;
                }
            }
        }

        int groupIndex = 0;
        Stack termsStack = new Stack(bcount);

        for(int i = 0; i < bcount; i++){
            if(bselected[i]){
                Queue group = new Queue(16);
                for(int j = 0; j < mCount; j++){
                    if(covers(barray[i], mArray[j])){
                        group.enqueue(Integer.parseInt(mArray[j], 2));
                    }
                }
                finalGroups[groupIndex++] = group;
                termsStack.push(formatSingleTerm(barray[i]));
            }
        }

        String[] finalTerms = new String[termsStack.size()];
        int idx = 0;
        while(!termsStack.isEmpty()){
            finalTerms[idx++] = (String) termsStack.pop();
        }
        java.util.Arrays.sort(finalTerms);
        return String.join(" + ", finalTerms);
    }

    private static String combineTerms(String a, String b) {
        int diff = 0;
        String res = "";

        for(int i = 0; i < a.length(); i++){
            if(a.charAt(i) == b.charAt(i)) {
                res += a.charAt(i);
            } else {
                diff++;
                res += '-';
            }
        }

        return diff == 1 ? res : null;
    }

    private static boolean covers(String b, String m) {
        for(int i = 0; i < b.length(); i++){
            if(b.charAt(i) != '-' && b.charAt(i) != m.charAt(i)) return false;
        }
        return true;
    }

    private static String formatSingleTerm(String b) {
        char[] vars = {'A', 'B', 'C', 'D'};
        String term = "";

        for(int i = 0; i < b.length(); i++){
            if(b.charAt(i) == '1') {
                term += vars[i];
            }
            else if(b.charAt(i) == '0') {
                term += vars[i] + "'";
            }
        }

        return term;
    }








}
