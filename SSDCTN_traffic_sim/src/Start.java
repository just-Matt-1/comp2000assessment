public class Start {
    private Point startPoint;
    private int ID;
    private static final int SPAWN_BUFFER = (int) ((App.width+App.height)/2 * 0.15);

    Start(Point startPoint, int ID){
        this.startPoint = addSpawnBufferToPoint(startPoint);
        this.ID = ID;
    }

    private Point addSpawnBufferToPoint(Point sp){
        double x = sp.getX()*App.width;
        double y = sp.getY()*App.height;

        if(sp.getX() == 0){
            x -= SPAWN_BUFFER;
        }else if(sp.getX() == 1){
            x += SPAWN_BUFFER;
        }

        if(sp.getY() == 0){
            y -= SPAWN_BUFFER;
        }else if(sp.getY() == 1){
            y += SPAWN_BUFFER;
        }
        return new Point(x, y);
    }

    public int getID(){
        return ID;
    }

    public Point getStartPoint(){
        return startPoint;
    }

    public void setStartPoint(Point p){
        startPoint = p;
    }
}
