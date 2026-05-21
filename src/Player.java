package src;

import java.util.Random;

public class Player {

    int life;
    int X, Y;
    /** Last movement direction: 1=right -1=left 2=up -2=down */
    int facing = 1;
    Random rnd = new Random();
    char[][] maze;
    boolean check = false;
    Stack bp;
    /** true = collect to tree slot, false = collect to backpack */
    boolean storageTree = true;

    public Player(char[][] inputMaze, Stack inputBp) {
        life = 100;
        maze = inputMaze;
        bp   = inputBp;
    }

    public void Starting_Place() {
        while (!check) {
            X = rnd.nextInt(1, 44);
            Y = rnd.nextInt(1, 20);
            if (maze[X][Y] == ' ') check = true;
        }
        check = false;
        maze[X][Y] = 'P';
    }

    public int getLife()  { return life; }
    public int getX()     { return X; }
    public int getY()     { return Y; }
    public int getFacing(){ return facing; }

    public void setX(int x) { X = x; }
    public void setY(int y) { Y = y; }

    /** direction: 1=right, -1=left, 2=up, -2=down */
    public void Movement(int direction) {
        facing = direction;
        int nx = X, ny = Y;
        if      (direction ==  1) nx = X + 1;
        else if (direction == -1) nx = X - 1;
        else if (direction ==  2) ny = Y - 1;
        else if (direction == -2) ny = Y + 1;

        // Boundary / wall check
        if (nx <= 0 || nx >= 44 || ny <= 0 || ny >= 20) return;
        if (maze[nx][ny] == '#' || maze[nx][ny] == 'X')  return;

        maze[X][Y] = ' ';
        X = nx;
        Y = ny;
        Collecting_Elements();
        maze[X][Y] = 'P';
    }

    public void Collecting_Elements() {
        char cell = maze[X][Y];
        if (cell == ' ' || cell == '#' || cell == 'P' || cell == 'X') return;

        if (cell == '@') {
            // Packed fireball → add to fireball count
            Main.Fireball++;
            maze[X][Y] = ' ';
            return;
        }

        // Logic symbol
        if (!bp.isFull()&&!isStorageTree()) {
            bp.push(cell);
            maze[X][Y] = ' ';
        }

        if(isStorageTree()){
            for(int i=1;i<16;i++){
                if(Main.exprTree.getNode(i)==0){
                    Main.exprTree.nodes[i]=cell;
                    maze[X][Y] = ' ';
                    return;

                }
            }
        }
    }

    /**
     * Fire a fireball in the facing direction.
     * Requires at least one fireball in inventory.
     * Spawns a Fireball one cell ahead of the player.
     */
    public Fireball fire() {
        if (Main.Fireball <= 0) return null;

        int fx = X, fy = Y;
        switch (facing) {
            case  1: fx = X + 1; break;
            case -1: fx = X - 1; break;
            case  2: fy = Y - 1; break;
            case -2: fy = Y + 1; break;
        }

        if (!Maze.isPassable(maze, fx, fy) || maze[fx][fy] == '#') return null;

        Main.Fireball--;
        return new Fireball(maze, fx, fy, facing);
    }

    /** Reduce life by amount; sets Main.death = true if life reaches 0. */
    public void takeDamage(int amount) {
        life -= amount;
        Main.Life = life;
        if (life <= 0) {
            life = 0;
            Main.death = true;
        }
    }

    /** Toggle storage mode between Tree and Backpack. */
    public void toggleStorage() {
        storageTree = !storageTree;
    }

    public boolean isStorageTree() { return storageTree; }
}
