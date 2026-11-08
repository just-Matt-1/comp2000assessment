public class TurnRight implements VehicleState{

    Vehicle vehicle;
    int counter = 0;
    public TurnRight(Vehicle vehicle){
        this.vehicle = vehicle;
    }

    public void move(Turn turn, double movementVelocity){
        if (movementVelocity == 0) return;
        vehicle.direction = vehicle.direction + ((10/(Math.PI*turn.getRadius())) * 180/Math.PI);
        vehicle.direction = (vehicle.direction < 0) ? (vehicle.direction + 360) : vehicle.direction;
        vehicle.direction = (vehicle.direction > 359) ? (vehicle.direction - 360) : vehicle.direction;
        vehicle.direction = (vehicle.direction > turn.getFinishAngle() && turn.getFinishAngle() != 0) ? turn.getFinishAngle() : vehicle.direction;
        vehicle.position = Velocity.calPositionTurningRight(turn, vehicle.direction, vehicle.x, vehicle.y, counter, (int)movementVelocity);
        vehicle.x = vehicle.position.getX();
        vehicle.y = vehicle.position.getY();
        vehicle.updatePosition();
        counter = counter + 1;
        if((int)vehicle.direction == (int)turn.getFinishAngle()){
            vehicle.setDirection(turn.getFinishAngle());
            vehicle.setState(vehicle.getMoveForwardState());
            vehicle.removeTurn();
            counter = 0;
            vehicle.setFlag(true);
        }
    }
}
