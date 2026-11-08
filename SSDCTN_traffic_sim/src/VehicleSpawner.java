import java.util.Random;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;

public class VehicleSpawner {
    // Frame dimensions
    private int width, height;

    // spawn points
    private Start NORTH_SP;
    private Start EAST_SP;
    private Start SOUTH_SP;
    private Start WEST_SP;
    private Start EASTTOO_SP;
    private Start WESTTOO_SP;

    // directions of travel
    private final float NORTH = 270;
    private final float EAST = 0;
    private final float SOUTH = 90;
    private final float WEST = 180;

    // number of vehicle types and declarations for their dimensions
    private final int VCL_TYPE_AMOUNT = 6;
    private double BUS_W, BUS_H;
    private double CAR_W, CAR_H;
    private double CYCLIST_W, CYCLIST_H;
    private double MOTORBIKE_W, MOTORBIKE_H;
    private double TRUCK_W, TRUCK_H;

    private long totalVehiclesSpawned;

    // a buffer for spawning/despawning vehicles to ensure they are entirely off screen
    private final int SPAWN_BUFFER;

    // track the current visible bounds so spawning and despawning follow window edges
    private double viewportLeft, viewportTop, viewportRight, viewportBottom;

    // spawn variables
    private int spawnChance;
    private Random rand;

    // List active vehicles with sublists based on spawn point (now uses EntityStore to reject unrelated object types at compile time (generics yay!))
    private List<EntityStore<Vehicle>> activeVehicles;
    private EntityStore<Vehicle> northSpawn, eastSpawn, southSpawn, westSpawn, eastTooSpawn, westTooSpawn;
    private HashMap<EntityStore<Vehicle>, ArrayList<EntityStore<Vehicle>>> mapLanes;

    ArrayList<EntityStore<Vehicle>> northArr;
    ArrayList<EntityStore<Vehicle>> eastArr;
    ArrayList<EntityStore<Vehicle>> southArr;
    ArrayList<EntityStore<Vehicle>> westArr;
    ArrayList<EntityStore<Vehicle>> eastTooArr;
    ArrayList<EntityStore<Vehicle>> westTooArr;

    //graphing
    private Graph localGraph;

    VehicleSpawner(int spawnChance, Graph g) throws SimulationConfigurationException {

        NORTH_SP = g.getVertex(0).getStart();
        EAST_SP = g.getVertex(1).getStart();
        SOUTH_SP = g.getVertex(2).getStart();
        WEST_SP = g.getVertex(3).getStart();
        EASTTOO_SP = g.getVertex(4).getStart();
        WESTTOO_SP = g.getVertex(5).getStart();

        if (spawnChance < 0 || spawnChance > 100) {
            throw new SimulationConfigurationException("Spawn chance must be between 0 and 100.");
        }
        this.width = App.width;     // provides app's bounds for determining spawn positions
        this.height = App.height;
        this.spawnChance = spawnChance;     // specified as a percentage
        SPAWN_BUFFER = (int) ((width+height)/2 * 0.15);
        
        // initialise viewport and spawn points to original panel bounds 
        updateViewportBounds(0, 0, width, height);
        assignDimensions();
        rand = new Random();

        northSpawn = new EntityStore<>();
        eastSpawn = new EntityStore<>();
        southSpawn = new EntityStore<>();
        westSpawn = new EntityStore<>();
        eastTooSpawn = new EntityStore<>();
        westTooSpawn = new EntityStore<>();

        activeVehicles = new ArrayList<>();
        activeVehicles.add(northSpawn);
        activeVehicles.add(eastSpawn);
        activeVehicles.add(southSpawn);
        activeVehicles.add(westSpawn);
        activeVehicles.add(eastTooSpawn);
        activeVehicles.add(westTooSpawn);

        mapLanes = new HashMap<EntityStore<Vehicle>, ArrayList<EntityStore<Vehicle>>>();
        northArr = new ArrayList<>(List.of(westSpawn, eastSpawn, westTooSpawn, eastTooSpawn));
        eastArr = new ArrayList<>(List.of(northSpawn, southSpawn));
        southArr = new ArrayList<>(List.of(eastSpawn, westSpawn, eastTooSpawn, westTooSpawn));
        westArr = new ArrayList<>(List.of(southSpawn, northSpawn));
        eastTooArr = new ArrayList<>(List.of(northSpawn, southSpawn));
        westTooArr = new ArrayList<>(List.of(southSpawn, northSpawn));
        mapLanes.put(northSpawn, northArr);
        mapLanes.put(eastSpawn, eastArr);
        mapLanes.put(southSpawn, southArr);
        mapLanes.put(westSpawn, westArr);
        mapLanes.put(eastTooSpawn, eastTooArr);
        mapLanes.put(westTooSpawn, westTooArr);

        localGraph = g;
    }


