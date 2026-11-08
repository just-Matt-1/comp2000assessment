import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.*;

public class CollisionDetection {

    // stores a rear-end collision detected during movement calculation
    private static Vehicle[] rearEndCollision = null;
    private static List<Map<Integer, EntityStore<Vehicle>>> turningVehiclesList;

    public static Vehicle[] findCollision(List<EntityStore<Vehicle>> vehicleArr) {
        // rear-end collisions within one lane are checked
        List<Vehicle> vehicles = vehicleArr.stream()
            .flatMap(EntityStore::stream)
            .collect(Collectors.toList());

        // compare each unique pair
        for (int firstIndex = 0; firstIndex < vehicles.size() - 1; firstIndex++) {
            for (int secondIndex = firstIndex + 1; secondIndex < vehicles.size(); secondIndex++) {
                Vehicle firstVehicle = vehicles.get(firstIndex);
                Vehicle secondVehicle = vehicles.get(secondIndex);
                if (firstVehicle.getHitBox().overlaps(secondVehicle.getHitBox())) {
                    // return first crash pair for explosion
                    return new Vehicle[] { firstVehicle, secondVehicle };
                }
            }
        }
        // detect rear-end collisions caused by speeding cars
        for (EntityStore<Vehicle> sublist : vehicleArr) {
            ArrayList<Vehicle> lane = new ArrayList<>(sublist.getEntities());

            for(Vehicle follower : lane) {
                if (!(follower instanceof SpeedingCar)){
                    continue;
                }
                for (Vehicle leader : lane) {
                    if (follower == leader) {
                        continue;
                    }
                    double forwardGap = getForwardGapInSameLane(follower, leader);

                    if (!Double.isNaN(forwardGap)
                           && follower.getHitBox().overlaps(leader.getHitBox())) {
                        return new Vehicle[] {follower, leader};
                    }
                }
            }
        }
        return null;
    }

    public static void detectHitboxCollision(List<EntityStore<Vehicle>> vehicleArr){
        for(int i = 0; i < vehicleArr.size(); i++){
            for(int j = 0; j < vehicleArr.get(i).size(); j++){
                for(int k = 0; k < vehicleArr.size(); k++){
                    for(int l = 0; l < vehicleArr.get(k).size(); l++){
                        if(vehicleArr.get(k).get(l).getHitBox().pointInHitBox(vehicleArr.get(i).get(j).getHitBox().getPointAhead())){
                            vehicleArr.get(i).get(j).setMovementBlocked(true);
                        }
                        else{
                            vehicleArr.get(i).get(j).setMovementBlocked(false);
                        }
                    }
                }
            }
        }
    }

    public static void changeTurningVehicleToDiffEntityStore(List<EntityStore<Vehicle>> vehicleArr, HashMap<EntityStore<Vehicle>, ArrayList<EntityStore<Vehicle>>> mapLanes){
        turningVehiclesList = new ArrayList<Map<Integer, EntityStore<Vehicle>>>();

        for(int i = 0; i < vehicleArr.size(); i++){

            Map<Integer, EntityStore<Vehicle>> turningVehicles = vehicleArr.get(i).stream().collect(new TurningVehicleCollector<Vehicle>());
            turningVehiclesList.add(turningVehicles);

            for(int j = 0; j < turningVehicles.get(0).size(); j++){
                boolean flag = turningVehicles.get(0).get(j).getFlag();
                if(!flag){
                    turningVehicles.get(0).get(j).setFlag(true);
                    moveToCorrectEntityStore(vehicleArr.get(i), turningVehicles.get(0).get(j), mapLanes,0);
                }
            }
            for(int j = 0; j < turningVehicles.get(1).size(); j++){
                boolean flag = turningVehicles.get(1).get(j).getFlag();
                if(!flag){
                    turningVehicles.get(1).get(j).setFlag(true);
                    moveToCorrectEntityStore(vehicleArr.get(i), turningVehicles.get(1).get(j), mapLanes,1);
                }
            }
        }
    }

    public static void moveToCorrectEntityStore(EntityStore<Vehicle> arr, Vehicle v, HashMap<EntityStore<Vehicle>, ArrayList<EntityStore<Vehicle>>> mapLanes, int dir){
        ArrayList<EntityStore<Vehicle>> potentialNewLanes = mapLanes.get(arr);
        if((dir == 0 && potentialNewLanes.size() == 2) || (dir == 0 && potentialNewLanes.size() == 4 && v.getPosition().getY() < App.height*0.5)){
            potentialNewLanes.get(0).add(v);
            arr.remove(v);
        }else if((dir == 1 && potentialNewLanes.size() == 2) || (dir == 1 && potentialNewLanes.size() == 4 && v.getPosition().getY() < App.height*0.5)){
            potentialNewLanes.get(1).add(v);
            arr.remove(v);
        }else if(dir == 0 && potentialNewLanes.size() == 4){
            if(v.getPosition().getY() > App.height*0.5){
                potentialNewLanes.get(2).add(v);
            }
            else{
                potentialNewLanes.get(3).add(v);
            }
            arr.remove(v);
        }else if(dir == 1 && potentialNewLanes.size() == 4){
            if(v.getPosition().getY() > App.height*0.5){
                potentialNewLanes.get(3).add(v);
            }
            else{
                potentialNewLanes.get(2).add(v);
            }
            arr.remove(v);
        }
    }

