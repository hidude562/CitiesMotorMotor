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

    public AreaGeneratorManagerConfig(int minTiles, int maxTiles, double fractionPercentAllocation) {
        this.maxTiles = maxTiles;
        this.minTiles = minTiles;
        this.fractionPercentAllocation = fractionPercentAllocation;
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

    public double fufillmentNeed(int numTilesWant, int numTilesHas) {
        if(numTilesHas < minTiles) {
            return 1.0;
        }
        if(numTilesHas > maxTiles) {
            return 0.0;
        }

        return 1.0 - ((double) (numTilesHas - minTiles) / (numTilesWant - minTiles));
    }

    public boolean aboveMinimum(int numTiles) {
        System.out.println(numTiles);
        return numTiles >= minTiles;
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

    /*
        Kinda like the european conference on who gets who in africa
        Like france and britain get a crap ton because they are the most relevent countries
        but countries that don't have many colonies like Belgium get priority for having atleast
        some of africa since they need it the most.

        You put in general requests of what you want before but negociate from there
     */
    public void negociateTile(Tiles.Tile tile) {
        // Give to highest fufillment need
        // Removes others and self
        double highestNeed = 0.0;
        Area highestNeedArea = null;
        ArrayList<Area> areas = tile.get().getAreas();

        Tiles.Tile[] tileNeighbors = tile.getNeighbors();

        // Get area with highest
        for(Area area : areas) {
            if(area.getClass() == this.getClass()) {
                AreaGeneratorFufiller asFufiller = (AreaGeneratorFufiller) area;
                double need = asFufiller.getFufillment();

                /*
                // Ignore those if the borders will look too goofy
                int countAreas = 0;
                for(Tiles.Tile neighbor : tileNeighbors) {
                    if(neighbor.get().getAreas().contains(this)) {
                        countAreas++;
                    }
                }

                 */

                if(need > highestNeed) { //  && countAreas > 1
                    highestNeedArea = area;
                    highestNeed = need;
                }
            }
        }

        // Remove all of the areas of the same class
        for(int i = 0; i < areas.size(); i++) {
            Area area = areas.get(i);
            if(area.getClass() == this.getClass() && area != highestNeedArea) {
                area.removeTile(tile);
                i--;
            }
        }
    }

    public void negociateAllTiles() {
        for(int i = 0; i < tiles.size(); i++) {
            Tiles.Tile tile = tiles.get(i);
            negociateTile(tile);
        }
    }

    // Rearranges aqaurd tiles to be in better locations
    public boolean rearrangeNegociations() {
        boolean changed = false;
        for(int i = 0; i < tiles.size(); i++) {
            Tiles.Tile tile = tiles.get(i);
            ArrayList<Tiles.Tile> potentialRearrangeSpots = getTilesCanExpandTo();
            ArrayList<Tiles.Tile> rearrangeSpots = new ArrayList<Tiles.Tile>();

            for (Tiles.Tile potentialRearrangeSpot : potentialRearrangeSpots) {
                int numThisNeighbors = countBordersToThis(potentialRearrangeSpot);
                if (numThisNeighbors >= 2) {
                    rearrangeSpots.add(potentialRearrangeSpot);
                }
            }

            int countAreas = countBordersToThis(tile);

            // 'Aquard' tile
            if (countAreas < 2 && !rearrangeSpots.isEmpty()) {
                // Find random swap that will work for this tile (Perhaps breaking another though)
                Tiles.Tile randomSwap = rearrangeSpots.get((int) (Math.random() * rearrangeSpots.size()));

                // Remove this area from tile
                removeTile(tile);

                // Add the neighbors
                ArrayList<Area> neighborsFlip = new ArrayList<Area>();
                for(Tiles.Tile tileNeighbor : tile.getNeighbors()) {
                    ArrayList<Area> neighborAreas = tileNeighbor.get().getAreas();
                    for(Area neighborArea : neighborAreas) {
                        if(neighborArea != this && !neighborsFlip.contains(neighborArea)) {
                            neighborsFlip.add(neighborArea);
                        }
                    }
                }

                Area randomNeighborToFlipFormer = neighborsFlip.get((int) (Math.random() * neighborsFlip.size()));

                randomNeighborToFlipFormer.addTile(tile);

                ArrayList<Area> areas = randomSwap.get().getAreas();
                for(int j = 0; j < areas.size(); j++) {
                    Area area = areas.get(j);
                    if(area.getClass() == this.getClass()) {
                        areas.remove(area);
                    }
                }
                addTile(randomSwap);

                changed = true;
            }
        }

        return changed;
    }

    public int countBordersToThis(Tiles.Tile tile) {
        Tiles.Tile[] tileNeighbors = tile.getNeighbors();

        int countAreas = 0;
        for(Tiles.Tile neighbor : tileNeighbors) {
            if(neighbor.get().getAreas().contains(this)) {
                countAreas++;
            }
        }

        return countAreas;
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
                if(
                        tileNeighbor.inRange() &&
                        expandableArea.has(tileNeighbor) &&
                        !tileNeighbor.get().getAreas().contains(this) &&
                        !potentialPotentialExpandTos.contains(tileNeighbor)
                ) {
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
        if(potentialExpandTos.size() > 0) {
            Tiles.Tile expandTo = potentialExpandTos.get((int) (Math.random() * potentialExpandTos.size()));

            // Stake the tile
            stakeTile(expandTo);
        }
    }

    public void generateInitial() {
        int numTilesItWants = config.numTilesWanted(expandableArea.getTiles().size());
        Tiles.Tile startTile = expandableArea.randomTile();
        addTile(startTile);
    }

    public double getFufillment() {
        return config.fufillmentNeed(expandableArea.getTiles().size(), tiles.size());
    }

    public boolean aboveMinimum() {
        return config.aboveMinimum(tiles.size());
    }
}

/*
    A sector is an enclosed space which may contain 1 or more rooms
    When given the space for the sector, this will generate the rooms layout

    TODO: Compress sector if unused space? (Behavior now is that it will repeat room types)
*/
class SectorGenerator extends Area {
    protected static ArrayList<AreaGeneratorManagerConfig> rooms;

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
            roomGenerator.generateInitial();
            roomGenerators.add(roomGenerator);
        }

        // Like the early 1800s when europe began colonizing africa
        // Not exact and final borders, but suggestions fo final conference
        boolean renegotiate = true;
        while(renegotiate) {
            boolean allColonzied = false;
            while (!allColonzied) {
                allColonzied = true;

                for (AreaGeneratorFufiller generatorFufiller : roomGenerators) {
                    if (generatorFufiller.shouldExpand()) {
                        generatorFufiller.expandIter();
                        allColonzied = false;
                    }
                }
            }

            // Finalize borders in africa colonization
            for (AreaGeneratorFufiller roomGenerator : roomGenerators) {
                roomGenerator.negociateAllTiles();
            }

            // Clean up ugly borders
            boolean change = true;
            while (change) {
                change = false;
                for (AreaGeneratorFufiller roomGenerator : roomGenerators) {
                    boolean iterChanged = roomGenerator.rearrangeNegociations();
                    if (iterChanged) {
                        change = iterChanged;
                    }
                }
            }

            // If messing with the ugly borders made something below the minimum
            // (Likely when first run), then renegociate
            renegotiate = false;
            for (AreaGeneratorFufiller roomGenerator : roomGenerators) {
                System.out.println(roomGenerator.aboveMinimum());
                if(!roomGenerator.aboveMinimum()) {
                    renegotiate = true;
                }
            }
        }
    }
}

class ApartmentSectorGenerator extends SectorGenerator {
    static {
        rooms = new ArrayList<AreaGeneratorManagerConfig>();
        // Bedroom
        rooms.add(new AreaGeneratorManagerConfig(6,30,0.3));
        // El bano
        rooms.add(new AreaGeneratorManagerConfig(4,15,0.1));
        // Kitchen
        rooms.add(new AreaGeneratorManagerConfig(6,40,0.3));
        // Living room
        rooms.add(new AreaGeneratorManagerConfig(6,40,0.5));
    };

    public ApartmentSectorGenerator(ArrayList<Tiles.Tile> tiles) {
        super(tiles);
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

class StructureGenerator {

}

public class BuildingGenerator {
    public static void main(String[] args) {
        // Test room
        Tiles tiles = new Tiles(50, 50);
        ArrayList<Tiles.Tile> room = new ArrayList<>();
        for(int y = 2; y < 6; y++) {
            for(int x = 2; x < 15; x++) {
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