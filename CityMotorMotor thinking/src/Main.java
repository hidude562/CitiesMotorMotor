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
    Vector 2 class for godot port for easier rewriting
 */
class Vector2 {
    public double x;
    public double y;
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Vector2 rotate(Vector2 vec, double deg) {
        // Convert degrees to radians
        double rad = Math.toRadians(deg);

        // Rotation matrix multiplication:
        // x' = x * cos(θ) - y * sin(θ)
        // y' = x * sin(θ) + y * cos(θ)
        double newX = vec.x * Math.cos(rad) - vec.y * Math.sin(rad);
        double newY = vec.x * Math.sin(rad) + vec.y * Math.cos(rad);

        return new Vector2(newX, newY);
    }

    public String toString() {
        return "("+x+", "+y+")";
    }
}

/*
    Data stored for each tile
    Don't instantiate, get a Tiles.Tile from the Tiles class using get()
    Tiles.Tile is basically a pointer to this but with extra functionality
 */
class TileData {
    private ArrayList<Rule> rules;
    private ArrayList<MoveableObject> npcs;
    private ArrayList<Area> areas;

    public TileData() {
        rules = new ArrayList<Rule>();
        npcs = new ArrayList<MoveableObject>();
        areas = new ArrayList<Area>();

        rules.add(new RuleRoadUp());
        rules.add(new RuleRoadLeft());
        rules.add(new RuleRoadRight());
        rules.add(new RuleRoadDown());
        rules.add(new RuleRoadRotate());
    }

    public Vector2 getOccupiedSpace(MoveableObject exclude) {
        Vector2 size = new Vector2(0, 0);
        for(MoveableObject moveableObject : npcs) {
            if(moveableObject != exclude) {
                size.x += Math.min(1, moveableObject.getSize().x);
                size.y += Math.min(1, moveableObject.getSize().y);
            }
        }
        return size;
    }

    public boolean isCompletelyFull() {
        Vector2 space = getOccupiedSpace(null);
        if(space.x >= 1 && space.y >= 1) {
            return true;
        }
        return false;
    }

    /*
     If the npc size fits within the npcs, return true, else false
     */
    boolean canMove(MoveableObject object) {
        Vector2 actualSize = new Vector2(Math.min(1, object.getSize().x), Math.min(1, object.getSize().y));
        Vector2 occupied = getOccupiedSpace(object);
        if(1 - (actualSize.x + occupied.x) >= 0 && 1 - (actualSize.y + occupied.y) >= 0) {
            return true;
        }
        return false;
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

    public ArrayList<MoveableObject> getMovableObjects() {
        return npcs;
    }

    public ArrayList<Area> getAreas() {
        return areas;
    }

    public void addArea(Area area) {
        if(!areas.contains(area)) {
            areas.add(area);
        }
    }

    public String toString() {
        return String.valueOf(npcs.size());
    }
}


/*
    The tileset. Eventually, this will be 3D once the game reaches decent completion in 2D Get individual tiles with get
 */
class Tiles {
    private TileData[][] tiles;

    public Tiles(int x, int y) {
        tiles = new TileData[y][x];
        for(int i = 0; i < tiles.length; i++) {
            for(int j = 0; j < tiles[i].length; j++) {
                tiles[i][j] = new TileData();
            }
        }
    }

    public int getWidth() {
        return tiles[0].length;
    }

