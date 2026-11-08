// a bunch of tests, mostly for debugging and running controlled scenarios 

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class CollisionDetectionTest {
    public static void main(String[] args) {
        // run focused regression checks for the traffic deadlocks
        testFollowingVehicleStopsForStationaryLeader();
        testSeparatedVehiclesContinue();
        testTboneCollisionIsDetected();
        testRearEndCollisionIsDetected();
        testMotorbikeHitboxBufferIsRespected();
        testMovementIsLimitedToSafeFollowingDistance();
        System.out.println("Collision detection tests passed.");
    }

    private static void testFollowingVehicleStopsForStationaryLeader() {
        // a close follower must stop when the leading vehicle is stationary
        Car rear = createCar(100, 100, 90);
        Car front = createCar(100, 150, 90);
        EntityStore<Vehicle> lane = createLane(rear, front);

        Map<Vehicle, Double> allowed = calculateMovements(List.of(lane), rear, 8, front, 0);
        require(allowed.get(rear) == 0,
            "The rear vehicle should stop for a stationary leader.");
    }

    private static void testSeparatedVehiclesContinue() {
        // vehicles outside the following distance shouldnt block either vehicle
        Car rear = createCar(100, 100, 90);
        Car front = createCar(100, 300, 90);
        EntityStore<Vehicle> lane = createLane(rear, front);

        Map<Vehicle, Double> allowed = calculateMovements(List.of(lane), rear, 8, front, 8);
        require(allowed.get(rear) == 8 && allowed.get(front) == 8,
            "Separated vehicles should continue at their requested speed.");
    }

    private static void testTboneCollisionIsDetected() {
        // overlapping vehicles in perpendicular lanes cause a t-bone collision
        Car southbound = createCar(100, 100, 90);
        Car eastbound = createCar(100, 100, 0);
        EntityStore<Vehicle> southboundLane = createLane(southbound);
        EntityStore<Vehicle> eastboundLane = createLane(eastbound);

        Vehicle[] collision = CollisionDetection.findCollision(List.of(southboundLane, eastboundLane));
        require(collision != null,
            "Overlapping perpendicular vehicles should create a collision.");
    }

    private static void testRearEndCollisionIsDetected() {
        Car firstVehicle = createCar(100, 100, 90);
        Car secondVehicle = createCar(100, 100, 90);

        // keep both vehicles in one lane
        Vehicle[] collision = CollisionDetection.findCollision(
            List.of(createLane(firstVehicle, secondVehicle))
        );
        require(collision != null,
            "Overlapping same-direction vehicles should create a rear-end collision.");
    }

    private static void testMotorbikeHitboxBufferIsRespected() {
        // motorbikes use a larger hitbox than base dimensions 
        Car rear = createCar(100, 100, 90);
        Motorbike front = new Motorbike(new Point(100, 160), 20, 30, 90);
        front.setPosition(100, 160);
        front.setDirection(90);
        front.updateHitBox();
        EntityStore<Vehicle> lane = createLane(rear, front);

        Map<Vehicle, Double> allowed = calculateMovements(List.of(lane), rear, 8, front, 0);
        require(allowed.get(rear) == 0,
            "The rear vehicle should stop before reaching the motorbike's enlarged hitbox.");
    }

    private static void testMovementIsLimitedToSafeFollowingDistance() {
        // a fast vehicle may only move as far as the safe gap allows
        Car rear = createCar(100, 100, 90);
        Car stoppedFront = createCar(100, 160, 90);
        EntityStore<Vehicle> lane = createLane(rear, stoppedFront);

        Map<Vehicle, Double> allowed = calculateMovements(List.of(lane), rear, 10, stoppedFront, 0);
        require(allowed.get(rear) > 0 && allowed.get(rear) <= 3.0001,
            "A vehicle should only move up to the safe following distance.");
    }

    private static Car createCar(double x, double y, float direction) {
        // initialise both component and hitbox pos for each vehicle 
        Car car = new Car(new Point(x, y), 30, 50, direction);
        car.setPosition(x, y);
        car.setDirection(direction);
        car.updateHitBox();
        return car;
    }

    private static EntityStore<Vehicle> createLane(Vehicle... vehicles) {
        // build the same typed store structure as live for any number of test vehicles
        EntityStore<Vehicle> lane = new EntityStore<>();
        // add each supplied vehicle so tests can also represent one vehicle per spawn store
        for (Vehicle vehicle : vehicles) {
            lane.add(vehicle);
        }
        return lane;
    }

    private static Map<Vehicle, Double> calculateMovements(
            List<EntityStore<Vehicle>> lanes,
            Vehicle firstVehicle, double firstMovement,
            Vehicle secondVehicle, double secondMovement) {
        Map<Vehicle, Double> desiredMovements = new IdentityHashMap<>();
        desiredMovements.put(firstVehicle, firstMovement);
        desiredMovements.put(secondVehicle, secondMovement);
        return CollisionDetection.calculateAllowedMovements(lanes, desiredMovements);
    }

    private static void require(boolean condition, String message) {
        // throw error 
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
