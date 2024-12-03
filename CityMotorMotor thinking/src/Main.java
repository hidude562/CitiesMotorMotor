import java.util.ArrayList;

/*
    Tiles apply a set of rules to NPCs
    TODO: refernce like jciv
 */

class Vector2 {}

class TileData {
    ArrayList<Rule> rules;
    ArrayList<NPC> npcs;

    public TileData() {
        rules = new ArrayList<Rule>();
        rules.add(new RuleRoadUp());
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

        private TileData get() {
            return tiles[y][x];
        }

        private Tile[] getNeighbors() {
            return new Tile[]{Tiles.this.get()};
        }
    }
}

class NPC {
    // 0 -> Right, 90 -> Up, 180 -> Left, 270 -> down
    int orientation;
    Vector2 size;
    Tiles.Tile tile;

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
    abstract public Tiles.Tile getMovableTiles(NPC npc);

}

// TODO: Probably should be static
class RuleRoadUp extends Rule {
    public Tiles.Tile getMovableTiles(NPC npc) {
        Tiles.Tile tile = npc.getTile();
        Tiles.Tile neighbors = tile.getNeighbors();
    }
}