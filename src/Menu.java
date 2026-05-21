
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Menu {

    private static final int COL = 12;
    private volatile int selection = 0;

    public int show(enigma.console.Console cn, HighScoreList scores) {
        drawMenu(cn);

        KeyListener kl = new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if      (k == KeyEvent.VK_1) selection = 1;
                else if (k == KeyEvent.VK_2) selection = 2;
                else if (k == KeyEvent.VK_3) selection = 3;
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

    private void drawMenu(enigma.console.Console cn) {
        clearScreen(cn);
        int row = 4;
        printAt(cn, COL, row++, "+---------------------------+");
        printAt(cn, COL, row++, "|  TREE  &  TABLE GAME      |");
        printAt(cn, COL, row++, "|  Logic Maze Adventure     |");
        printAt(cn, COL, row++, "+---------------------------+");
        printAt(cn, COL, row++, "|  [1]  Start Game          |");
        printAt(cn, COL, row++, "|  [2]  High Scores         |");
        printAt(cn, COL, row++, "|  [3]  Exit                |");
        printAt(cn, COL, row++, "+---------------------------+");
        printAt(cn, COL, row,   "   Press a key to select");
    }

    private void showHighScores(enigma.console.Console cn, HighScoreList scores) {
        clearScreen(cn);
        printAt(cn, COL, 2, "=== HIGH SCORES ===");
        if (scores != null && scores.getSize() > 0) {
            scores.display(cn, COL, 4);
        } else {
            printAt(cn, COL, 4, "(no scores yet)");
        }
        printAt(cn, COL, 19, "Press any key to return...");
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

    private void clearScreen(enigma.console.Console cn) {
        for (int col = 0; col < 45; col++)
            for (int row = 0; row < 21; row++)
                cn.getTextWindow().output(col, row, ' ');
    }

    private void printAt(enigma.console.Console cn, int col, int row, String text) {
        cn.getTextWindow().setCursorPosition(col, row);
        System.out.print(text);
    }
}