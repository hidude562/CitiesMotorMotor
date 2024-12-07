import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;

/*
    -- CityMotorMotor --
    This is a city simulation game that attempts to be as realistic as possible.
    Like dwarf fortress perhaps.

    It is tile based, and the idea for movement is that each tile can have 1 or more rules,
    So like a road that goes up would check if the player orientation is 90, then push forward
    Also, on the same tile, you can have a rotater that flip flops the orientation between 0 and 90 degrees for example
    The pathfinder forks when there are 2 rules

    For intersections, i haven't massively thought this out yet but something would need to trigger the rest of the
    filled in intersection with a value or something? This would probably need a different kind of rule because it will
    always be applied

    This is me thinking about architecture for CityMotorMotor
    This shall be ported to godot soonish
 */

/*
    Vector 2 class for godot port
 */
class Vector2 {
    public double x;
    public double y;
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

/*
    Data stored for each tile
    Don't instantiate, get a Tiles.Tile from the Tiles class using get()
    Tiles.Tile is basically a pointer to this but with extra functionality
 */
class TileData {
    ArrayList<Rule> rules;
    ArrayList<MoveableObject> npcs;

    public TileData() {
        rules = new ArrayList<Rule>();
        npcs = new ArrayList<MoveableObject>();
        rules.add(new RuleRoadUp());
        rules.add(new RuleRoadLeft());
        rules.add(new RuleRoadRight());
        rules.add(new RuleRoadDown());
        rules.add(new RuleRoadRotate());
    }

    // TODO: If the npc size fits within the npcs, return true, else false
    public boolean canMove(NPC npc) {
        return true;
    }

