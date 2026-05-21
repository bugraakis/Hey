package src;

import java.io.*;
import java.util.Scanner;

/**
 * Doubly-linked list of high-score entries, kept sorted descending by score.
 * Loaded from / saved to highscore.txt.
 */
public class HighScoreList {

    private static class Node {
        String name;
        int    score;
        Node   prev, next;

        Node(String name, int score) {
            this.name  = name;
            this.score = score;
        }
    }

    private Node head, tail;
    private int  size;

    /** Insert a new entry in descending score order. */
    public void insert(String name, int score) {
        Node node = new Node(name, score);
        if (head == null) {
            head = tail = node;
        } else if (score >= head.score) {
            node.next  = head;
            head.prev  = node;
            head       = node;
        } else {
            Node cur = head;
            while (cur.next != null && cur.next.score > score) {
                cur = cur.next;
            }
            node.next = cur.next;
            node.prev = cur;
            if (cur.next != null) cur.next.prev = node;
            else                  tail           = node;
            cur.next = node;
        }
        size++;
    }

    /**
     * Load entries from file. Each line: "Name    score"
     * (name may contain spaces; last whitespace-separated token is the score).
     */
    public static HighScoreList loadFromFile(String filename) {
        HighScoreList list = new HighScoreList();
        try (Scanner sc = new Scanner(new File(filename))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                int sep = line.lastIndexOf(' ');
                if (sep < 0) continue;
                String name  = line.substring(0, sep).trim();
                int    score = Integer.parseInt(line.substring(sep + 1).trim());
                list.insert(name, score);
            }
        } catch (FileNotFoundException | NumberFormatException ignored) { }
        return list;
    }

    /** Persist the list back to the same file format. */
    public void saveToFile(String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            Node cur = head;
            while (cur != null) {
                pw.printf("%-20s %d%n", cur.name, cur.score);
                cur = cur.next;
            }
        } catch (IOException e) {
            System.out.println("Could not save high scores: " + e.getMessage());
        }
    }

    /**
     * Render the top entries on the Enigma console starting at (col, row).
     * Each row: rank, name, score.
     */
    public void display(enigma.console.Console cn, int startCol, int startRow) {
        Node cur = head;
        int  row = startRow;
        int  rank = 1;
        while (cur != null && row < Maze.ROWS - 1) {
            cn.getTextWindow().setCursorPosition(startCol, row);
            System.out.printf("%2d. %-18s %6d%n", rank, cur.name, cur.score);
            cur  = cur.next;
            row++;
            rank++;
        }
    }

    public int getSize() { return size; }
    public Node getHead() { return head; }
}
