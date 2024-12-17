import java.util.ArrayList;

class RoomGenerator {
    ArrayList<MoveableObject> objectsToPlace;
    ArrayList<Tiles.Tile> tiles;

    public RoomGenerator(ArrayList<Tiles.Tile> tiles, ArrayList<MoveableObject> objectsToPlace) {
        this.objectsToPlace = objectsToPlace;
        this.tiles = tiles;

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
                            if (neighborCorner.get().canMove(moveableObject)) {
                                numOpen += 1;
                                System.out.println("Increase..");
                            }
                        }

                        // If the furniture is not well put, move it randomly somewhere
                        if (numOpen <= i) {
                            // TODO: Optimize lol
                            for (int useless = 0; useless < 10; useless++) {
                                int directIndex = (int) (Math.random() * 4) + 4;
                                Tiles.Tile neighborDirect = neighbors[directIndex];
                                if (moveableObject.canMoveTo(neighborDirect)) {
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
        for(int y = 5; y < 15; y++) {
            for(int x = 5; x < 15; x++) {
                room.add(tiles.get(x,y));
            }
        }
        ArrayList<MoveableObject> furniture = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            MoveableObject obj = new MoveableObject();
            obj.setSize(new Vector2(3, 2));
            furniture.add(obj);
        }

        RoomGenerator roomGenerator = new RoomGenerator(room, furniture);
        roomGenerator.organizeIteration();
        System.out.println(tiles);
    }
}