    public boolean hasRule(Rule rule) {
        for (Rule r : rules) {
            if (r.getClass() == rule.getClass()) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Rule> getRules() {
        return rules;
    }

    public void setRules(ArrayList<Rule> rules) {
        this.rules = rules;
    }

    public ArrayList<MoveableObject> getNpcs() {
        return npcs;
    }

    public void setNpcs(ArrayList<MoveableObject> npcs) {
        this.npcs = npcs;
    }

    public String toString() {
        return String.valueOf(npcs.size());
    }
}


/*
    The tileset. Eventually, this will be 3D. Get individual tiles with get
 */
class Tiles {
    TileData[][] tiles;

    public Tiles(int x, int y) {
        tiles = new TileData[y][x];
        for(int i = 0; i < tiles.length; i++) {
            for(int j = 0; j < tiles[i].length; j++) {
                tiles[i][j] = new TileData();
            }
        }
    }

    public boolean inRange(int x, int y) {
        return (x >= 0 && x < tiles[0].length) && (y >= 0 && y < tiles.length);
    }

    public Tile get(int x, int y) {
        if(!inRange(x,y)) return null;
        return new Tile(x, y);
    }

    public String toString() {
        String b = "";
        for(int i = 0; i < tiles.length; i++) {
            for(int j = 0; j < tiles[i].length; j++) {
                b+=tiles[i][j].toString();
            }
            b+="\n";
        }
        return b;
    }

    class Tile {
        protected int y;
        protected int x;

        public Tile(int x, int y) {
            this.y = y;
            this.x = x;
        }

        public TileData get() {
            return tiles[y][x];
        }

        public Tile getRelative(int x, int y) {
            return new Tile(this.x+x, this.y+y);
        }

        public Tile[] getNeighbors() {
            Tile[] tiles = new Tile[4];
            for(int i = 0; i < 4; i++) {
                Tile tile = new Tile(i%2*2-1+x, (i+1)%2*2-1+y);
            }
            return tiles;
        }

        public String toString() {
            return get().toString();
        }
    }
}

/*
    Credit to Anthropic's Sonnet for implementation.

    This is basically the google maps for your npc, it tells you where to make certain direction choices and allat based off where you wish to go
 */

class Navigation {
    private MoveableObject moveableObject;
    protected ArrayList<Integer> navigation;
    private ArrayList<Tiles.Tile> navigationTargets;

    // For pathfinding
    private class Node implements Comparable<Node> {
        Tiles.Tile tile;
        int orientation;
        Node parent;
        int ruleIndex; // Which rule got us here
        double g; // Cost from start
        double h; // Heuristic to goal

        public Node(Tiles.Tile tile, int orientation, Node parent, int ruleIndex, double g, double h) {
            this.tile = tile;
            this.orientation = orientation;
            this.parent = parent;
            this.ruleIndex = ruleIndex;
            this.g = g;
            this.h = h;
        }

        public double f() {
            return g + h;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f(), other.f());
        }
    }

    public Navigation(MoveableObject moveableObject) {
        this.navigation = new ArrayList<Integer>();
        this.navigationTargets = new ArrayList<Tiles.Tile>();
        this.moveableObject = moveableObject;
    }

    private double heuristic(Tiles.Tile from, Tiles.Tile to) {
        // Manhattan distance
        return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
    }

    public void recalculateNavigation() {
        if (navigationTargets.isEmpty()) return;

        navigation.clear();
        Tiles.Tile target = navigationTargets.get(0);

        // Priority queue for A*
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        HashSet<String> closedSet = new HashSet<>(); // Using string key for tile+orientation

        // Start node
        Node start = new Node(
                moveableObject.getTile(),
                moveableObject.getOrientation(),
                null,
                -1,
                0,
                heuristic(moveableObject.getTile(), target)
        );

        openSet.add(start);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // Create unique key for this state
            String stateKey = current.tile.x + "," + current.tile.y + "," + current.orientation;

            if (closedSet.contains(stateKey)) continue;
            closedSet.add(stateKey);

            // Check if we reached the target
            if (current.tile.get().equals(target.get())) {
                // Reconstruct path
                ArrayList<Integer> path = new ArrayList<>();
                Node node = current;
                while (node.parent != null) {
                    path.add(0, node.ruleIndex);
                    node = node.parent;
                }
                navigation.addAll(path);
                return;
            }

            // Try all rules at current tile
            ArrayList<Rule> rules = current.tile.get().getRules();
            for (int i = 0; i < rules.size(); i++) {
                Rule rule = rules.get(i);

                // Create a temporary moveable object to test the rule
                MoveableObject tempObject = new MoveableObject(
                        current.orientation,
                        moveableObject.getSize(),
                        current.tile
                );

                // If rule can be applied
                if (rule.canApply(tempObject)) {
                    // Apply rule to get new state
                    rule.apply(tempObject);

                    // Create new node
                    // Create new node
                    Node neighbor = new Node(
                            tempObject.getTile(),
                            tempObject.getOrientation(),
                            current,
                            i,
                            current.g + 1, // Assume cost of 1 for each move
                            heuristic(tempObject.getTile(), target)
                    );

                    openSet.add(neighbor);
                }

                tempObject.tile.get().getNpcs().remove(tempObject);
            }
        }
    }

    public void addTarget(Tiles.Tile tile) {
        navigationTargets.add(tile);
        recalculateNavigation();
    }

    public int next() {
        if (navigation.isEmpty()) {
            recalculateNavigation();
            if (navigation.isEmpty()) return -1; // No path found
        }

        int destinationReached = navigationTargets.indexOf(moveableObject.getTile());
        if (destinationReached != -1) {
            navigationTargets.remove(destinationReached);
            recalculateNavigation();
            if (navigation.isEmpty()) return -1;
        }

        return navigation.remove(0);
    }
}

class MoveableObject {
    // 0 -> Right, 90 -> Up, 180 -> Left, 270 -> down
    int orientation;
    Vector2 size;
    Tiles.Tile tile;

    Navigation navigation;

    ArrayList<MoveableObject> carrying;
    MoveableObject parent;

