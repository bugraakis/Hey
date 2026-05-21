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
                if (k == KeyEvent.VK_UP)    { cursor = (cursor + 2) % 3; drawMenu(cn); }
                if (k == KeyEvent.VK_DOWN)  { cursor = (cursor + 1) % 3; drawMenu(cn); }
                if (k == KeyEvent.VK_ENTER) { selection = cursor + 1; }
            }
        };

        cn.getTextWindow().addKeyListener(kl);
        while (selection == 0) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        cn.getTextWindow().removeKeyListener(kl);

        if (selection == 2) {
            showHighScores(cn, scores);
            selection = 0;
            return show(cn, scores);
        }
        return selection;
    }

    // 5-row pixel font for "TREE" — each string is exactly 30 chars wide
    // Letters: T(6) + gap(2) + R(6) + gap(2) + E(6) + gap(2) + E(6) = 30
    private static final String[] TITLE_ROWS = {
        "######  #####   ######  ######",
        "  ##    ##  ##  ##      ##    ",
        "  ##    #####   ####    ####  ",
        "  ##    ## ##   ##      ##    ",
        "  ##    ##  ##  ######  ######"
    };

    // Fixed star positions [col, row] — keep away from content rows 1-5, 7, 13, 16, 19
    private static final int[][] STARS = {
        { 1, 0},{10, 0},{20, 0},{30, 0},{42, 0},{55, 0},{65, 0},{75, 0},
        { 4, 6},{14, 6},{36, 6},{50, 6},{60, 6},{70, 6},{79, 6},
        { 0, 8},{ 8, 9},{18, 9},{28, 9},{38, 9},{48, 9},{58, 9},{68, 9},{77, 9},
        { 3,10},{13,10},{33,10},{44,10},{54,10},{64,10},{74,10},
        { 6,11},{16,11},{26,11},{37,11},{47,11},{57,11},{67,11},{76,11},
        { 2,12},{12,12},{22,12},{32,12},{43,12},{53,12},{63,12},{72,12},
        { 5,14},{78,14},
        { 1,15},{71,15},
        { 3,17},{77,17},
        { 7,18},{74,18},
        { 4,20},{14,20},{24,20},{34,20},{45,20},{55,20},{65,20},{74,20},
        { 7,21},{17,21},{27,21},{37,21},{47,21},{59,21},{69,21},{77,21},
        { 2,22},{12,22},{22,22},{32,22},{43,22},{53,22},{63,22},{73,22},
        { 5,23},{15,23},{25,23},{36,23},{46,23},{56,23},{66,23},{75,23},
        { 0,24},{10,24},{20,24},{30,24},{40,24},{50,24},{60,24},{70,24},{79,24},
        { 3,25},{13,25},{23,25},{33,25},{44,25},{54,25},{64,25},{74,25},
        { 6,26},{16,26},{26,26},{37,26},{47,26},{57,26},{67,26},{77,26},
        { 4,29},{12,29},{22,29},{33,29},{43,29},{53,29},{63,29},{73,29},
    };

    private static final String[] OPTIONS  = {
        "S T A R T   G A M E",
        "H I G H   S C O R E S",
        "E X I T"
    };
    private static final int[] OPT_ROWS = {13, 16, 19};

    // ── Drawing ────────────────────────────────────────────────────────────────

    private void drawMenu(enigma.console.Console cn) {
        clearScreen(cn);
        drawStars(cn);
        drawTitle(cn);
        drawSubtitle(cn);
        drawMenuItems(cn);
        drawFooter(cn);
    }

    private void clearScreen(enigma.console.Console cn) {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                cn.getTextWindow().output(c, r, ' ');
    }

    private void drawStars(enigma.console.Console cn) {
        TextAttributes dim = new TextAttributes(Color.DARK_GRAY, Color.BLACK);
        for (int[] s : STARS)
            cn.getTextWindow().output(s[0], s[1], '.', dim);
    }

    private void drawTitle(enigma.console.Console cn) {
        TextAttributes blue = new TextAttributes(new Color(80, 100, 255), Color.BLACK);
        int startCol = (COLS - TITLE_ROWS[0].length()) / 2; // = 25
        for (int r = 0; r < TITLE_ROWS.length; r++) {
            String line = TITLE_ROWS[r];
            for (int c = 0; c < line.length(); c++) {
                if (line.charAt(c) != ' ')
                    cn.getTextWindow().output(startCol + c, 1 + r, line.charAt(c), blue);
            }
        }
    }

    private void drawSubtitle(enigma.console.Console cn) {
        TextAttributes orange = new TextAttributes(new Color(210, 80, 10), Color.BLACK);
        TextAttributes lgray  = new TextAttributes(Color.LIGHT_GRAY,      Color.BLACK);

        String sub = "&   T A B L E";
        int sc = (COLS - sub.length()) / 2;
        for (int i = 0; i < sub.length(); i++)
            cn.getTextWindow().output(sc + i, 7, sub.charAt(i), orange);

        String ver = "v1.0";
        int vc = COLS - ver.length() - 2;
        for (int i = 0; i < ver.length(); i++)
            cn.getTextWindow().output(vc + i, 8, ver.charAt(i), lgray);
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
        int    sepC = (COLS - sep.length()) / 2;
        for (int i = 0; i < sep.length(); i++)
            cn.getTextWindow().output(sepC + i, 27, sep.charAt(i), gray);

        String foot = "UP / DOWN  to navigate      ENTER  to select      1 / 2 / 3  direct";
        int    footC = (COLS - foot.length()) / 2;
        for (int i = 0; i < foot.length(); i++)
            cn.getTextWindow().output(footC + i, 28, foot.charAt(i), gray);
    }

    // ── High Score Screen ──────────────────────────────────────────────────────

    private void showHighScores(enigma.console.Console cn, HighScoreList scores) {
        clearScreen(cn);
        drawStars(cn);

        TextAttributes titleCol = new TextAttributes(new Color(80, 100, 255), Color.BLACK);
        TextAttributes textCol  = new TextAttributes(Color.WHITE,              Color.BLACK);
        TextAttributes grayCol  = new TextAttributes(Color.GRAY,               Color.BLACK);

        String hdr = "H I G H   S C O R E S";
        int hc = (COLS - hdr.length()) / 2;
        for (int i = 0; i < hdr.length(); i++)
            cn.getTextWindow().output(hc + i, 3, hdr.charAt(i), titleCol);

        if (scores != null && scores.getSize() > 0) {
            scores.display(cn, 20, 6);
        } else {
            String none = "( no scores yet )";
            int nc = (COLS - none.length()) / 2;
            for (int i = 0; i < none.length(); i++)
                cn.getTextWindow().output(nc + i, 8, none.charAt(i), textCol);
        }

        String back = "Press any key to return...";
        int bc = (COLS - back.length()) / 2;
        for (int i = 0; i < back.length(); i++)
            cn.getTextWindow().output(bc + i, 27, back.charAt(i), grayCol);

        waitForKey(cn);
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
