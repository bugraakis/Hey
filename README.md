# Tree & Table — Logic Maze Game

> **CME1252 · Project 2 · 2025–2026 Spring Semester**
> Duration: 7 weeks | First Eval: 04.05.2026 | Final Eval: 22.05.2026

A Java console game built on the **Enigma** text-window library. The player navigates a maze collecting logic symbols, builds an expression tree, and evaluates its truth table to maximize their score.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [How to Build & Run](#how-to-build--run)
- [Project Structure](#project-structure)
- [Game Screens](#game-screens)
  - [1. Maze Screen](#1-maze-screen)
  - [2. Tree Screen](#2-tree-screen)
  - [3. Table Screen](#3-table-screen)
- [Controls](#controls)
- [Game Elements & Symbols](#game-elements--symbols)
- [Scoring](#scoring)
- [File Formats](#file-formats)
- [Data Structures](#data-structures)
- [Workload Distribution](#workload-distribution)
- [Changelog](#changelog)

---

## Tech Stack

| Component | Source |
|---|---|
| Language | Java 17+ |
| Console window & rendering | `Enigma-Edited2.jar` (edited Enigma library) |
| Keyboard & mouse input | Edited `Game.java` / `GameTest.java` from Mouse Keyboard Standard Code |

### Enigma Library — Key Classes

The Enigma JAR provides a Java2D-rendered text window — no raw terminal or ANSI codes needed.

| Class | Package | Purpose |
|---|---|---|
| `TextWindow` | `enigma.console` | The main drawable text grid |
| `Console` | `enigma.console` | High-level console interface |
| `TextAttributes` | `enigma.console` | Foreground/background colors per cell |
| `Java2DTextWindow` | `enigma.console.java2d` | Swing/Java2D rendering backend |
| `TextWindowListener` | `enigma.event` | Keyboard events |
| `TextMouseListener` | `enigma.event` | Mouse click events |
| `TextMouseMotionListener` | `enigma.event` | Mouse motion events |
| `TextMouseEvent` | `enigma.event` | Mouse event data (row, col) |
| `TextMouseAdapter` | `enigma.event` | Adapter class for mouse listeners |

### Mouse & Keyboard Standard Code

The edited `Game.java` provides the baseline game loop and input dispatch. Your edited version of it becomes the entry point that wires Enigma events into the game logic.

---

## How to Build & Run

### Prerequisites

- Java 17+ — [Download JDK](https://adoptium.net/)
- `Enigma-Edited2.jar` in the `lib/` folder

### Project layout for compilation

```
Tree & Table/
├── lib/
│   └── Enigma-Edited2.jar
├── src/
│   └── *.java
└── out/
```

### Build

```bash
javac -cp "lib/Enigma-Edited2.jar" -d out src/*.java
```

### Run

```bash
java -cp "out;lib/Enigma-Edited2.jar" Main
```

> On macOS/Linux replace `;` with `:` in the classpath.

Make sure `maze.txt` and `highscore.txt` are in the working directory when you run.

### IntelliJ IDEA Setup

1. `File → Open` → select the project root.
2. Right-click `lib/Enigma-Edited2.jar` → `Add as Library`.
3. Mark `src/` as Sources Root.
4. Run `Main.java`.

---

## Project Structure

```
Tree & Table/
├── README.md
├── maze.txt                  # Initial maze layout (loaded at start)
├── highscore.txt             # Default high score entries
├── lib/
│   └── Enigma-Edited2.jar    # Enigma console library (do not modify)
├── src/
│   ├── Main.java             # Entry point — launches Enigma window
│   ├── Game.java             # Game loop, timing, screen switching (edited from standard code)
│   ├── GameTest.java         # Test harness (edited from standard code)
│   ├── Maze.java             # 21x45 grid, movement, collision
│   ├── Player.java           # Player state, backpack, fireball
│   ├── Robot.java            # Enemy AI (random + targeted modes)
│   ├── InputQueue.java       # Queue logic, element generation
│   ├── ExpressionTree.java   # Binary tree, cursor, node placement
│   ├── Expression.java       # Infix/postfix traversal, evaluation
│   ├── TruthTable.java       # Truth table computation
│   ├── KarnaughMap.java      # K-map display and simplification
│   └── HighScoreList.java    # Doubly linked list for scores
└── docs/
    └── CME1252 - Project 2 - Tree Table Game.doc
```

### Enigma Usage Pattern

```java
// Typical setup from the standard code base
Console console = Enigma.getConsole("Tree & Table");
TextWindow window = console.getTextWindow();

// Draw a character at row, col with color
TextAttributes attr = new TextAttributes(Color.GREEN, Color.BLACK);
window.putChar(row, col, 'P', attr);

// Listen for keyboard input
console.addTextWindowListener(new TextWindowListener() {
    public void keyPressed(TextWindowEvent e) {
        int key = e.getKeyCode();
        // handle key...
    }
});
```

---

## Game Screens

The game is played on a **21×45 grid** (including outer walls).

```
#############################################  Input
#        v            #         #           #  <<<<<<<<<<
#                     #      B  #   +       #  B+>aX=v@@c
#  ####         X     ###  ###  #           #  <<<<<<<<<<
#          ####                 #     #     #
#   @      #       ^  ###             #     #  Time     :   54
#    @#    #          #    Po   X           #  Score    :    0
# d   #    ####   #####                 v   #  Fireball :    6
#     #  =                ######      #     #  Life     :   95
#     #           ###          #   B  #     #  Storage  : Tree
#######    #####  #      o     #      #######
#      A          #  ####      ####   #     #      |   |
#          #####  #      X            #     #      |   |
#      X                    a               #      |   |
#  ###  #  #####  ####  #  ####  #### X     #      |   |
#    #  #     @         #     #     #   #   #      | ~ |
#    #  #    #          #     #   C #   #   #      | D |
#  ###       #    #   ###     ###   #   #   #      | ^ |
#      D  ####    #        >       @        #      | b |
#                 #                         #      +---+
#############################################     Backpack
```

### 1. Maze Screen

Switch with **Key 1**. Leaving the maze pauses the game.

**Player (`P`)**
- Moves in 4 directions with cursor keys.
- Starts with **100 life points** and a **backpack of 8 slots**.
- **Storage Mode** (toggle **Key M**): `Tree` or `Backpack`.
  - `Tree` mode: collected symbols go to the active tree cursor slot.
  - `Backpack` mode: symbols go to the backpack (overflows to tree if full).
- Press **Space** to fire a fireball in the facing direction.

**Enemy Robots (`X`)**
- Move at **1/4 of player speed**.
- **50% Green** — random movement.
- **50% Red** — targeted movement toward the nearest logic symbol.
- Blocked by obstacles; cannot dodge fireballs.
- Deals **5 life point damage per time unit** when adjacent to the player.

**Fireball (`@` / `o`)**
- `@` = packed (player-collectible only).
- `o` = active, moves 1 square per time unit in the fired direction.
- Destroys any robot it hits and continues — one fireball can chain multiple kills.
- Stops on hitting any non-robot obstacle.

**Input Queue**

Size 10, always full. Inserts one element into a random empty cell every **2 seconds (20 time units)**. At game start, the first 10 elements are placed immediately.

| Element | Probability |
|---|---|
| Logic symbols (14 total) | 7/10 |
| Packed fireball (`@`) | 2/10 |
| Robot (`X`) | 1/10 |

**Timing Reference**

| Event | Frequency |
|---|---|
| Player moves 1 square | Every 1 time unit (100 ms) |
| Enemy robot moves 1 square | Every 4 time units |
| Fireball moves 1 square | Every 1 time unit |
| New queue element inserted | Every 20 time units (2 s) |
| Neighbor square damage tick | Every 1 time unit |

---

### 2. Tree Screen

Switch with **Key 2**.

The green tree cursor marks the active slot. Symbols collected in `Tree` storage mode are placed at the cursor, which then advances to the next empty slot.

**Tree Controls**

| Key | Action | Penalty |
|---|---|---|
| W | Move cursor to parent | -1 pt |
| A | Move cursor to left child | -1 pt |
| D | Move cursor to right child | -1 pt |
| T | Place symbol from backpack into cursor slot | — |
| R | Remove symbol at cursor → backpack | -2 pts |
| F | Finalize tree | -10 pts on error |

**Finalization (`F`) Requirements**
- Minimum **3 variables** in the tree.
- Minimum **depth of 3**.
- On success: displays infix and postfix expression, advances to Table Screen.
- On error: -10 penalty, stays on Tree Screen.

**Tree Score**
```
Tree Score = 10 × Number of Tree Nodes
```

**Node Numbering**

```
                --------------1--------------
               /                             \
        ------2------                   ------3------
       /             \                 /             \
    --4--           --5--           --6--           --7--
   /     \         /     \         /     \         /     \
  8       9       0       1       2       3       4       5
```

**Example**

```
        ------~------                   ------+------
       /                               /             \
    --v--                             A               C
   /     \
  a       B

Infix   :  ((~(a v B)) > (A + C))
Postfix :  a B v ~ A C + >
```

---

### 3. Table Screen

Switch with **Key 3** (only after tree is finalized).

- Full **truth table** is computed and displayed.
- Each column has one **random hidden cell (yellow)**. Correct answer = **+3 pts**, wrong = **-2 pts**.
- After the table, the **Karnaugh Map** is shown.
- Player inputs the **simplified Boolean expression** (yellow field).
  - Correct: +Tree Score points.

**Example Truth Table**

```
ABCD | avB | ~(avB) | A+C | (~(avB))>(A+C)
-------------------------------------------
0000 |  1  |    0   |  0  |       1
0001 |  1  |    0   |  0  |       1
...
1010 |  0  |    1   |  0  |       0
1011 |  0  |    1   |  0  |       0
...
```

**Example Karnaugh Map**

```
      CD
      00    01    11    10
 AB  +---------+-----------+
 00  | 1     1 |   1     1 |
     +---------|-----------+
 01  | 1     1 |   1     1 |
     +---------|-----------+
 11  | 1     1 |   1     1 |
     +---------|-----------+
 10  | 1     1 |   0     0
     +---------+

Simplified: A' + B + C'
```

---

## Controls

| Key | Screen | Action |
|---|---|---|
| Arrow Keys | Maze | Move player |
| Space | Maze | Fire fireball |
| M | Maze | Toggle storage mode (Tree / Backpack) |
| 1 | Any | Switch to Maze Screen |
| 2 | Any | Switch to Tree Screen |
| 3 | Any | Switch to Table Screen |
| W | Tree | Move cursor to parent |
| A | Tree | Move cursor to left child |
| D | Tree | Move cursor to right child |
| T | Tree | Place symbol from backpack into tree |
| R | Tree | Remove symbol from tree to backpack |
| F | Tree | Finalize tree |

---

## Game Elements & Symbols

| Symbol | Maze Char | Meaning |
|---|---|---|
| A, B, C, D | A, B, C, D | Boolean variables |
| A', B', C', D' | a, b, c, d | Negated variables |
| ¬ | ~ | NOT |
| ∧ | ^ | AND |
| ∨ | v | OR |
| ⊕ | + | XOR |
| → | > | IMPLIES |
| ↔ | = | IFF |
| — | `@` | Packed fireball |
| — | `o` | Active fireball |
| — | `P` | Human player |
| — | `X` | Enemy robot |
| — | `#` | Wall |

---

## Scoring

| Event | Points |
|---|---|
| Destroy a robot with fireball | +50 |
| Tree finalization (per node) | +10 |
| Correct truth table cell | +3 |
| Wrong truth table cell | -2 |
| Correct Karnaugh simplification | +Tree Score |
| Tree cursor move (W/A/D) | -1 per move |
| Remove symbol from tree (R) | -2 |
| Invalid tree finalization (F) | -10 |

---

## File Formats

### `maze.txt`

```
#############################################
#                                           #
#   ####                                    #
#                                           #
#############################################
```
- `#` = wall, ` ` = open passage.
- Player `P` is placed at a random empty cell at game start.
- All other game elements are placed dynamically via the input queue.

### `highscore.txt`

One entry per line, loaded into a doubly linked list and sorted descending:

```
Tarkan Bulut    728
Irmak Yol       412
Deniz Toprak    190
Ali Deniz        56
```

---

## Data Structures

| Structure | Class | Usage |
|---|---|---|
| 2D `char[][]` array (21×45) | `Maze` | Game grid |
| Circular queue (size 10) | `InputQueue` | Element feed |
| Array (size 8) | `Player` | Backpack |
| Binary tree | `ExpressionTree` | Symbol tree |
| Doubly linked list | `HighScoreList` | High score table |

---

## Workload Distribution

| Week | Goal | Status |
|---|---|---|
| 1 | Class design, Enigma setup, file loading, screen skeleton | ✅ Done |
| 2 | Player movement, fireball, timing, backpack | 🔄 In progress |
| 3 | Input queue, robot AI, neighbor-square damage | 🔄 In progress |
| **4** | **Tree Screen, infix/postfix expressions** | ⬜ Todo |
| 5 | Table Screen, Karnaugh map simplification | ⬜ Todo |
| 6 | High score table (doubly linked list) | ⬜ Todo |
| 7 | Remaining parts, testing, debugging | ⬜ Todo |

**Legend:** ⬜ Todo · 🔄 In progress · ✅ Done · ❌ Blocked

**First Evaluation: 04.05.2026** — Weeks 1–4 complete + report
**Final Evaluation: 22.05.2026** — Full game + report + PowerPoint + video
| Member | Responsibilities |
|---|---|
| **Efe** | Screen switching infrastructure, Tree Screen, screen-level movement |
| **Derya** | Table Screen, Karnaugh map setup |
| **Rüzgar** | Backpack (global), screen rendering, Player (movement, pick-up, health), right-side stats display |
| **Buğra** | Menu, Highscore table, Maze, Fireball, Robot, death mechanic, F key (tree finalization) |

---


## Changelog

| Version | Date       | Notes                                                                               |
|---------|------------|-------------------------------------------------------------------------------------|
| v0.1    | 2026-04-06 | Initial README, project setup                                                       |
| v1      | 2026-04-?? | I don't remember                                                                    |
| v2      | 2026-04-12 | Backpack, player and its movements, picking up elements added<br/> Timer is tweaked |
| v3      | 2026-04-19 | Logic Tree, simultaneous working screens-movement amongst them                      | 
---