    public static Map<Vehicle, Double> calculateAllowedMovements(
            List<EntityStore<Vehicle>> vehicleArr,
            Map<Vehicle, Double> desiredMovements) {

        // start with relevant limits
        Map<Vehicle, Double> allowedMovements = vehicleArr.stream()
                            .flatMap(sublist -> sublist.stream())
                            .collect(Collectors.toMap(                                      // Using two parameter toMap()
                                v -> v,                                                         // 1. key
                                v -> Math.max(0, desiredMovements.getOrDefault(v, v.velocity))  // 2. value
                            ));

        // each store is one directional lane, so only the nearest leader limits a follower
        for (EntityStore<Vehicle> sublist : vehicleArr) {
            ArrayList<Vehicle> lane = sublist.stream()
                                          .sorted((first, second) -> Double.compare(getTravelProgress(second), getTravelProgress(first)))
                                          .collect(Collectors.toCollection(ArrayList::new));

            // front-to-back order passes the lead vehicle's movement through the whole queue
            for (int followerIndex = 1; followerIndex < lane.size(); followerIndex++) {
                Vehicle follower = lane.get(followerIndex);
                Vehicle leader = lane.get(followerIndex - 1);
                double forwardGap = getForwardGapInSameLane(follower, leader);
                if (Double.isNaN(forwardGap)) {
                    continue;
                }

                // speeding cars do not keep the normal safe following distance
                double requiredDistance;

               if(follower instanceof SpeedingCar) {
                    requiredDistance = getCollisionDistance(follower, leader);
                }else{
                    requiredDistance = getSafeFollowingDistance(follower, leader);
                }
                double availableGap = forwardGap - requiredDistance;
                double leaderMovement = allowedMovements.getOrDefault(leader, 0.0);
                double followingLimit = Math.max(0, availableGap + leaderMovement);
                double currentAllowance = allowedMovements.get(follower);

                // detect when a speeding car would hit the vehicle ahead
                if (follower instanceof SpeedingCar
                    && currentAllowance > followingLimit
                    && rearEndCollision == null) {
                        rearEndCollision = new Vehicle[] {follower,leader};
                    }
                allowedMovements.put(follower, Math.min(currentAllowance, followingLimit));
            }
        }

        return allowedMovements;
    }

    private static double getTravelProgress(Vehicle vehicle) {
        // convert position into distance travelled along the current direction
        double directionRadians = Math.toRadians(vehicle.getDirection());
        return vehicle.getPosition().getX() * Math.cos(directionRadians)
            + vehicle.getPosition().getY() * Math.sin(directionRadians);
    }


    private static double getForwardGapInSameLane(Vehicle follower, Vehicle leader) {
        if (Math.round(follower.getDirection()) != Math.round(leader.getDirection())) {
            return Double.NaN;
        }

        double directionRadians = Math.toRadians(follower.getDirection());
        double deltaX = leader.getPosition().getX() - follower.getPosition().getX();
        double deltaY = leader.getPosition().getY() - follower.getPosition().getY();
        double forwardGap = deltaX * Math.cos(directionRadians)
            + deltaY * Math.sin(directionRadians);
        if (forwardGap <= 0) {
            return Double.NaN;
        }

        // reject any adjacent lanes
        double lateralGap = Math.abs(
            -deltaX * Math.sin(directionRadians)
            + deltaY * Math.cos(directionRadians)
        );
        double laneOverlapDistance = follower.getHitBox().getLateralExtent()
            + leader.getHitBox().getLateralExtent();
        return lateralGap <= laneOverlapDistance ? forwardGap : Double.NaN;
    }
    // calculate the distance at which two vehicless touch
    private static double getCollisionDistance(Vehicle follower, Vehicle leader){
        return follower.getHitBox().getForwardExtent()
        + leader.getHitBox().getForwardExtent();
    }
    private static double getSafeFollowingDistance(Vehicle follower, Vehicle leader) {
        // combine buffered front and rear extends with a gap
        return follower.getHitBox().getForwardExtent()
            + leader.getHitBox().getForwardExtent()
            + 2;
    }
    // returns the rear-end collision and clears it for the next frame
    public static Vehicle[] getRearEndCollision(){
        Vehicle[] collision = rearEndCollision;
        rearEndCollision = null;
        return collision;
    }
}
