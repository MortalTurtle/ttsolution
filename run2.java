import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


public class run2 {


    private static final char[] KEYS_CHAR = new char[26];
    private static final char[] DOORS_CHAR = new char[26];
    private static final Map<Character, Character> KEY_TO_DOOR;
    private static final Map<Character, Character> DOOR_TO_KEY;

    static {
        KEY_TO_DOOR = new HashMap<>();
        DOOR_TO_KEY = new HashMap<>();
        for (int i = 0; i < 26; i++) {
            KEYS_CHAR[i] = (char)('a' + i);
            DOORS_CHAR[i] = (char)('A' + i);
            KEY_TO_DOOR.put(KEYS_CHAR[i], DOORS_CHAR[i]);
            DOOR_TO_KEY.put(DOORS_CHAR[i], KEYS_CHAR[i]);
        }
    }

    private static enum CellType {
        Wall, 
        Robot,
        Passage,
        Key,
        Door
    }
    private static class MazeSolver {
        private static class Point {
            public int row;
            public int col;
            public Point(int row,int col) {
                this.row = row;
                this.col = col;
            }

            @Override
            public int hashCode(){
                return (row << 6) + col;
            }


            @Override
            public boolean equals(Object obj){
                if (obj.getClass() != this.getClass())
                    return false;
                Point p = (Point)obj;
                return row == p.row && col == p.col;
            }
        }

        private static class Node {
            public Point p;
            public List<Node> next = new ArrayList<>();
            public Map<Point, Integer> weight = new HashMap<>();
            public CellType type;
            public Optional<Character> ch = Optional.empty();

            public Node(Point p, CellType t, Character node_char) {
                this.p = p;
                type = t;
                if (node_char != null)
                    this.ch = Optional.of(node_char);
            }

            public Node(Point p, CellType t){
                this.p = p;
                type = t;
            }
        }

        private static class AccessibleKey {
            Node node;
            public Integer stepsToGet;

            public AccessibleKey(Node n, int steps) {
                node = n;
                this.stepsToGet = steps;
            }
        }

        private final int WIDTH;
        private final int HEIGHT; 
        private final CellType[][] mazeByTypes;
        private final char[][] maze;
        private List<Point> robots = new ArrayList<>();
        private final Map<Point, Character> keys = new HashMap<>();
        private final Map<Point, Character> doors = new HashMap<>();
        private static final Map<Character, CellType> CHAR_TO_CELLTYPE;
        private static final List<Point> delta = 
            Arrays.asList(
                new Point(1, 0),
                new Point(0, 1),
                new Point(-1, 0),
                new Point(0, -1)
            );
        static {
            CHAR_TO_CELLTYPE = new HashMap<>();
            for (char key : KEYS_CHAR) CHAR_TO_CELLTYPE.put(key, CellType.Key);
            for (char door : DOORS_CHAR) CHAR_TO_CELLTYPE.put(door, CellType.Door);
            CHAR_TO_CELLTYPE.put('#', CellType.Wall);
            CHAR_TO_CELLTYPE.put('.', CellType.Passage);
            CHAR_TO_CELLTYPE.put('@', CellType.Robot);
        }

        public MazeSolver(char[][] maze) {
            this.maze = maze;
            HEIGHT = maze.length;
            mazeByTypes = new CellType[HEIGHT][];
            if (HEIGHT > 0) WIDTH = maze[0].length;
            else WIDTH = 0;
            for (int i = 0; i < HEIGHT; i++) {
                mazeByTypes[i] = new CellType[WIDTH];
                for (int j = 0; j < WIDTH; j++) {
                    mazeByTypes[i][j] = CHAR_TO_CELLTYPE.get(maze[i][j]);
                    if (mazeByTypes[i][j] == CellType.Robot)
                        robots.add(new Point(i,j));
                    if (mazeByTypes[i][j] == CellType.Key)
                        keys.put(new Point(i,j), maze[i][j]);
                    if (mazeByTypes[i][j] == CellType.Door)
                        doors.put(new Point(i,j), maze[i][j]);
                }
            }
        }

