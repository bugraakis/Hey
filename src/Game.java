
import enigma.core.Enigma;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import enigma.console.TextAttributes;

public class Game {

    public static enigma.console.Console cn = Enigma.getConsole("Tree & Table", 80, 30, 16, 25);


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

    static TruthTable truthTable = new TruthTable();
    public Game() throws InterruptedException {

        highScores = HighScoreList.loadFromFile("highscore.txt");

        Menu menu = new Menu();
        int choice = menu.show(cn, highScores);
        if (choice == 3) System.exit(0);

        maze = Maze.loadFromFile("maze.txt");
        scanRobotsFromMaze();

        Input_System in = new Input_System(maze, input_q);

        play = new Player(maze, backpack);
        Game.Life = play.getLife();

        for (int i = 0; i < 10; i++) {
            in.Generator();
            in.Writer();
        }

        play.Starting_Place();

        Dashboard();
        Maze_Drawer(maze);

        registerKeyListener();

        while (!death) {
            long start = System.currentTimeMillis();
            time_unit++;

            if (currentScreen == 1) {
                while (!input_q.isFull()) in.Generator();

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

                if (time_unit % 4 == 0) moveRobots();
                if (time_unit % 20 == 0) in.Writer();

                Maze_Drawer(maze);
                input_list();
                Backpack_Writer();

                if (treeInvalidMsg) {
                    try { Thread.sleep(50); } catch (InterruptedException e) {}

                    cn.getTextWindow().setCursorPosition(2, 22);
                    System.out.print("Tree invalid! Need >=3 variables and depth >=3. -10 pts");
                    try { Thread.sleep(2000); } catch (InterruptedException e) {}
                    cn.getTextWindow().setCursorPosition(2, 22);
                    System.out.print("                                                              ");

                    treeInvalidMsg = false;
                    Maze_Drawer(maze);
                }
            } else if (currentScreen == 3 && !truthTableShown && finalizedInfix != null) {
                truthTableShown = true;
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                clearFullScreen();
                cn.getTextWindow().setCursorPosition(0, 0);
                Score = truthTable.TruthTable(finalizedInfix,cn,Score);
            }

            if (time_unit % 10 == 0) {
                if (currentScreen == 1) {
                    display_time++;
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
        // 1/2/3 switch screens from anywhere
        if (k == KeyEvent.VK_1) {
            if(currentScreen==3)return;


            currentScreen = 1;
            clearMazeArea();
            clearRightPanelExtra(); // wipe any leftover finalize text
            Maze_Drawer(maze);
            return;
        }
        if (k == KeyEvent.VK_2) {
            if(currentScreen==3)return;
            currentScreen = 2; clearMazeArea(); treeDrawer(); return;
        }
        if (k == KeyEvent.VK_3) { if (treeFinalized) currentScreen = 3; return; } // locked until tree is done

        if (currentScreen == 1) {
            // just buffer the intent — main thread applies it to avoid EDT/game-loop race condition
            switch (k) {
                case KeyEvent.VK_UP:    pendingMove = 2;    break;
                case KeyEvent.VK_DOWN:  pendingMove = -2;   break;
                case KeyEvent.VK_LEFT:  pendingMove = -1;   break;
                case KeyEvent.VK_RIGHT: pendingMove = 1;    break;
                case KeyEvent.VK_SPACE: pendingFire = true;  break;
                case KeyEvent.VK_M:     pendingToggle = true; break;
            }
        } else if (currentScreen == 2) {
            handleTreeKey(k);
            if (currentScreen == 2) treeDrawer(); // redraw unless F succeeded (screen changed to 3)
        }
        if (currentScreen != 3) {
            refreshStats();
        }
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
            treeInvalidMsg = true;
            currentScreen = 1;
            clearMazeArea();
            Maze_Drawer(maze);
            refreshStats();
        } else {
            treeFinalized = true;
            currentScreen = 3;

            new Thread(() -> {
                clearMazeArea();
                cn.getTextWindow().setCursorPosition(5, 7);
                System.out.print("Infix  : " + result.infix);
                cn.getTextWindow().setCursorPosition(5, 8);
                System.out.print("Postfix: " + result.postfix);
                cn.getTextWindow().setCursorPosition(5, 10);
                System.out.print("Tree Score: +" + result.treeScore + " pts");

                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                finalizedTreeScore = result.treeScore;
                clearFullScreen();
                cn.getTextWindow().setCursorPosition(0, 0);

                Score = truthTable.TruthTable(result.infix, cn, Score);

                currentScreen = 3;
                KarnaughMap kMap = new KarnaughMap(result.infix);
                Score = kMap.showKmap(cn, Score, finalizedTreeScore);

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
        cn.getTextWindow().setCursorPosition(col, row - 1);
        System.out.print("  GAME OVER  ");
        cn.getTextWindow().setCursorPosition(col, row);
        System.out.print(" Score: " + Score + " ");
        cn.getTextWindow().setCursorPosition(col, row + 1);
        System.out.print(" Press any key ");

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
                } else {
                    cn.getTextWindow().output(x, y, cell);
                }
            }
        }
    }


    public static void Dashboard() {
        for (int i = 0; i < 10; i++) {
            cn.getTextWindow().output(55 + i, 4, '<');
            cn.getTextWindow().output(55 + i, 6, '<');
        }
        cn.getTextWindow().setCursorPosition(55, 3);
        System.out.print("Input");
        refreshStats();
    }

    public static void refreshStats() {
        cn.getTextWindow().setCursorPosition(55, 8);
        System.out.print("Time      : " + display_time + "   ");
        cn.getTextWindow().setCursorPosition(55, 9);
        System.out.print("Score     : " + Score + "   ");
        cn.getTextWindow().setCursorPosition(55, 10);
        System.out.print("Fireball  : " + Fireball + "   ");
        cn.getTextWindow().setCursorPosition(55, 11);
        System.out.print("Life      : " + Life + "   ");

        int barLen    = 10;
        int filled    = Math.max(0, (int)((float) Life / 100f * barLen));
        Color barColor = Life > 50 ? Color.GREEN : (Life > 25 ? Color.YELLOW : Color.RED);
        for (int i = 0; i < barLen; i++) {
            if (i < filled) {
                cn.getTextWindow().output(55 + i, 12, '|', new TextAttributes(barColor, barColor));
            } else {
                cn.getTextWindow().output(55 + i, 12, '-', new TextAttributes(Color.DARK_GRAY, Color.DARK_GRAY));
            }
        }

        cn.getTextWindow().setCursorPosition(55, 13);
        System.out.print("Storage   : " + (play != null && play.isStorageTree() ? "Tree    " : "Backpack"));
    }

    // wipe rows 15-20 of the right panel (finalize summary lives there)
    private static void clearRightPanelExtra() {
        for (int row = 15; row <= 20; row++) {
            cn.getTextWindow().setCursorPosition(55, row);
            System.out.print("                      ");
        }
    }

    public static void input_list() {
        CircularQueue temp = new CircularQueue(10);
        int k = input_q.Size();
        for (int i = 0; i < k; i++) {
            cn.getTextWindow().output(55 + i, 5, (char) input_q.Peek());
            temp.Enqueue(input_q.Dequeue());
        }
        int kl = temp.Size();
        for (int i = 0; i < kl; i++) input_q.Enqueue(temp.Dequeue());
    }

    public static void Backpack_Writer() {
        Stack temp = new Stack(8);
        for (int i = 0; i < 8; i++) {
            cn.getTextWindow().setCursorPosition(60, 21 - i);
            if (!backpack.isEmpty()) {
                char sym = (char) backpack.pop();
                System.out.print("| " + sym + " |");
                temp.push(sym);
            } else {
                System.out.print("|   |");
            }
        }
        int k = temp.size();
        for (int i = 0; i < k; i++) backpack.push(temp.pop());

        cn.getTextWindow().setCursorPosition(60, 22);
        System.out.print("+---+");
        cn.getTextWindow().setCursorPosition(58, 23);
        System.out.print(" Backpack");
    }

    public static void clearMazeArea() {
        for (int x = 0; x < Columns; x++)
            for (int y = 0; y < Rows; y++)
                cn.getTextWindow().output(x, y, ' ');
    }

    // wipes the full console window — used before truth table so the right panel doesn't bleed in
    public static void clearFullScreen() {
        for (int x = 0; x < 80; x++)
            for (int y = 0; y < 30; y++)
                cn.getTextWindow().output(x, y, ' ');
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
            if (i == cursor) {

                cn.getTextWindow().setCursorPosition(cx - 1, cy);
                System.out.print("[" + symbol + "]");
            } else {
                cn.getTextWindow().setCursorPosition(cx, cy);
                System.out.print(symbol);
            }
        }

        cn.getTextWindow().setCursorPosition(2, 12);
        System.out.print("W/A/D=Move(-1)  T=Place  R=Remove(-2)  ");
        cn.getTextWindow().setCursorPosition(2, 13);
        System.out.print("F=Finalize  [M]=ToggleMode  [Space]=Fire");

        if (treeInvalidMsg) {
            cn.getTextWindow().setCursorPosition(2, 15);
            System.out.print("Invalid tree! Need >=3 variables and depth >=3. (-10 pts)");
            treeInvalidMsg = false;
        } else {
            cn.getTextWindow().setCursorPosition(2, 15);
            System.out.print("                                                          ");
        }

        Backpack_Writer();
        refreshStats();
    }



}