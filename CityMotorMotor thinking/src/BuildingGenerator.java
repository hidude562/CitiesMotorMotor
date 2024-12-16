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
            obj.setSize(new Vector2(2, 2));
            furniture.add(obj);
        }

        RoomGenerator roomGenerator = new RoomGenerator(room, furniture);
        System.out.println(tiles);
    }
}