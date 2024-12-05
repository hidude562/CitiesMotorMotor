import java.util.ArrayList;

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
    ArrayList<NPC> npcs;

    public TileData() {
        rules = new ArrayList<Rule>();
        npcs = new ArrayList<NPC>();
        rules.add(new RuleRoadUp());
        rules.add(new RuleRoadRight());

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

    public ArrayList<NPC> getNpcs() {
        return npcs;
    }

    public void setNpcs(ArrayList<NPC> npcs) {
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

    public Tiles() {
        tiles = new TileData[32][32];
        for(int i = 0; i < tiles.length; i++) {
            for(int j = 0; j < tiles[i].length; j++) {
                tiles[i][j] = new TileData();
            }
        }
    }

    public Tile get(int x, int y) {
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
        private int y;
        private int x;

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

class MoveableObject {
    // 0 -> Right, 90 -> Up, 180 -> Left, 270 -> down
    int orientation;
    Vector2 size;
    Tiles.Tile tile;
    ArrayList<Integer> navigation;
    ArrayList<MoveableObject> carrying;
    MoveableObject parent;

    /*
        Types it can immitate:
            0 - General people navigation (sidewalks, parks, etc.)
            1 - General Vehicle navigation (roads)
     */
    int[] travelTypes;

    public MoveableObject(int orientation, Vector2 size, Tiles.Tile tile) {
        this.orientation = orientation;
        this.size = size;
        this.tile = tile;
        this.navigation = new ArrayList<Integer>();

        this.tile.get().getNpcs().add(this);
    }

    public void navigate() {
        int choice = navigation.removeFirst();
        this.tile.get().getRules().get(choice).apply(this);
    }

    public boolean navigateTo(Tile t) {

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
    abstract public void apply(MoveableObject npc);
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

    public boolean canApply(MoveableObject npc) {
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

    public boolean canApply(MoveableObject npc) {
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

    public boolean canApply(MoveableObject npc) {
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

    public boolean canApply(MoveableObject npc) {
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

    public boolean canApply(MoveableObject npc) {
        if(       npc.getTile().get().hasRule(this)) {
            return true;
        }
        return false;
    }
}

public class Main {
    public static void main(String[] args) {
        Tiles tiles = new Tiles();
        NPC npc = new NPC(90, new Vector2(0.1, 0.1), tiles.get(10,10));
        for(int i = 0; i < 5; i++) {npc.navigate();}
        System.out.println(tiles.toString());
    }
}