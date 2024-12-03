import java.util.ArrayList;

/*
    Tiles apply a set of rules to NPCs
    TODO: refernce like jciv
 */

class Vector2 {
    public double x;
    public double y;
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

class TileData {
    ArrayList<Rule> rules;
    ArrayList<NPC> npcs;

    public TileData() {
        rules = new ArrayList<Rule>();
        rules.add(new RuleRoadUp());
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
}

class Tiles {
    TileData[][] tiles;

    public Tiles() {
        tiles = new TileData[32][32];
        for(TileData[] i : tiles) {
            for(TileData j : i) {
                j = new TileData();
            }
        }
    }

    public Tile get(int x, int y) {
        return new Tile(x, y);
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

        public Tile[] getNeighbors() {
            Tile[] tiles = new Tile[4];
            for(int i = 0; i < 4; i++) {
                Tile tile = new Tile(i%2*2-1+x, (i+1)%2*2-1+y);
            }
            return tiles;
        }
    }
}

class NPC {
    // 0 -> Right, 90 -> Up, 180 -> Left, 270 -> down
    int orientation;
    Vector2 size;
    Tiles.Tile tile;

    public NPC(int orientation, Vector2 size, Tiles.Tile tile) {
        this.orientation = orientation;
        this.size = size;
        this.tile = tile;

        this.tile.get().npcs.add(this);
    }

    public ArrayList<Tiles.Tile> getMoveableTiles() {
        
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
    Rules for tiles:

    - For all tiles, if the target tile is not open, do not push direction (Except if it is self)
    - Some tile need to send signals to others. Like an intersection.
*/

abstract class Rule {
    abstract public ArrayList<Tiles.Tile> getMovableTiles(NPC npc);

}

// TODO: Probably should be static
class RuleRoadUp extends Rule {
    public ArrayList<Tiles.Tile> getMovableTiles(NPC npc) {
        Tiles.Tile tile = npc.getTile();
        Tiles.Tile[] neighbors = tile.getNeighbors();
        ArrayList<Tiles.Tile> moveableTiles = new ArrayList<Tiles.Tile>();

        for(Tiles.Tile neighbor : neighbors) {
            if(neighbor.get().canMove(npc)) {
                if(ruleset(npc, tile)) {
                    moveableTiles.add(neighbor);
                }
            }
        }

        return moveableTiles;
    }

    public boolean ruleset(NPC npc, Tiles.Tile tile) {
        if(npc.orientation == 90 && tile.get().hasRule(this)) {
            return true;
        }
        return false;
    }
}

public class Main {
    public static void main(String[] args) {
        Tiles tiles = new Tiles();
        NPC npc = new NPC(90, new Vector2(0.1, 0.1), tiles.get(10,10));

        npc.getTile().
    }
}