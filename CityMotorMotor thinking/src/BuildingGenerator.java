import java.util.ArrayList;

// Given the objects wanted in room and tiles, decorate the room and such
class RoomGenerator extends Area {
    ArrayList<MoveableObject> objectsToPlace;

    public RoomGenerator(ArrayList<Tiles.Tile> tiles, ArrayList<MoveableObject> objectsToPlace) {
        super();
        this.objectsToPlace = objectsToPlace;
        addTiles(tiles);

        // Randomly place furniture
        for(MoveableObject object : objectsToPlace) {
            while (true) {
                Tiles.Tile ranTile = tiles.get((int) (Math.random() * tiles.size()));
                if(object.canMoveTo(ranTile) && object.willBeInSameArea(this, ranTile)) {
                    object.setTile(ranTile);
                    break;
                }
            }
        }
        System.out.println(tiles.get(0).getTileset());
    }

    /*
        Celluar automana for furniture organization??

        (Trust bro)/
     */
    public void organize() {
        // Look for first the worst offenders of bad designer, then work down
        // Whats defined as bad design is by the number of corners that are full with furniture
        for(int j = 0; j < 1; j++) {
            for (int i = 0; i < 4; i++) {
                while(true) {
                    boolean iterationChange = false;
                    for (MoveableObject moveableObject : objectsToPlace) {
                        Tiles.Tile[] neighbors = moveableObject.getTile().getTilesExactlyInRange(1);
                        Tiles.Tile[] cornerNeighbors = moveableObject.getNeigborCornerTiles();

                        int numOpen = 0;
                        for (Tiles.Tile neighborCorner : cornerNeighbors) {
                            if (neighborCorner.get().canMove(moveableObject) && moveableObject.willBeInSameArea(this, neighborCorner)) {
                                numOpen += 1;
                            }
                        }

                        // If the furniture is not well put, move it randomly somewhere
                        if (numOpen <= i) {
                            // TODO: Optimize lol
                            for (int useless = 0; useless < 10; useless++) {
                                int directIndex = (int) (Math.random() * 4);
                                Tiles.Tile neighborDirect = neighbors[directIndex];
                                if (moveableObject.canMoveTo(neighborDirect) && moveableObject.willBeInSameArea(this, neighborDirect)) {
                                    moveableObject.setTile(neighborDirect);
                                    iterationChange = true;
                                    break;
                                }
                            }
                        }
                    }

                    if(!iterationChange) {
                        break;
                    }
                }
            }
        }
    }
}


/*
    Rules to configure size, and what should go in rooms
 */
class AreaGeneratorManagerConfig {
    private int maxTiles;
    private int minTiles;
    private double fractionPercentAllocation;

    private ArrayList<MoveableObject> neededFurniture;
    private ArrayList<MoveableObject> wantedFurniture;

    private boolean isWalled;

    public AreaGeneratorManagerConfig() {
        this.maxTiles = -1;
        this.minTiles = -1;
        this.fractionPercentAllocation = -1;
        this.neededFurniture = new ArrayList<MoveableObject>();
        this.wantedFurniture = new ArrayList<MoveableObject>();
        this.isWalled = true;
    }

    public int numTilesWanted(int numInputTiles) {
        int percentWants = (int) (numInputTiles * fractionPercentAllocation);
        int capped = Math.min(maxTiles,
                Math.max(minTiles, percentWants)
        );
        return capped;
    }

    public double fufillmentNeed(int numTilesHas) {
        if(numTilesHas < minTiles) {
            return 1.0;
        }
        if(numTilesHas > maxTiles) {
            return 0.0;
        }

        return 1.0 - ((double) (numTilesHas - minTiles) / (maxTiles - minTiles));
    }
}

/*
    How it should work

    Put in an order for the tiles it wants
    Try to ignore people with other claims but then go for the claims if there are no open options
    claim it,
    then whichever room needs it most will get the claim. (Defined by max and min tiles)
    If minimum cannot be achieved, go on to next item in the sector list (Implemenet in sector, not here
    but saying now)
 */
class AreaGeneratorFufiller extends Area {
    private AreaGeneratorManagerConfig config;
    private Area expandableArea;

    public AreaGeneratorFufiller(AreaGeneratorManagerConfig config, Area expandArea) {
        this.config = config;
        this.expandableArea = expandArea;
    }

    public void negociateTile(Tiles.Tile tile) {
        // Give to highest fufillment need
        // Removes others and self
        double highestNeed = 0.0;
        AreaGeneratorFufiller highestNeedArea;
        ArrayList<Area> areas = tile.get().getAreas();

        // Get area with highest
        for(Area area : areas) {
            if(area.getClass() == this.getClass()) {
                AreaGeneratorFufiller asFufiller = (AreaGeneratorFufiller) area;
                double need = asFufiller.getFufillment();
                if(need > highestNeed) {
                    highestNeedArea = asFufiller;
                    highestNeed = need;
                }
            }
        }

        // Remove all of the areas of the same class
        for(int i = 0; i < areas.size(); i++) {
            Area area = areas.get(i);
            if(area.getClass() == this.getClass()) {
                areas.remove(i);
                i--;
            }
        }
    }

    public void stakeTile(Tiles.Tile tile) {
        addTile(tile);
    }