    public void spawn() {
        if (rand.nextInt(100) < spawnChance && activeVehicles.get(0).size() < 2) spawnHelper(NORTH_SP, SOUTH, 0);
        if (rand.nextInt(100) < spawnChance && activeVehicles.get(1).size() < 2) spawnHelper(EAST_SP, WEST, 1);
        if (rand.nextInt(100) < spawnChance && activeVehicles.get(2).size() < 2) spawnHelper(SOUTH_SP, NORTH, 2);
        if (rand.nextInt(100) < spawnChance && activeVehicles.get(3).size() < 2) spawnHelper(WEST_SP, EAST, 3);
        if (rand.nextInt(100) < spawnChance && activeVehicles.get(4).size() < 2) spawnHelper(EASTTOO_SP, WEST, 4);
        if (rand.nextInt(100) < spawnChance && activeVehicles.get(5).size() < 2) spawnHelper(WESTTOO_SP, EAST, 5);
    }

    private void spawnHelper(Start spawnPoint, float direction, int sublistIndex) {
        int vehicleType = rand.nextInt(VCL_TYPE_AMOUNT);
            
        // prevents spawning when a vehicle is already in spawn zone
        int lastElement = activeVehicles.get(0).getEntities().size()-1;
        if (lastElement >= 0 && activeVehicles.get(0).get(lastElement).getPosition().getY() < viewportTop) {
            vehicleType = -1;   // uses default case (doesn't create vehicle)
        } else {
            totalVehiclesSpawned++;
        }

        Vertex finish = localGraph.getRandomFinish(localGraph.getVertex(spawnPoint.getID()));

        switch (vehicleType) {
            case 0: // BUS
                Bus bus = new Bus(spawnPoint.getStartPoint(), BUS_W, BUS_H, direction);
                bus.createRoute(spawnPoint.getID(), finish.getID(), localGraph);
                activeVehicles.get(sublistIndex).add(bus);
                break;
            
            case 1: // CAR
                Car car = new Car(spawnPoint.getStartPoint(), CAR_W, CAR_H, direction);
                car.createRoute(spawnPoint.getID(), finish.getID(), localGraph);
                activeVehicles.get(sublistIndex).add(car);
                break;

            case 2:  // CYCLIST
                Cyclist cyclist = new Cyclist(spawnPoint.getStartPoint(), CYCLIST_W, CYCLIST_H, direction);
                cyclist.createRoute(spawnPoint.getID(), finish.getID(), localGraph);
                activeVehicles.get(sublistIndex).add(cyclist);
                break;
            
            case 3: // MOTORBIKE
                Motorbike motorbike = new Motorbike(spawnPoint.getStartPoint(), MOTORBIKE_W, MOTORBIKE_H, direction);
                motorbike.createRoute(spawnPoint.getID(), finish.getID(), localGraph);
                activeVehicles.get(sublistIndex).add(motorbike);
                break;

            case 4: // TRUCK
                Truck truck = new Truck(spawnPoint.getStartPoint(), TRUCK_W, TRUCK_H, direction);
                truck.createRoute(spawnPoint.getID(), finish.getID(), localGraph);
                activeVehicles.get(sublistIndex).add(truck);
                break;
            
            case 5: // SPEEDING CAR
                // Randomly choose whether the speeding car follows traffic lights.
                TrafficLightStrategy strategy = rand.nextBoolean()
                ? new FollowTrafficLight()
                : new IgnoreTrafficLight();
                
                // Create a speeding car with the selected strategy.
                SpeedingCar speedingCar = new SpeedingCar(spawnPoint.getStartPoint(), CAR_W, CAR_H, direction, strategy);
                speedingCar.createRoute(spawnPoint.getID(), finish.getID(), localGraph);
                activeVehicles.get(sublistIndex).add(speedingCar);
                break;

            default:
        }
    }


