public class MoveForward implements VehicleState{

    Vehicle vehicle;
    public MoveForward(Vehicle vehicle){
        this.vehicle = vehicle;
    }

    public void move(Turn turn, double movementVelocity){
        vehicle.position = Velocity.calPosition(vehicle.x, vehicle.y, vehicle.direction, movementVelocity);
        vehicle.x = vehicle.position.getX();
        vehicle.y = vehicle.position.getY();
        vehicle.updatePosition();
        if(vehicle.getVehicleRoute().size() == 1){
            return;
        }
        if(turn.vehicleOverLapTurn(vehicle) && turn.getDirection() == 0){
            vehicle.setState(vehicle.getTurnLeftState());
        }
        else if(turn.vehicleOverLapTurn(vehicle) && turn.getDirection() == 1){
            vehicle.setState(vehicle.getTurnRightState());
        }
        else if(turn.vehicleOverLapTurn(vehicle) && turn.getDirection() == 2){
            vehicle.removeTurn();
        }
    }
}