    public int numAreaContenders(Tiles.Tile tile) {
        int contenders = 0;
        for(Area a : tile.get().getAreas()) {
            if(a.getClass() == this.getClass()) {
                contenders++;
            }
        }
        return contenders;
    }

    public boolean shouldExpand() {
        /*
            Expand if not at the target tiles
            Note that contending areas are sorted out elsewhere,
        */

        int numTilesItWants = config.numTilesWanted(expandableArea.getTiles().size());

        if(tiles.size() < numTilesItWants) {
            return true;
        }
        return false;
    }

    public ArrayList<Tiles.Tile> getTilesCanExpandTo() {
        ArrayList<Tiles.Tile> potentialPotentialExpandTos = new ArrayList<Tiles.Tile>();

        // Get border of contenders
        for(Tiles.Tile tile : tiles) {
            Tiles.Tile[] tileNeighbors = tile.getNeighbors();
            for(Tiles.Tile tileNeighbor : tileNeighbors) {
                if(tileNeighbor.get().getAreas().contains(this) && !potentialPotentialExpandTos.contains(tileNeighbor)) {
                    potentialPotentialExpandTos.add(tileNeighbor);
                }
            }
        }
        return potentialPotentialExpandTos;
    }

    private int getLowestContenders(ArrayList<Tiles.Tile> potentialPotentialExpandTos) {
        int lowestContenders = -1;

        // Get lowest contending tile
        for(Tiles.Tile tile : potentialPotentialExpandTos) {
            int numContenders = numAreaContenders(tile);
            if(numContenders < lowestContenders || lowestContenders == -1) {
                lowestContenders = numContenders;
            }
        }
        return lowestContenders;
    }

    public void expandIter() {
        int lowestContenders = -1;
        ArrayList<Tiles.Tile> potentialPotentialExpandTos = getTilesCanExpandTo();

        lowestContenders = getLowestContenders(potentialPotentialExpandTos);

        // Save the tiles that match the lowest contendor
        ArrayList<Tiles.Tile> potentialExpandTos = new ArrayList<Tiles.Tile>();

        for(Tiles.Tile tile : potentialPotentialExpandTos) {
            int numContenders = numAreaContenders(tile);
            if(numContenders == lowestContenders) {
                potentialExpandTos.add(tile);
            }
        }

        // Randomly pick from potentialExpandTos
        Tiles.Tile expandTo = potentialExpandTos.get((int) (Math.random() * potentialExpandTos.size()));

        // Stake the tile
        stakeTile(expandTo);
    }

    public void generateInitial() {
        int numTilesItWants = config.numTilesWanted(expandableArea.getTiles().size());
        Tiles.Tile startTile = expandableArea.randomTile();
    }

    public double getFufillment() {
        return config.fufillmentNeed(tiles.size());
    }
}

/*
    A sector is an enclosed space which may contain 1 or more rooms
    When given the space for the sector, this will generate the rooms layout

    TODO: Compress sector if unused space? (Behavior now is that it will repeat room types)
*/
class SectorGenerator extends Area {
    private static ArrayList<AreaGeneratorManagerConfig> rooms;

    public SectorGenerator(ArrayList<Tiles.Tile> tiles) {
        super();
        addTiles(tiles);
    }

    public void generate() {
        int estimatedSpaceLeft = tiles.size();

        ArrayList<AreaGeneratorFufiller> roomGenerators = new ArrayList<AreaGeneratorFufiller>();
        int i = 0;
        while(estimatedSpaceLeft > 0) {
            AreaGeneratorManagerConfig roomConfig = rooms.get(i);
            estimatedSpaceLeft -= roomConfig.numTilesWanted(tiles.size());
            i = (i+1) % rooms.size();

            AreaGeneratorFufiller roomGenerator = new AreaGeneratorFufiller(roomConfig, this);
            roomGenerators.add(roomGenerator);
        }

    }
}

/*
    There are a few different layers of rooms:
        Rooms, like rooms of a house, serve a purpose, but don't neccesarily need a hallway
        Sectors, Need hallways
 */
class FloorGenerator extends Area {
    public FloorGenerator(ArrayList<Tiles.Tile> tiles) {
        super();
        addTiles(tiles);
    }

    public void generate() {

    }
}

class ApartmentFloorGenerator extends FloorGenerator {
    public ApartmentFloorGenerator(ArrayList<Tiles.Tile> tiles) {
        super(tiles);
    }

}

class StructureGenerator {

}

public class BuildingGenerator {
    public static void main(String[] args) {
        // Test room
        Tiles tiles = new Tiles(20, 20);
        ArrayList<Tiles.Tile> room = new ArrayList<>();
        for(int y = 2; y < 6; y++) {
            for(int x = 2; x < 12; x++) {
                room.add(tiles.get(x,y));
            }
        }
        ArrayList<MoveableObject> furniture = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            MoveableObject obj = new MoveableObject();
            obj.setSize(
                    new Vector2(
                    (int) (Math.random() * 2) + 1,
                            (int) (Math.random() * 2) + 1
                    )
            );
            furniture.add(obj);
        }

        ApartmentSectorGenerator roomGenerator = new ApartmentSectorGenerator(room);
        roomGenerator.generate();
        System.out.println(tiles);
    }
}