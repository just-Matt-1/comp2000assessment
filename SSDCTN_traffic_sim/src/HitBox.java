import java.util.ArrayList;

public class HitBox {
    private double xAddon;
    private double yAddon;
    private double radius;
    private Point pointAhead;
    private double direction;
    private boolean isActive;
    private ArrayList<Point> pointArr = new ArrayList<>();
    private ArrayList<Edge> edgeArr = new ArrayList<>();
    private Point centrePoint;

    HitBox(Point p, double Direction, double width, double height){
        xAddon = height*0.55;   // *0.55 to create a small buffer between vehicles.
        yAddon = width*0.5;
        getRadius();
        direction = Direction;
        centrePoint = p;
        pointAhead = Velocity.calPositionWithRadius(centrePoint, direction, yAddon + 5);

        Point p1 = new Point(p.getX() + xAddon, p.getY() + yAddon);
        Point p2 = new Point(p.getX() + xAddon, p.getY() - yAddon);
        Point p3 = new Point(p.getX() - xAddon, p.getY() - yAddon);
        Point p4 = new Point(p.getX() - xAddon, p.getY() + yAddon);
        pointArr.add(p1);
        pointArr.add(p2);
        pointArr.add(p3);
        pointArr.add(p4);

        Edge e1 = new Edge(p1, p2);
        Edge e2 = new Edge(p2, p3);
        Edge e3 = new Edge(p3, p4);
        Edge e4 = new Edge(p4, p1);
        edgeArr.add(e1);
        edgeArr.add(e2);
        edgeArr.add(e3);
        edgeArr.add(e4);        
    }

    public void updateHitbox(Point p, double direction){
        this.direction = direction;
        centrePoint = p;
        pointArr.set(0, Velocity.calPositionWithRadius(p, direction, radius, -1, 1, xAddon, yAddon));
        pointArr.set(1, Velocity.calPositionWithRadius(p, direction, radius, 1, 1, xAddon, yAddon));
        pointArr.set(2, Velocity.calPositionWithRadius(p, direction, radius, 1, -1, xAddon, yAddon));
        pointArr.set(3, Velocity.calPositionWithRadius(p, direction, radius, -1, -1, xAddon, yAddon));
        edgeArr.set(0, new Edge(pointArr.get(0), pointArr.get(1)));
        edgeArr.set(1, new Edge(pointArr.get(1), pointArr.get(2)));
        edgeArr.set(2, new Edge(pointArr.get(2), pointArr.get(3)));
        edgeArr.set(3, new Edge(pointArr.get(3), pointArr.get(0)));
        pointAhead = Velocity.calPositionWithRadius(centrePoint, direction, xAddon + 2);
    }

