public class Edge {
    Point left;
    Point right;
    
    public Edge(Point left, Point right){
        this.left = left;
        this.right = right;
    }

    public boolean pointHasPassedEdge(Point p){
        double xDiff = right.getX() - left.getX();
        double yDiff = right.getY() - left.getY();
        double xDiffToPoint = p.getX() - left.getX();
        double yDiffToPoint = p.getY() - left.getY();
        double leftAngleToRight = Math.atan2(yDiff, xDiff); 
        leftAngleToRight = Math.toDegrees(leftAngleToRight);
        double leftAngleToPoint = Math.atan2(yDiffToPoint, xDiffToPoint);
        boolean inbetweenPoints;
        if((p.getX() > left.getX() && p.getX() < right.getX()) || (p.getX() < left.getX() && p.getX() > right.getX())){
            System.out.println("inbetween points");
            inbetweenPoints = true;
        }else{
            inbetweenPoints = false;
        }
        if((leftAngleToPoint < leftAngleToRight) && inbetweenPoints){
            return true;
        }
        return false;
    }

    public boolean pointIsOnFlatEdge(Point p, double direction){
        if(direction == 90.0 && p.getX() > left.getX() && p.getX() < right.getX() && p.getY() == left.getY()){
            return true;
        }
        if(direction == 270.0 && p.getX() < left.getX() && p.getX() > right.getX() && p.getY() == left.getY()){
            return true;
        }
        if(direction == 0.0 && p.getY() > left.getY() && p.getY() < right.getY() && p.getX() == left.getX()){
            return true;
        }
        if(direction == 180.0 && p.getY() < left.getY() && p.getY() > right.getY() && p.getX() == left.getX()){
            return true;
        }
        return false;
    }
}