    /*
        Types it can immitate:
            0 - General people navigation (sidewalks, parks, etc.)
            1 - General Vehicle navigation (roads)
     */
    ArrayList<Integer> travelTypes;

    public MoveableObject(int orientation, Vector2 size, Tiles.Tile tile) {
        this.orientation = orientation;
        this.size = size;
        this.tile = tile;
        this.navigation = new Navigation(this);
        this.travelTypes = new ArrayList<Integer>();
        this.travelTypes.add(0);

        this.tile.get().getNpcs().add(this);
    }

    public void navigate() {
        int choice = navigation.next();
        this.tile.get().getRules().get(choice).apply(this);
    }

    public void move(int x, int y) {
        this.tile.get().getNpcs().remove(this);
        this.tile = this.tile.getRelative(x,y);
        this.tile.get().getNpcs().add(this);


    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public Vector2 getSize() {
        return size;
    }

    public void setSize(Vector2 size) {
        this.size = size;
    }

    public Tiles.Tile getTile() {
        return tile;
    }

    public void setTile(Tiles.Tile tile) {
        this.tile = tile;
    }
}

/*
     NPC that can move, maybe extendable to more applications?
 */
class NPC extends MoveableObject {

    public NPC(int orientation, Vector2 size, Tiles.Tile tile) {
        super(orientation, size, tile);
    }
}

/*
    Rule for a tile. Like 'if player rotation == 0 and nlah blaah blah, move player up 2
 */

abstract class Rule {
    private int applyType = 0;

    abstract public void apply(MoveableObject npc);

    public boolean canApply(MoveableObject npc) {
        if(npc.travelTypes.contains(applyType)) {
            if(baseRule(npc)) {
                return true;
            }
        }
        return false;
    }
    abstract public boolean baseRule(MoveableObject npc);
}

// Idk? For tiles itself. Like deciding
// TODO:
abstract class TileRule {
    abstract public void apply(Tiles.Tile tile);
}

// TODO: static?
class RuleRoadUp extends Rule {
    public void apply(MoveableObject npc) {
        npc.move(0, -1);
    }

    public boolean baseRule(MoveableObject npc) {
        System.out.println(npc.getOrientation());
        if(npc.getOrientation() == 90 && npc.getTile().get().hasRule(this)) {
            return true;
        }
        return false;
    }
}

class RuleRoadRight extends Rule {
    public void apply(MoveableObject npc) {
        npc.move(1, 0);
    }

    public boolean baseRule(MoveableObject npc) {
        if(npc.getOrientation() == 0 && npc.getTile().get().hasRule(this)) {
            return true;
        }
        return false;
    }
}

class RuleRoadDown extends Rule {
    public void apply(MoveableObject npc) {
        npc.move(0, 1);
    }

    public boolean baseRule(MoveableObject npc) {
        if(npc.getOrientation() == 270 && npc.getTile().get().hasRule(this)) {
            return true;
        }
        return false;
    }
}

class RuleRoadLeft extends Rule {
    public void apply(MoveableObject npc) {
        npc.move(-1, 0);
    }

    public boolean baseRule(MoveableObject npc) {
        if(npc.getOrientation() == 180 && npc.getTile().get().hasRule(this)) {
            return true;
        }
        return false;
    }
}

class RuleRoadRotate extends Rule {
    public void apply(MoveableObject npc) {
        npc.setOrientation((npc.getOrientation() + 90) % 360);
    }

    public boolean baseRule(MoveableObject npc) {
        if(       npc.getTile().get().hasRule(this)) {
            return true;
        }
        return false;
    }
}

public class Main {
    public static void main(String[] args) {
        Tiles tiles = new Tiles(32, 32);
        NPC npc = new NPC(0, new Vector2(0.1, 0.1), tiles.get(10,10));
        npc.navigation.addTarget(tiles.get(5, 5));
        System.out.println(npc.navigation.navigation);
        for(int i = 0; i < 5; i++) {npc.navigate(); System.out.println(tiles);}

    }
}