    public boolean detectHitboxOverlap(HitBox other){
        double thisLowerMostPoint = getLowerMostPoint();
        double thisUpperMostPoint = getUpperMostPoint();
        double thisLeftMostPoint = getLeftMostPoint();
        double thisRightMostPont = getRightMostPoint();
        double otherLowerMostPoint = other.getLowerMostPoint();
        double otherUpperMostPoint = other.getUpperMostPoint();
        double otherLeftMostPoint = other.getLeftMostPoint();
        double otherRightMostPoint = other.getRightMostPoint();
        if((int)direction == 270){
            if(pointAhead.getY() <= otherUpperMostPoint && thisUpperMostPoint >= otherLowerMostPoint && ((thisLeftMostPoint >= otherLeftMostPoint && thisLeftMostPoint <= otherRightMostPoint) || (thisRightMostPont <= otherRightMostPoint && thisRightMostPont >= otherLeftMostPoint) || (centrePoint.getX() >= otherLeftMostPoint && centrePoint.getX() <= otherRightMostPoint))){
                return true;
            }
        }

        else if((int)direction == 0){
            if(pointAhead.getX() >= otherLeftMostPoint && thisLeftMostPoint <= otherRightMostPoint && ((thisUpperMostPoint >= otherLowerMostPoint && thisUpperMostPoint <= otherUpperMostPoint) || (thisLowerMostPoint <= otherUpperMostPoint && thisLowerMostPoint >= otherLowerMostPoint) || (centrePoint.getY() >= otherLowerMostPoint && centrePoint.getY() <= otherUpperMostPoint))){
                return true;
            }
        }

        if((int)direction == 90){
            if(pointAhead.getY() >= otherLowerMostPoint && thisLowerMostPoint <= otherUpperMostPoint && ((thisLeftMostPoint >= otherLeftMostPoint && thisLeftMostPoint <= otherRightMostPoint) || (thisRightMostPont <= otherRightMostPoint && thisRightMostPont >= otherLeftMostPoint) || (centrePoint.getX() >= otherLeftMostPoint && centrePoint.getX() <= otherRightMostPoint))){
                return true;
            }
        }

        else if((int)direction == 180){
            if(pointAhead.getX() <= otherRightMostPoint && thisRightMostPont >= otherLeftMostPoint && ((thisUpperMostPoint >= otherLowerMostPoint && thisUpperMostPoint <= otherUpperMostPoint) || (thisLowerMostPoint <= otherUpperMostPoint && thisLowerMostPoint >= otherLowerMostPoint) || (centrePoint.getY() >= otherLowerMostPoint && centrePoint.getY() <= otherUpperMostPoint))){
                return true;
            }
        }

        for(int i = 0; i < other.getPointArr().size(); i ++){
            if(pointInHitBox(other.getPointArr().get(i))){
                return true;
            }
        }
        return false;
    }

    public boolean overlaps(HitBox other) {
        // compare two hitbox boundaries to find real overlap 
        return getLeftMostPoint() < other.getRightMostPoint()
            && getRightMostPoint() > other.getLeftMostPoint()
            && getLowerMostPoint() < other.getUpperMostPoint()
            && getUpperMostPoint() > other.getLowerMostPoint();
    }

    public boolean pointInHitBox(Point p){
        if(direction % 90 != 0){
            if(this.getLeftMostPoint() <= p.getX() && this.getRightMostPoint() >= p.getX() && this.getUpperMostPoint() >= p.getY() && this.getLowerMostPoint() <= p.getY()){
                return true;
            }
        }
        return false;
    }

    public Double getLeftMostPoint(){
        Double leftMostPoint = null;
        for(Point p : pointArr){
            if(leftMostPoint == null || p.getX() < leftMostPoint){
                leftMostPoint = p.getX();
            }
        }
        return leftMostPoint;
    }

    public Double getRightMostPoint(){
        Double rightMostPoint = null;
        for(Point p : pointArr){
            if(rightMostPoint == null || p.getX() > rightMostPoint){
                rightMostPoint = p.getX();
            }
        }
        return rightMostPoint;
    }

    public Double getUpperMostPoint(){
        Double upperMostPoint = null;
        for(Point p : pointArr){
            if(upperMostPoint == null || p.getY() > upperMostPoint){
                upperMostPoint = p.getY();
            }
        }
        return upperMostPoint;
    }

    public Double getLowerMostPoint(){
        Double lowerMostPoint = null;
        for(Point p : pointArr){
            if(lowerMostPoint == null || p.getY() < lowerMostPoint){
                lowerMostPoint = p.getY();
            }
        }
        return lowerMostPoint;
    }

    public Point getPointAhead(){
        return pointAhead;
    }
    //public 

    public ArrayList<Point> getPointArr(){
        return pointArr;
    }

    private void getRadius(){
        radius = Math.sqrt(xAddon*xAddon + yAddon*yAddon);
    }

    public void setActive(boolean isActive){
        this.isActive = isActive;
    }

    public boolean getActive(){
        return isActive;
    }

    public double getDirection(){
        return direction;
    }

    // provide buffered front-to-centre distance
    public double getForwardExtent() {
        return xAddon;
    }

    // provide centre-to-side distance for adjacent lane checks
    public double getLateralExtent() {
        return yAddon;
    }
}