    public int getHeight() {
        return tiles.length;
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
                b+=tiles[i][j].toString() + " ";
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

        public boolean inRange() {
            return Tiles.this.inRange(x, y);
        }

        public Tiles getTileset() {
            return Tiles.this;
        }

        public TileData get() {
            return tiles[y][x];
        }

        public Tile getRelative(int x, int y) {
            return new Tile(this.x+x, this.y+y);
        }

        public Tile getRelative(Vector2 pos) {
            return new Tile(
                    this.x+((int) pos.x),
                    this.y+((int) pos.y)
            );
        }

        // NOTE, Up down left right, not diagonals
        public Tile[] getNeighbors() {
            Tile[] tiles = new Tile[4];
            for(int i = 0; i < 4; i++) {
                Tile tile = new Tile(i%2*2-1+x, (i+1)%2*2-1+y);
            }
            return tiles;
        }

        public Tile[] getTilesExactlyInRange(int range) {
            // Do fancy math to get a spiral for it basically
            int distToCover = (range*2*4);
            Tile[] tiles = new Tile[distToCover];
            for (int i = 0; i < distToCover; i++) {
                int dx = (i%2*2-1);
                int dy = ((i/2)%2*2-1);
                Vector2 direction = new Vector2(
                        dx * range + (i/4 * -dx * ((i+1)%2)),
                        dy * range + (i/4 * -dy * (i%2))
                );
                Tile tile = getRelative(direction);
                tiles[i] = tile;
            }

            return tiles;
        }


        public String toString() {
            return get().toString();
        }

        public boolean equals(Tiles.Tile other) {
            return this.x == other.x && this.y == other.y;
        }
    }
}

/*
    Credit to Anthropic's Sonnet for implementation of calculating navigation.

    This is basically the google maps for your npc, it tells you where to make certain direction choices and allat based off where you wish to go
 */

class Navigation {
    private MoveableObject moveableObject;
    private ArrayList<Integer> navigation;
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

                tempObject.getTile().get().getMovableObjects().remove(tempObject);
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

    public MoveableObject getMoveableObject() {
        return moveableObject;
    }

    public void setMoveableObject(MoveableObject moveableObject) {
        this.moveableObject = moveableObject;
    }

    public ArrayList<Integer> getNavigation() {
        return navigation;
    }

    public void setNavigation(ArrayList<Integer> navigation) {
        this.navigation = navigation;
    }

    public ArrayList<Tiles.Tile> getNavigationTargets() {
        return navigationTargets;
    }

    public void setNavigationTargets(ArrayList<Tiles.Tile> navigationTargets) {
        this.navigationTargets = navigationTargets;
    }
}


/*
    Defines a retrievable Area by tiles an such
 */
class Area {
    ArrayList<Tiles.Tile> tiles;
    public Area() {
        tiles = new ArrayList<Tiles.Tile>();
    }

    public void addTile(Tiles.Tile tile) {
        if(!tiles.contains(tile)) {
            tiles.add(tile);
            tile.get().addArea(this);
        }
    }

    public void addTiles(ArrayList<Tiles.Tile> tiles) {
        for(Tiles.Tile tile : tiles) {
            addTile(tile);
        }
    }

    public boolean has(Tiles.Tile tile) {
        return tile.get().getAreas().contains(this);
    }

    /*
        Find MoveableObject base class by inherited class
    */
    public MoveableObject findClass(MoveableObject search) {
        for (Tiles.Tile tile : tiles) {
            for(MoveableObject moveableObject : tile.get().getMovableObjects()) {
                // TODO: Recursively search children too
                if(moveableObject.getClass() == search.getClass()) {
                    return moveableObject;
                }
            }
        }
        return null;
    }

    public ArrayList<Tiles.Tile> getTiles() {
        return tiles;
    }

    protected Tiles.Tile randomTile() {
        return tiles.get((int) (tiles.size() * Math.random()));
    }
}

/*
    This defines a ruled area for managing npcs that work in this area

    So, for an area for a cashier, there would be a rule to navigate the workers to the cash registers
    Or for restocking, find what needs to be restocked, then send over the navigation for that too
 */
abstract class WorkableArea extends Area {
    // TODO:

    private ArrayList<NPC> allowed;

    public WorkableArea() {
        super();
    }

