package src;

/**
 * Binary expression tree.
 * Nodes are numbered 1-15 (root = 1, left child = 2n, right child = 2n+1).
 * The green cursor marks the active slot for symbol placement.
 *
 * Responsibilities split:
 *   Efe  – Tree Screen rendering, cursor display, W/A/D navigation
 *   Buğra – F key finalization (this class), infix/postfix generation
 */
public class ExpressionTree {

    private static final int CAPACITY = 15;
    private static final String VARIABLES = "ABCDabcd";

    public char[] nodes = new char[CAPACITY + 1]; // 1-indexed
    private int    cursor = 1;

    public ExpressionTree() {
        for (int i = 1; i <= CAPACITY; i++) nodes[i] = 0; // 0 = empty
    }

    // ── Cursor navigation (called by Tree Screen key handlers) ──────────────

    /** Move cursor to parent. Returns false if already at root. Costs -1 pt. */
    public boolean moveCursorUp() {
        if (cursor <= 1) return false;
        cursor /= 2;
        Main.Score -= 1;
        return true;
    }

    /** Move cursor to left child. Costs -1 pt. */
    public boolean moveCursorLeft() {
        int next = cursor * 2;
        if (next > CAPACITY) return false;
        cursor = next;
        Main.Score -= 1;
        return true;
    }

    /** Move cursor to right child. Costs -1 pt. */
    public boolean moveCursorRight() {
        int next = cursor * 2 + 1;
        if (next > CAPACITY) return false;
        cursor = next;
        Main.Score -= 1;
        return true;
    }

    // ── Symbol placement / removal ──────────────────────────────────────────

    /** Place symbol at cursor. Returns false if slot is occupied. */
    public boolean placeSymbol(char symbol) {
        if (nodes[cursor] != 0) return false;
        nodes[cursor] = symbol;
        advanceCursor();
        return true;
    }

    /** Remove symbol at cursor → caller should push it to backpack. Costs -2 pts. */
    public char removeSymbol() {
        char sym = nodes[cursor];
        nodes[cursor] = 0;
        Main.Score -= 2;
        return sym;
    }

    /** Advance cursor to the next empty slot (BFS order). */
    private void advanceCursor() {
        for (int i = 1; i <= CAPACITY; i++) {
            if (nodes[i] == 0) { cursor = i; return; }
        }
    }

    // ── F key: tree finalization ─────────────────────────────────────────────

    /**
     * Attempt to finalize the tree (F key).
     * Requirements: ≥3 distinct variables, depth ≥3.
     * On failure: deducts 10 pts, returns null.
     * On success: returns FinalizeResult with infix and postfix strings.
     */
    public FinalizeResult finalizeTree() {
        int varCount = countVariables();
        int depth    = computeDepth(1, 1);

        if (varCount < 3 || depth < 3) {
            Main.Score -= 10;
            return null;
        }

        String infix   = buildInfix(1);
        String postfix = buildPostfix(1).trim();
        int    treeScore = countNodes() * 10;
        Main.Score += treeScore;
        return new FinalizeResult(infix, postfix, treeScore);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private int countVariables() {
        boolean[] seen = new boolean[256];
        int count = 0;
        for (int i = 1; i <= CAPACITY; i++) {
            char c = nodes[i];
            if (c != 0 && VARIABLES.indexOf(c) >= 0 && !seen[c]) {
                seen[c] = true;
                count++;
            }
        }
        return count;
    }

    private int computeDepth(int idx, int currentDepth) {
        if (idx > CAPACITY || nodes[idx] == 0) return currentDepth - 1;
        int left  = computeDepth(idx * 2,     currentDepth + 1);
        int right = computeDepth(idx * 2 + 1, currentDepth + 1);
        return Math.max(left, right);
    }

    private int countNodes() {
        int count = 0;
        for (int i = 1; i <= CAPACITY; i++) if (nodes[i] != 0) count++;
        return count;
    }

    private String buildInfix(int idx) {
        if (idx > CAPACITY || nodes[idx] == 0) return "";
        char c = nodes[idx];
        if (VARIABLES.indexOf(c) >= 0) return String.valueOf(c);

        String left  = buildInfix(idx * 2);
        String right = buildInfix(idx * 2 + 1);

        if (c == '~') {
            return "(~" + left + ")";
        }
        return "(" + left + " " + c + " " + right + ")";
    }

    private String buildPostfix(int idx) {
        if (idx > CAPACITY || nodes[idx] == 0) return "";
        char c = nodes[idx];
        if (VARIABLES.indexOf(c) >= 0) return String.valueOf(c) + " ";

        String left  = buildPostfix(idx * 2);
        String right = buildPostfix(idx * 2 + 1);
        return left + right + c + " ";
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int   getCursor()          { return cursor; }
    public char  getNode(int idx)     { return idx >= 1 && idx <= CAPACITY ? nodes[idx] : 0; }
    public char[] getNodes()          { return nodes; }

    // ── Result container ─────────────────────────────────────────────────────

    public static class FinalizeResult {
        public final String infix;
        public final String postfix;
        public final int    treeScore;

        FinalizeResult(String infix, String postfix, int treeScore) {
            this.infix     = infix;
            this.postfix   = postfix;
            this.treeScore = treeScore;
        }
    }
}
