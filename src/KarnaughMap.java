import enigma.console.TextAttributes;
import java.awt.*;
import java.util.Scanner;
import java.util.Random;

public class KarnaughMap {

    private String infix;
    private int[][] grid;
    private Queue[] groups;
    private String expectedAnswer;
    private int[][] mapping = {{0, 1, 3, 2}, {4, 5, 7, 6}, {12, 13, 15, 14}, {8, 9, 11, 10}};

    public KarnaughMap(String infix) {
        this.infix = infix;
        this.grid = new int[4][4];
        this.groups = new Queue[16];

        int[] vals = new int[4];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int idx = mapping[row][col];
                vals[0] = (idx / 8) % 2;
                vals[1] = (idx / 4) % 2;
                vals[2] = (idx / 2) % 2;
                vals[3] = idx % 2;
                this.grid[row][col] = TruthTable.evaluatePostfix(TruthTable.infixToPostfix(infix), vals);
            }
        }
        this.expectedAnswer = solveKMap(this.grid, this.groups);
    }

    public int showKmap(enigma.console.Console cn, int currentScore, int finalizedTreeScore) {

        Game.clearFullScreen();

        int startX = 2;
        int startY = 2;

        cn.getTextWindow().setCursorPosition(startX, startY);
        cn.getTextWindow().output("--- KARNAUGH MAP ---");
        cn.getTextWindow().setCursorPosition(startX, startY + 2);
        cn.getTextWindow().output("AB\\CD | 00 | 01 | 11 | 10 |");
        cn.getTextWindow().setCursorPosition(startX, startY + 3);
        cn.getTextWindow().output("------+----+----+----+----+");

        String[] abLabels = {"00", "01", "11", "10"};

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
        System.out.print("Score     : " + currentScore + "   ");

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
            currentScore += finalizedTreeScore;
            cn.getTextWindow().output("Correct! +" + finalizedTreeScore + " points gained.", new TextAttributes(Color.GREEN, Color.BLACK));
        } else {
            cn.getTextWindow().output("Wrong! Expected simplified expression is: ", new TextAttributes(Color.RED, Color.BLACK));
            cn.getTextWindow().setCursorPosition(startX, inputY + 3);
            cn.getTextWindow().output(expectedAnswer, new TextAttributes(Color.YELLOW, Color.BLACK));
        }

        Game.refreshStats();

        cn.getTextWindow().setCursorPosition(startX, inputY + 5);
        cn.getTextWindow().output("Press Enter to continue...");
        cn.readLine();

        return currentScore;
    }

    private String solveKMap(int[][] grid, Queue[] finalGroups) {
        Queue minterms = new Queue(16);

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

    private String combineTerms(String a, String b) {
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

    private boolean covers(String b, String m) {
        for(int i = 0; i < b.length(); i++){
            if(b.charAt(i) != '-' && b.charAt(i) != m.charAt(i)) return false;
        }
        return true;
    }

    private String formatSingleTerm(String b) {
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