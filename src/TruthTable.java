import enigma.console.TextAttributes;
import java.util.Scanner;
import java.util.Random;
import java.awt.Color;

public class TruthTable {

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
        int count = 0;
        for (String p : parts) if (p != null) count++;
        if (count <= max) return parts;

        String[] trimmed = new String[parts.length];
        int kept = 0;
        int skip = count - max;
        int dropped = 0;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] == null) continue;
            if (dropped < skip) { dropped++; continue; }
            trimmed[kept++] = parts[i];
        }
        return trimmed;
    }

    public int TruthTable(String infix, enigma.console.Console cn, int Score) {

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

        Game.clearFullScreen();
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

        return Score;
    }
}