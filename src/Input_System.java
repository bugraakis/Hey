
import java.util.Random;



public class Input_System {

    static Random rnd = new Random();

    char[][] Maze;
    boolean check = false;
    int X = 0;
    int Y = 0;
    CircularQueue line;

    public Input_System(char[][] maze1, CircularQueue cq) {
        Maze = maze1;
        line = cq;
    }

    // picks a random empty cell and drops the next queued item there
    public void Writer() {
        while (!check) {
            X = 1 + rnd.nextInt(43);
            Y = 1 + rnd.nextInt(19);
            if (Maze[X][Y] == ' ') check = true;
        }
        check = false;
        // if we're placing a robot, also create the Robot object
        if ((char) line.Peek() == 'X') Game.robots.add(new Robot(Game.maze, X, Y));
        Maze[X][Y] = (char) line.Dequeue();
    }

    // 70% logic symbol, 20% fireball, 10% robot
    public void Generator() {
        int input = rnd.nextInt(10);
        if (input < 7) {
            line.Enqueue(logic_randomizer());
        } else if (input < 9) {
            line.Enqueue('@'); // packed fireball
        } else {
            line.Enqueue('X'); // robot
        }
    }

    public static char logic_randomizer() {
        int input = rnd.nextInt(15);
        // A B C C D a b c d ~ ^ v + > =
        if (input == 0)  return 'A';
        if (input == 1)  return 'B';
        if (input == 2)  return 'C';
        if (input == 3)  return 'C';
        if (input == 4)  return 'D';
        if (input == 5)  return 'a';
        if (input == 6)  return 'b';
        if (input == 7)  return 'c';
        if (input == 8)  return 'd';
        if (input == 9)  return '~';
        if (input == 10) return '^';
        if (input == 11) return 'v';
        if (input == 12) return '+';
        if (input == 13) return '>';
        if (input == 14) return '=';
        return 'L'; // shouldn't happen
    }
}