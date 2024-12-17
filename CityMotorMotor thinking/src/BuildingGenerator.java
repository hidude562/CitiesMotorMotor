import java.util.ArrayList;

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
                if(object.canMoveTo(ranTile)) {
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
    public void organizeIteration() {
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
                                System.out.println("Increase..");
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
                    System.out.println(tiles.get(0).getTileset());
                }
            }
        }
    }
}

class FloorGenerator {

}

class StructureGenerator {

}

public class BuildingGenerator {
    public static void main(String[] args) {
        Tiles tiles = new Tiles(20, 20);
        ArrayList<Tiles.Tile> room = new ArrayList<>();
        for(int y = 2; y < 12; y++) {
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

        RoomGenerator roomGenerator = new RoomGenerator(room, furniture);
        roomGenerator.organizeIteration();
        System.out.println(tiles);
    }
}