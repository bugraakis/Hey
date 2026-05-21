package src;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import enigma.console.TextAttributes;

public class Menu {

    private static final int COLS = 80;
    private static final int ROWS = 30;

    private volatile int selection = 0;
    private volatile int cursor    = 0;

    public int show(enigma.console.Console cn, HighScoreList scores) {
        cursor    = 0;
        selection = 0;
        drawMenu(cn);

        KeyListener kl = new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_1) { selection = 1; return; }
                if (k == KeyEvent.VK_2) { selection = 2; return; }
                if (k == KeyEvent.VK_3) { selection = 3; return; }
                if (k == KeyEvent.VK_4) { selection = 4; return; }
                if (k == KeyEvent.VK_UP)    { cursor = (cursor + 3) % 4; drawMenu(cn); }
                if (k == KeyEvent.VK_DOWN)  { cursor = (cursor + 1) % 4; drawMenu(cn); }
                if (k == KeyEvent.VK_ENTER) { selection = cursor + 1; }
            }
        };

        cn.getTextWindow().addKeyListener(kl);
        while (selection == 0) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        cn.getTextWindow().removeKeyListener(kl);

        if (selection == 2) { showHighScores(cn, scores); selection = 0; return show(cn, scores); }
        if (selection == 3) { showHelp(cn);                selection = 0; return show(cn, scores); }
        return selection; // 1 = start game, 4 = exit
    }

    // ── Pixel font ─────────────────────────────────────────────────────────────
    // Each letter is 6 chars wide, gaps between letters are 2 chars.
    // TREE: T+R+E+E with 3 gaps = 6*4 + 2*3 = 30 wide
    private static final String[] TREE_ROWS = {
        "######  #####   ######  ######",
        "  ##    ##  ##  ##      ##    ",
        "  ##    #####   ####    ####  ",
        "  ##    ## ##   ##      ##    ",
        "  ##    ##  ##  ######  ######"
    };

    // TABLE: T+A+B+L+E with 4 gaps = 6*5 + 2*4 = 38 wide
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

    // ── Main menu ──────────────────────────────────────────────────────────────

    private void drawMenu(enigma.console.Console cn) {
        clearScreen(cn);
        drawPixelTitle(cn, TREE_ROWS,  new Color(80, 100, 255), 1);
        drawPixelTitle(cn, TABLE_ROWS, new Color(210, 80, 10),  7);
        drawMenuItems(cn);
        drawFooter(cn);
    }

    private void drawPixelTitle(enigma.console.Console cn, String[] rows, Color color, int startRow) {
        TextAttributes ta = new TextAttributes(color, Color.BLACK);
        int startCol = (COLS - rows[0].length()) / 2;
        for (int r = 0; r < rows.length; r++) {
            String line = rows[r];
            for (int c = 0; c < line.length(); c++) {
                if (line.charAt(c) != ' ')
                    cn.getTextWindow().output(startCol + c, startRow + r, line.charAt(c), ta);
            }
        }
    }

    private void drawMenuItems(enigma.console.Console cn) {
        TextAttributes sel   = new TextAttributes(Color.WHITE,            Color.BLACK);
        TextAttributes unsel = new TextAttributes(new Color(80, 80, 160), Color.BLACK);
        TextAttributes arr   = new TextAttributes(Color.WHITE,            Color.BLACK);

        for (int i = 0; i < OPTIONS.length; i++) {
            String opt = OPTIONS[i];
            int row = OPT_ROWS[i];
            int col = (COLS - opt.length()) / 2;
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
        String sep  = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -";
        String foot = "UP / DOWN  navigate      ENTER  select      1 / 2 / 3 / 4  direct";
        putCentered(cn, 27, sep,  gray);
        putCentered(cn, 28, foot, gray);
    }

    // ── High Score Screen ──────────────────────────────────────────────────────

    private void showHighScores(enigma.console.Console cn, HighScoreList scores) {
        clearScreen(cn);
        TextAttributes titleCol = new TextAttributes(new Color(80, 100, 255), Color.BLACK);
        TextAttributes textCol  = new TextAttributes(Color.WHITE,              Color.BLACK);
        TextAttributes grayCol  = new TextAttributes(Color.GRAY,               Color.BLACK);

        putCentered(cn, 3, "H I G H   S C O R E S", titleCol);

        if (scores != null && scores.getSize() > 0) {
            scores.display(cn, 20, 6);
        } else {
            putCentered(cn, 8, "( no scores yet )", textCol);
        }

        putCentered(cn, 28, "Press any key to return...", grayCol);
        waitForKey(cn);
    }

    // ── Help Screen ────────────────────────────────────────────────────────────

    private void showHelp(enigma.console.Console cn) {
        clearScreen(cn);
        TextAttributes titleCol   = new TextAttributes(new Color(80, 100, 255), Color.BLACK);
        TextAttributes sectionCol = new TextAttributes(new Color(210, 150, 0),  Color.BLACK);
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
