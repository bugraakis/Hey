
import java.io.*;
import java.util.Scanner;

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

    // keeps the list sorted high→low
    public void insert(String name, int score) {
        Node node = new Node(name, score);
        if (head == null) {
            head = tail = node;
        } else if (score >= head.score) {
            // new top score
            node.next = head;
            head.prev = node;
            head      = node;
        } else {
            Node cur = head;
            while (cur.next != null && cur.next.score > score) cur = cur.next;
            node.next = cur.next;
            node.prev = cur;
            if (cur.next != null) cur.next.prev = node;
            else                  tail           = node;
            cur.next = node;
        }
        size++;
    }

    // each line in the file: "Name   score"
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
        } catch (FileNotFoundException | NumberFormatException ignored) {}
        return list;
    }

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

    public void display(enigma.console.Console cn, int startCol, int startRow) {
        Node cur  = head;
        int  row  = startRow;
        int  rank = 1;
        while (cur != null && row < Maze.ROWS - 1) {
            cn.getTextWindow().setCursorPosition(startCol, row);
            System.out.printf("%2d. %-18s %6d%n", rank, cur.name, cur.score);
            cur = cur.next;
            row++;
            rank++;
        }
    }

    public int  getSize() { return size; }
    public Node getHead() { return head; }
}