    // run periodically to delete (make eligible for garbage collection) vehicles out of the frame bounds
    public void despawn() {
        // System.out.println("VehicleSpawner.despawn() called");
        for (EntityStore<Vehicle> sublist : activeVehicles) 
            despawnHelper(sublist);
    }
    
    // takes the N/E/S/W activeVehicle sublists 
    private void despawnHelper(EntityStore<Vehicle> sublist) {
        Iterator<Vehicle> iterator = sublist.modifyEntities().iterator();
        while (iterator.hasNext()) {
            Vehicle v = iterator.next();
            // remove vehicles only after they have passed beyond current visible window and buffer
            if (v.getPosition().getX() < viewportLeft-SPAWN_BUFFER
            || v.getPosition().getY() < viewportTop-SPAWN_BUFFER
            || v.getPosition().getX() > viewportRight+SPAWN_BUFFER
            || v.getPosition().getY() > viewportBottom+SPAWN_BUFFER) {
                // System.out.println("Vehicle despawned at " + v.getPosition());
                iterator.remove();
            }
        }
    }


    private void assignDimensions() {
        BUS_W       = width * 0.05;
        BUS_H       = height* 0.12;
        CAR_W       = width * 0.0375;
        CAR_H       = height* 0.0625;
        CYCLIST_W   = width * 0.05;
        CYCLIST_H   = height* 0.05;
        MOTORBIKE_W = width * 0.025;
        MOTORBIKE_H = height * 0.0375;
        TRUCK_W     = width * 0.1;
        TRUCK_H     = height* 0.08;
    }


    // move spawn / despawn boundaries to the coords of resized panel 
    public void updateViewportBounds(double left, double top, double right, double bottom) {
        viewportLeft = left;
        viewportTop = top;
        viewportRight = right;
        viewportBottom = bottom;

        // replace points so active vehicles never share a moving spawn coord
        NORTH_SP.setStartPoint(new Point(width * 0.57, viewportTop - SPAWN_BUFFER));
        EAST_SP.setStartPoint(new Point(viewportRight + SPAWN_BUFFER, height * 0.32));
        EASTTOO_SP.setStartPoint(new Point(viewportRight + SPAWN_BUFFER, height*0.82));
        SOUTH_SP.setStartPoint(new Point(width * 0.43, viewportBottom + SPAWN_BUFFER));
        WEST_SP.setStartPoint(new Point(viewportLeft - SPAWN_BUFFER, height * 0.18));
        WESTTOO_SP.setStartPoint(new Point(viewportLeft - SPAWN_BUFFER, height*0.68));
    }


    // Returns active vehicles as a 2D array (essentially*)  
    public List<EntityStore<Vehicle>> getVehicleList() {
        // return store's read-only typed view for Panel's movement and drawing loops
        return activeVehicles;
    }

    // Returns active vehicles as a stream
    public Stream<Vehicle> getVehicleStream() {
        return activeVehicles.stream()
                             .flatMap(sublist -> sublist.stream());
    }

    public void resetVehicleCount() {
        totalVehiclesSpawned = 0;
    }

    public long getTotalVehicleCount() {
        return totalVehiclesSpawned;
    }

    public HashMap<EntityStore<Vehicle>, ArrayList<EntityStore<Vehicle>>> getMapLanes(){
        return mapLanes;
    }
}