    abstract void manage();
}

/*
    As opposed to workable area where it controls the npc,
    a CommonArea provides simplified methods for doing certain things,
    For example, a general store may have a list of items that can be bought
    and the user can just call something as simple as buy([skibidi_rizzler, blah_blah_blah])
    (which would set the npc navigation to get the items, pay and such)

    The idea is that you can technically do this manually setting the navigation,
    but this is basically a kinda facade for that.

    Each room of a building that has use (Including houses and such) should inherit this
    What shouldn't inherit this are like streets and hallways
 */
class CommonArea extends Area  {
    // TODO:
    public CommonArea() {
        super();
    }
}

class ImmediateStoreArea extends CommonArea {
    public ImmediateStoreArea() {
        super();
    }
    public boolean buy(MoveableObject moveableObject) {
        MoveableObject found = findClass(moveableObject);
        if (found == null) return false;

        // TODO:
        return false;
    }
    public void getObject(MoveableObject moveableObject) {

    }
}

/*
    Wrapper class for actionable that places it on the given tile
    You probably want to use this in ActionablesTransmitter
    as it is useless on its own.

    Relative position is for orientation of 0,
    This notably changes with the orientation when used by ActionablesTransmitter
 */
class DynamicActionable {
    private Actionable     actionable;
    private Vector2        relativePosition;
    private Tiles.Tile     tile;

    public DynamicActionable(Actionable actionable, Vector2 relativePosition) {
        this.actionable = actionable;
        this.relativePosition = relativePosition;
    }

    public void onChange(MoveableObject source) {
        // TODO: Actually rotate
        Vector2 rotatedPosition = relativePosition;
        Tiles.Tile tileToTransferTo = source.getTile().getRelative((int) rotatedPosition.x,(int) rotatedPosition.y);

        // Remove from original tile
        tile.get().getRules().remove(actionable);

        // Set tile to anew
        tile = tileToTransferTo;

        // Push
        tile.get().getRules().add(actionable);
    }

    public Actionable getActionable() {
        return actionable;
    }

    public Vector2 getRelativePosition() {
        return relativePosition;
    }

    public Tiles.Tile getTile() {
        return tile;
    }
}


/*
    Pass in a MoveableObject to the constructor and it transmits Actionables to the appropriate neighbor tiles
*/
class ActionablesTransmitter {
    private MoveableObject source;
    private ArrayList<DynamicActionable> dynamicActionables;

    public ActionablesTransmitter(MoveableObject source, ArrayList<DynamicActionable> dynamicActionables) {
        this.source = source;
        this.dynamicActionables = dynamicActionables;
    }

    /*
        Call this when change happens in the source that makes the
        transmititions change, like movement or orientation
     */
    public void onChange() {
        for(DynamicActionable dynamicActionable : dynamicActionables) {
            dynamicActionable.onChange(source);
        }
    }

    public MoveableObject getSource() {
        return source;
    }

    public ArrayList<DynamicActionable> getDynamicActionables() {
        return dynamicActionables;
    }
}


/*
    This class is used for basically all items, from people (NPC extends this), equipment, vehicles, money, etc.

    What this isn't used for are the ground tiles (Roads, sidewalks, floors).

    Like Tiles, these have an extension of rules you can use for things to transmit

    For example, for a cash register, there are two actionables placed where the customer and worker is. The customer is able to run the actionable if the worker is there, where a navigation path is transmitted and sent back

    I'm thinking about having a class this extends, perhaps AbstractObject that is this, but minus the navigation and tiles
*/
class MoveableObject {
    /*
        Name, for just displaying to user, not used for identification or anything
     */
    private String name;

    // 0 -> Right, 90 -> Up, 180 -> Left, 270 -> down
    private int orientation;

    /*
        Size (not implemented):
        How many tiles (or partial tiles) the object occupies
     */
    private Vector2 size;

    /*
        The tile that the object is on, or, in navigation with vehicles, the exit point
     */
    private Tiles.Tile tile;

    /*
        Navigation, navigate to anything using this
     */
    private Navigation navigation;

    /*
        Carrying,
     */
    private ArrayList<MoveableObject> carrying;

    private MoveableObject parent;

    /*
        Intended for currency and allat
    */
    private int numOf;

