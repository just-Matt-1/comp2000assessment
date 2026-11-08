//import java.math.*;

public class Velocity {
    static public Point calPosition(double x, double y, double direction, double velocity) {
        direction = direction * Math.PI/180;
        double xPosAddition = Math.cos(direction) * velocity;
        double yPosAddition = Math.sin(direction) * velocity;
        x += xPosAddition;
        y += yPosAddition;
        return new Point(x, y);
    }

    static public Point calPositionWithRadius(Point p, double direction, double radius, int xMultiplier, int yMultiplier, double xAddon, double yAddon){
        double newX = 0.0;
        double newY = 0.0;
        if(direction % 180 != 0 && direction % 90.0 == 0){
            newX = p.getX() + yAddon * xMultiplier;
            newY = p.getY() + xAddon * yMultiplier;
        }
        else if(direction % 180 == 0){
            newX = p.getX() + xAddon * xMultiplier;
            newY = p.getY() + yAddon * yMultiplier;
        }
        return new Point(newX, newY);
    }

    static public Point calPositionWithRadius(Point p, double direction, double radius){
        double dir = direction * Math.PI/180;
        double newX = 0.0;
        double newY = 0.0;
        newX = p.getX() + Math.cos(dir) * radius;
        newY = p.getY() + Math.sin(dir) * radius;
        return new Point(newX, newY);
    }

    static public Point calPositionTurningLeft(Turn turn, double direction, double x, double y, int counter, int movementVelocity){
        float radius = turn.getRadius();
        direction = direction * Math.PI/180;
        double startAngle = turn.getStartAngle() * Math.PI/180;
        double plugin = startAngle - 5*counter/(Math.PI*turn.getRadius()) + Math.PI/2;
        plugin = (plugin >= 2*Math.PI) ? plugin - 2*Math.PI : plugin;
        plugin = (Math.PI*radius)/2 - plugin;
        double xPos = turn.getRotationPoint().getX() - (radius * Math.cos(plugin));
        double yPos = turn.getRotationPoint().getY() + (radius * Math.sin(plugin));
        return new Point(xPos, yPos);
    }

    static public Point calPositionTurningRight(Turn turn, double direction, double x, double y, int counter, int movementVelocity){
        float radius = turn.getRadius();
        direction = direction * Math.PI/180;
        double startAngle = turn.getStartAngle() * Math.PI/180;
        double plugin = 10*counter/(Math.PI*turn.getRadius()) - Math.PI/2 + startAngle;
        plugin = (plugin >= 2*Math.PI) ? plugin - 2*Math.PI : plugin;
        double xPos = turn.getRotationPoint().getX() + (radius * Math.cos(plugin));
        double yPos = turn.getRotationPoint().getY() + (radius * Math.sin(plugin));
        return new Point(xPos, yPos);
    }
}