        private Map<Point, Node> checkedPoints = new HashMap<>();
        private void findAllAccessibleNodes(Node start) {
            Queue<Point> q = new LinkedList<>();
            Set<Point> visited = new HashSet<>();
            Map<Point, Integer> weight = new HashMap<>();
            weight.put(start.p, 0);
            visited.add(start.p);
            q.add(start.p);
            while(!q.isEmpty()) {
                var p = q.poll();
                for (var d : delta) {
                    var np = new Point(p.row + d.row, p.col + d.col);
                    if (np.row < 0 || np.row >= HEIGHT ||
                        np.col < 0 || np.col >= WIDTH ||
                        mazeByTypes[np.row][np.col] == CellType.Wall ||
                        visited.contains(np))
                        continue;
                    visited.add(np);
                    weight.put(np, weight.get(p) + 1);
                    if (doors.containsKey(np) || keys.containsKey(np)) {
                        start.weight.put(np, weight.get(np));
                        if (!checkedPoints.containsKey(np)) {
                            var node = new Node(np, mazeByTypes[np.row][np.col], maze[np.row][np.col]);
                            start.next.add(node);
                            checkedPoints.put(np, node);
                            findAllAccessibleNodes(node);
                        } else start.next.add(checkedPoints.get(np));
                    } else q.add(np);
                }
            }
        }

        private List<AccessibleKey> findAllAccessibleKeys(
            Node start,
            Set<Character> keysCollected) {
            List<AccessibleKey> keysAccessible = new ArrayList<>();
            Queue<Node> q = new LinkedList<>();
            q.add(start);
            Set<Point> visited = new HashSet<>();
            visited.add(start.p);
            Map<Point, Integer> weight = new HashMap<>();
            weight.put(start.p, 0);
            while(!q.isEmpty()) {
                var cur = q.poll();
                for (var next : cur.next) {
                    if (visited.contains(next.p) ||
                        next.type == CellType.Door &&
                        !keysCollected.contains(DOOR_TO_KEY.get(next.ch.get())))
                        continue;
                    visited.add(next.p);
                    weight.put(next.p, weight.get(cur.p) + cur.weight.get(next.p));
                    q.add(next);
                    if (next.type == CellType.Key && !keysCollected.contains(next.ch.get()))
                        keysAccessible.add(new AccessibleKey(next, weight.get(next.p)));
                }
            }
            return keysAccessible;
        }

        private int findMinSteps(List<Node> robotPositions, int curSteps, Set<Character> collectedKeys) {
            if (collectedKeys.size() == this.keys.size())
                return curSteps;
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < robotPositions.size(); i++) {
                var accessibleKeys = findAllAccessibleKeys(robotPositions.get(i), collectedKeys);
                for (var accessibleKey : accessibleKeys) {
                    collectedKeys.add(accessibleKey.node.ch.get());
                    curSteps += accessibleKey.stepsToGet;
                    var t = robotPositions.get(i);
                    robotPositions.set(i, accessibleKey.node);
                    min = Math.min(min, findMinSteps(robotPositions, curSteps, collectedKeys));
                    robotPositions.set(i, t);
                    curSteps -= accessibleKey.stepsToGet;
                    collectedKeys.remove(accessibleKey.node.ch.get());
                }
            }
            return min;
        }

        public int Solve() {
            List<Node> start = new ArrayList<>();
            for(var p : robots)
                start.add(new Node(p, CellType.Robot));
            for (var node: start) 
                findAllAccessibleNodes(node);
            return findMinSteps(start, 0, new HashSet<>());
        }
    }

    // Чтение данных из стандартного ввода
    private static char[][] getInput() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        List<String> lines = new ArrayList<>();
        String line;


        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            lines.add(line);
        }


        char[][] maze = new char[lines.size()][];
        for (int i = 0; i < lines.size(); i++) {
            maze[i] = lines.get(i).toCharArray();
        }


        return maze;
    }


    private static int solve(char[][] data) {
        MazeSolver solver = new MazeSolver(data);
        return solver.Solve();
    }
    
    public static void main(String[] args) throws IOException {
        char[][] data = getInput();
        int result = solve(data);
        
        if (result == Integer.MAX_VALUE) {
            System.out.println("No solution found");
        } else {
            System.out.println(result);
        }
    }
}