    /*
        If same parent or tile, whether or not it is allowed to merge the two. Intended for items like pens or money

        TODO: Implement split method
     */
    private boolean merge;

    /*
        Types it can immitate:
            0 - General people navigation (sidewalks, parks, etc.)
            1 - General Vehicle navigation (roads)
     */
    private ArrayList<Integer> travelTypes;

    /*
        Used for doing actions for other things:
        Example, grabbing item
     */
    private ActionablesTransmitter actionablesTransmitter;

    public MoveableObject() {
        this.name = "Unknown";
        this.orientation = 0;
        this.size = new Vector2(1, 1);
        this.tile = null;
        this.navigation = new Navigation(this);
        this.carrying = new ArrayList<MoveableObject>();
        this.travelTypes = new ArrayList<Integer>();
        this.travelTypes.add(0);
        this.numOf = 1;
        this.merge = false;
        this.actionablesTransmitter = new ActionablesTransmitter(
                this,
                new ArrayList<DynamicActionable>()
        );

        if(this.tile != null)
            this.tile.get().getMovableObjects().add(this);
    }

    public MoveableObject(int orientation, Vector2 size, Tiles.Tile tile) {
        this.name = "Unknown";
        this.orientation = orientation;
        this.size = size;
        this.tile = tile;
        this.navigation = new Navigation(this);
        this.carrying = new ArrayList<MoveableObject>();
        this.travelTypes = new ArrayList<Integer>();
        this.travelTypes.add(0);
        this.numOf = 1;
        this.merge = false;
        this.actionablesTransmitter = new ActionablesTransmitter(
                this,
                new ArrayList<DynamicActionable>()
        );

        if(this.tile != null)
            this.tile.get().getMovableObjects().add(this);
    }

    // Use this to do the next action, for internal class use as it takes time to go from tile to tile so it will eventually be private
    public void navigate() {
        int choice = navigation.next();
        this.tile.get().getRules().get(choice).apply(this);
    }

    public void setTile(Tiles.Tile tile) {
        remove();
        this.tile = tile;
        place();
    }

    // For Rule use
    public void move(int x, int y) {
        remove();
        this.tile = this.tile.getRelative(x,y);
        place();
    }

    /*
        Removes object reference(s) in tile
     */
    private void remove() {
        if(this.tile != null) {
            int startRelX = Math.min(0, (int) size.x);
            int startRelY = Math.min(0, (int) size.y);

            int endRelX = Math.max(0, (int) size.x);
            int endRelY = Math.max(0, (int) size.y);

            for (int x = startRelX; x < endRelX; x++) {
                for (int y = startRelY; y < endRelY; y++) {
                    Tiles.Tile relativeTile = this.tile.getRelative(x, y);
                    relativeTile.get().getMovableObjects().remove(this);
                }
            }
        }
    }

    public Tiles.Tile[] getNeigborCornerTiles() {
        int startRelX = Math.min(-1, (int) size.x);
        int startRelY = Math.min(-1, (int) size.y);

        int endRelX = Math.max(1, (int) size.x);
        int endRelY = Math.max(1, (int) size.y);

        Tiles.Tile[] tiles = new Tiles.Tile[] {
            getTile().getRelative(startRelX, startRelY),
            getTile().getRelative(startRelX, endRelY),
            getTile().getRelative(endRelX, startRelY),
            getTile().getRelative(endRelX, endRelY)
        };
        return tiles;
    }

