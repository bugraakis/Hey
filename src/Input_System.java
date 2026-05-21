package src;
import java.util.Random;

import static src.Main.maze;
import static src.Main.robots;

public class Input_System {
    static Random rnd = new Random();

    char[][] Maze;
    boolean check = false;
    int X =0;
    int Y =0;
    CircularQueue line;

    public Input_System(char[][] maze1,CircularQueue cq){
        Maze = maze1;
        line = cq;
    }

    public void Writer(){

        while(!check){
            X = rnd.nextInt(1,44);
            Y = rnd.nextInt(1,20);

            if(Maze[X][Y] == ' '){check = true;}
        }
        check = false;
        if ((char)line.Peek() == 'X') robots.add(new Robot(maze, X, Y));
        Maze[X][Y] = (char)line.Dequeue();

    }

    public void Generator(){
        int input = rnd.nextInt(0,10);
        if(input <7)
        {
             line.Enqueue(logic_randomizer());
        }

        else if(7<= input && input<9)
        {
            line.Enqueue('@');
        }
        else if(input == 9)
        {
            line.Enqueue('X');
        }
    }

    public static char logic_randomizer(){
        int input = rnd.nextInt(0,15);
        if(input ==0){
        return 'A';
        }
        else if(input ==1){
            return'B';
        }
      else if(input ==2){
            return'C';
        }
      else if(input ==3){
            return'C';
        }
      else if(input ==4){
            return'D';
        }
      else if(input ==5){
            return'a';
        }
      else if(input ==6){
            return'b';
        }
      else if(input ==7){
            return'c';
        }
      else if(input ==8){
            return'd';
        }
      else if(input ==9){
            return'~';
        }
      else if(input ==10){
            return'^';
        }
      else if(input ==11){
            return'v';
        }
      else if(input ==12){
            return'+';
        }
      else if(input ==13){
            return'>';
        }
        else if(input ==14){
            return'=';
        }
        else{return 'L';}
    }
}