    public boolean willBeInSameArea(Area a, Tiles.Tile tile) {
        int startRelX = Math.min(0, (int) size.x);
        int startRelY = Math.min(0, (int) size.y);

        int endRelX = Math.max(0, (int) size.x);
        int endRelY = Math.max(0, (int) size.y);


        for (int x = startRelX; x < endRelX; x++) {
            for (int y = startRelY; y < endRelY; y++) {
                Tiles.Tile relativeTile = tile.getRelative(x, y);
                if(!relativeTile.get().getAreas().contains(a)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canMoveTo(Tiles.Tile tile) {
        int startRelX = Math.min(0, (int) size.x);
        int startRelY = Math.min(0, (int) size.y);

        int endRelX = Math.max(0, (int) size.x);
        int endRelY = Math.max(0, (int) size.y);


        for (int x = startRelX; x < endRelX; x++) {
            for (int y = startRelY; y < endRelY; y++) {
                Tiles.Tile relativeTile = tile.getRelative(x, y);
                if(relativeTile.inRange() && !relativeTile.get().canMove(this)) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
        Pushes object reference(s) in tile
     */
    private void place() {
        if(this.tile != null) {
            int startRelX = Math.min(0, (int) size.x);
            int startRelY = Math.min(0, (int) size.y);

            int endRelX = Math.max(0, (int) size.x);
            int endRelY = Math.max(0, (int) size.y);

            for (int x = startRelX; x < endRelX; x++) {
                for (int y = startRelY; y < endRelY; y++) {
                    Tiles.Tile relativeTile = this.tile.getRelative(x, y);
                    relativeTile.get().getMovableObjects().add(this);
                }
            }
        }
    }

    // Count the amount of a class in children
    public int count(MoveableObject type) {
        int total = 0;
        for(MoveableObject has : carrying) {
            if(has.getClass() == type.getClass()) {
                total += has.getNumOf();
            }
        }
        return total;
    }

    public ArrayList<MoveableObject> recurseAllChildrenAndSelf() {
        ArrayList<MoveableObject> children = new ArrayList<>();
        children.add(this);
        for(MoveableObject obj : getCarrying()) {
            ArrayList<MoveableObject> childrenChildren = obj.recurseAllChildrenAndSelf();
            for(MoveableObject obj2 : childrenChildren) {
                children.add(obj2);
            }
        }
        return children;
    }

    // Getters / setters
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

    public int getNumOf() {
        return numOf;
    }

    public void setNumOf(int val) {
        this.numOf = val;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Navigation getNavigation() {
        return navigation;
    }

    public void setNavigation(Navigation navigation) {
        this.navigation = navigation;
    }

    public ArrayList<MoveableObject> getCarrying() {
        return carrying;
    }

    public void setCarrying(ArrayList<MoveableObject> carrying) {
        this.carrying = carrying;
    }

    public MoveableObject getParent() {
        return parent;
    }

    public void setParent(MoveableObject parent) {
        if(this.parent != null) {
            this.parent.getCarrying().remove(this);
        }

        this.parent = parent;

        if(this.parent != null) {
            this.parent.getCarrying().add(this);
        }
    }

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    public ArrayList<Integer> getTravelTypes() {
        return travelTypes;
    }

    public void setTravelTypes(ArrayList<Integer> travelTypes) {
        this.travelTypes = travelTypes;
    }

    /*
        TODO: Not fully implemented
    */

    public MoveableObject split(int newAmount) {
        if(newAmount >= this.getNumOf()) {
            try {
                Class<? extends MoveableObject> moveableObject = this.getClass();
                MoveableObject split = moveableObject.newInstance();
                split.setNumOf(newAmount);
                this.setNumOf(this.getNumOf() - newAmount);
                return split;
            } catch(Exception e) {

            }
        }
        return null;
    }

    /*
        Reparents object, removing references
     */
    public void reparent(MoveableObject to) {
        this.parent.carrying.remove(this);
        this.parent = to;
        this.parent.carrying.add(this);
    }

    /*
        Take an amount of object and give it to another movable object
        Returns number transfered
     */
    public int takeAndGive(MoveableObject wants, MoveableObject to) {
        ArrayList<MoveableObject> toTransfer = new ArrayList<>();
        int numHas = 0;
        for(MoveableObject has : recurseAllChildrenAndSelf()) {
            if(has.getClass() == wants.getClass()) {
                toTransfer.add(has);
                // Split if too many of item
                if(numHas + has.getNumOf() > wants.getNumOf()) {
                    MoveableObject partial = this.split(wants.getNumOf() - numHas);
                    toTransfer.add(partial);
                    numHas = wants.getNumOf();

                }

                if(numHas + has.getNumOf() >= wants.getNumOf()) {
                    // Remove from self and add to to
                    for (MoveableObject obj : toTransfer) {
                        obj.reparent(to);
                    }
                    return wants.getNumOf();
                }

                numHas += has.getNumOf();
                toTransfer.add(has);
            }
        }
        return numHas;
    }
}

class Tag extends MoveableObject {
    MoveableObject removeCost;
    public Tag(MoveableObject removeCost) {
        super();
        this.removeCost = removeCost;
    }
}

class Money extends MoveableObject {

}

/*
     An NPC is a MoveableObject that is able to move on its own?
     (That is what i am leaning towards)
 */
class NPC extends MoveableObject {

    public NPC(int orientation, Vector2 size, Tiles.Tile tile) {
        super(orientation, size, tile);
    }

    // If moveable object base class is within 1 tile or same tile, get it
    public boolean grab(MoveableObject grab) {
        Tiles.Tile[] tiles = getTile().getTilesExactlyInRange(1);

        int targetNum = grab.getNumOf();
        int numHas = 0;

        for(Tiles.Tile tile : tiles) {
            for(MoveableObject moveableObject : tile.get().getMovableObjects()) {
                int numGot = moveableObject.takeAndGive(grab, this);
                grab.setNumOf(grab.getNumOf() - numGot);
                numHas += numGot;
            }
        }
        grab.setNumOf(targetNum);

        return numHas == targetNum;
    }
}

/*
    The base rule class which there are extensions of (Like actionable)
    This is very powerful and basically the whole game will/is going to be powered by this and classes
    extended by it.


    Rule for a tile. Like 'if player rotation == 0 and nlah blaah blah, move player up 2
    For tile navigation

    Perhaps this class should be static? I'm leaving it as instance base for now due to actionable actually needing to not be static
 */

abstract class Rule {
    private int applyType = 0;

    abstract public void apply(MoveableObject npc);

    public boolean canApply(MoveableObject npc) {
        if(npc.getTravelTypes().contains(applyType)) {
            if(baseRule(npc)) {
                return true;
            }
        }
        return false;
    }
    abstract public boolean baseRule(MoveableObject npc);
}


/*
    An extension of Rule emitted to specific tiles and are supposed to be used for interactions for NPCs/MoveableObjects to MoveableObjects
    Example:
        Choice to open button for door,
        Cash register for a restraunt, tile.y + 1 buys and transmit a message to worker or something which alters navigation stack

    This should also be rotatable

 */
abstract class Actionable extends Rule {
    private MoveableObject hostObject;
    public Actionable(MoveableObject hostObject) {this.hostObject = hostObject;}
    public MoveableObject getHostObject() {return hostObject;}
}

/*
    Items from store are tagged, and running an npc through the cashier removes the tags
    for the price

 */
class CashierCustomer extends Actionable {
    private MoveableObject cost;

    public CashierCustomer(MoveableObject hostObject) {
        super(hostObject);
        cost = new Money();
    }

    public void apply(MoveableObject npc) {
        npc.takeAndGive(cost, getHostObject());
    }

    public boolean baseRule(MoveableObject npc) {
        if(npc.count(cost) > cost.getNumOf()) {
            return true;
        }
        return false;
    }
}

/*
    Not used yet but intended for tings like tile decay which could be potholes, or maybe buildings collapsing
 */

abstract class TileRule {
    abstract public void apply(Tiles.Tile tile);
}

class RuleRoadUp extends Rule {
    public void apply(MoveableObject npc) {
        npc.move(0, -1);
    }

    public boolean baseRule(MoveableObject npc) {
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
        if(npc.getTile().get().hasRule(this)) {
            return true;
        }
        return false;
    }
}
