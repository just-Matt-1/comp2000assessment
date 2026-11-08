import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;

class Panel extends JPanel implements KeyListener {
    private int width, height;
    private boolean pausedForResize;

    // pothole values
    private static final int VEHICLES_BEFORE_POTHOLE = 8;
    private static final double POTHOLE_GROWTH_CHANCE = 0.20;
    private static final double INITIAL_POTHOLE_SIZE = 12;
    private static final long SPEED_CAMERA_FINE = 100;
    private static final long POTHOLE_REPAIR_REVENUE = 300;
    
    VehicleSpawner vehicleSpawner;
    Road roadN, roadE, roadMid, roadW, intersection, roadOnFarRight; // upper road
    Road roadE2, roadS2, roadW2, intersection2;   // lower road
    Pothole pothole;
    List<SpeedCamera> speedCameras;
    TrafficLight trafficLight1, trafficLight2, trafficLight3, trafficLight4, trafficLight5, trafficLight6, trafficLight7, trafficLight8;
    StopLine stopLine1, stopLine2, stopLine3, stopLine4;
    ArrayList<StopLine> stopLineArr = new ArrayList<>();
    // active explosions are temporary traffic-blocking
    private final List<Explosion> explosions = new ArrayList<>();
    TrafficLightController trafficLightController;
    BusStop busStop;
    Statistics stats;
    long explosionCounter = 0;
    private long speedCameraRevenue = 0;
    PoliceCar policeCar;

    Graph g;

    // panel creates VehicleSpawner, so also passes any invalid config up
    Panel(int w, int h) throws SimulationConfigurationException, Exception {

        
        this.width = w;
        this.height = h;
        this.setPreferredSize(new Dimension(width, height));
        this.setBackground(new Color(63, 155, 11));
        // remove any possible stale pixels
        this.setOpaque(true);
        this.setDoubleBuffered(true);

        this.addKeyListener(this);
        this.setFocusable(true);

        stats = new Statistics();

        g = new Graph();

        // ---------------------- Vehicle Spawn Timers ----------------------
        vehicleSpawner = new VehicleSpawner(25, g); // % chance
        Timer vehicleSpawnTimer = new Timer(1390, e -> {        // attempt freq
            // pause spawner when paused for resize 
            if (!pausedForResize) {
                vehicleSpawner.spawn();
            }
        });
        vehicleSpawnTimer.start();
        Timer vehicleDespawnTimer = new Timer(10000, e -> {
            // pause spawner when paused for resize
            if (!pausedForResize) {
                vehicleSpawner.despawn();
            }
        });
        vehicleDespawnTimer.start();

        
        // ---------------------- Vehicle Acceleration Timers ----------------------
        Timer vehicleAccelTimer = new Timer(1000, e -> {
            // pause acceleration when paused for resize
            if (pausedForResize) {
                return;
            }
            for (EntityStore<Vehicle> sublist : vehicleSpawner.getVehicleList()) {
                for (Vehicle vehicle : sublist.getEntities()) {

                    // Do not accelerate the bus while it is stopped at the bus stop
                    if (vehicle instanceof Bus && ((Bus) vehicle).isStoppedAtBusStop()){
                        continue;
                    }
                  
                    // Dont build up speed when movement is blocked
                    if (!vehicle.isMovementBlocked() && vehicle.velocity < vehicle.topSpeed) {
                        vehicle.accelerate(vehicle.accelerationRate);
                    }
                }
            }
        });
        vehicleAccelTimer.start(); 


        // ---------------------- Movement Timer ----------------------
        // Vehicle movement & repaint timer (~60fps)
        Timer moveTimer = new Timer(16, e -> {
            if (pausedForResize) {
                this.repaint();
                return;
            }

            CollisionDetection.changeTurningVehicleToDiffEntityStore(vehicleSpawner.getVehicleList(), vehicleSpawner.getMapLanes());
            //CollisionDetection.detectTurningHitboxOverLap();

            explosions.removeIf(Explosion::isFinished);
            Vehicle[] collision = CollisionDetection.findCollision(vehicleSpawner.getVehicleList());
            if (collision != null) {
                createExplosion(collision[0], collision[1]);
                explosionCounter++;
            }
            
            // build each vehicles movement before applying distances
            Map<Vehicle, Double> desiredMovements = new IdentityHashMap<>();
            Map<Vehicle, Boolean> environmentBlocks = new IdentityHashMap<>();
            for (EntityStore<Vehicle> sublist : vehicleSpawner.getVehicleList()) {
                for (Vehicle vehicle : sublist.getEntities()) {
                    //Bus stop behaviour
                    if (vehicle instanceof Bus){
                        Bus bus = (Bus) vehicle;

                        bus.checkBusStop(busStop);
                        bus.waitAtBusStop();
                        bus.leaveBusStop();
                    }
                    
                    // Check whether the vehicle should stop at a traffic light.
                    boolean stopAtLight = shouldStopAtRed(vehicle);

                    // Speeding cars use their own traffic light strategy.
                    if (vehicle instanceof SpeedingCar) {
                        SpeedingCar speedingCar = (SpeedingCar) vehicle;
                        stopAtLight = speedingCar.shouldStopAtTrafficLight(stopAtLight);
                       
                    }

                    // Keep the existing pothole and explosion blocking rules.
                    boolean environmentBlocked = stopAtLight
                        || pothole.shouldBlock(vehicle)
                        || isBlockedByExplosion(vehicle);

                    environmentBlocks.put(vehicle, environmentBlocked);
                    desiredMovements.put(
                        vehicle,
                        // keep normal speed; rendering quality handles visual smoothing separately
                        environmentBlocked ? 0 : vehicle.velocity
                    );
                }
            }

            // let queues match movement ahead instead of switching between stopping and full speed
            Map<Vehicle, Double> allowedMovements = CollisionDetection.calculateAllowedMovements(
                vehicleSpawner.getVehicleList(),
                desiredMovements
            );

            // handle rear-end collisions cased by speeding cars
            Vehicle[] rearEndCollision = CollisionDetection.getRearEndCollision();
            
            if (rearEndCollision != null){
                createExplosion(rearEndCollision[0], rearEndCollision[1]);
                explosionCounter++;
            }

            // only one vehicle can be swallowed so a temporary list is not needed
            Vehicle swallowedVehicle = null;
            for (EntityStore<Vehicle> sublist : vehicleSpawner.getVehicleList()) {
                for (Vehicle vehicle : sublist.getEntities()) {
                    double allowedMovement = allowedMovements.getOrDefault(vehicle, 0.0);
                    // pause acceleration whenever an obstacle limits the desired movement
                    double desiredMovement = desiredMovements.getOrDefault(vehicle, 0.0);
                    boolean movementBlocked = environmentBlocks.getOrDefault(vehicle, false)
                        || allowedMovement + 0.0001 < desiredMovement;
                    vehicle.setMovementBlocked(movementBlocked);
                    //CollisionDetection.detectHitboxCollision(vehicleSpawner.getVehicleList());

                    if (allowedMovement > 0 && !vehicle.isMovementBlocked()) {
                        vehicle.move(allowedMovement);
                    }
                    vehicle.updateHitBox();

                    // speeding vehicles are fined
                    for (SpeedCamera camera : speedCameras) {
                        if (camera.checkSpeed(vehicle)) {
                            speedCameraRevenue += SPEED_CAMERA_FINE;
                            if (speedCameraRevenue >= POTHOLE_REPAIR_REVENUE) {
                                pothole.remove();
                            }
                        }
                    }

                    // let pothole count pass and flag swallowing events
                    if (pothole.update(vehicle)) {
                        swallowedVehicle = vehicle;
                    }
                }
            }


            /*CollisionDetection.detectHitboxCollision(vehicleSpawner.getVehicleList());
            for(int i = 0; i < vehicleSpawner.getVehicleList().size(); i++){
                for(int j = 0; j < vehicleSpawner.getVehicleList().get(i).size(); j++){
                    if(!vehicleSpawner.getVehicleList().get(i).get(j).isMovementBlocked()){
                        vehicleSpawner.getVehicleList().get(i).get(j).move(3);
                    }
                    else{
                        vehicleSpawner.getVehicleList().get(i).get(j).move(0);
                    }
                }
            }*/

            // remove the swallowed vehicle after movement iteration finishes
            if (swallowedVehicle != null) {
                for (EntityStore<Vehicle> sublist : vehicleSpawner.getVehicleList()) {
                    sublist.remove(swallowedVehicle);
                }
            }

            stats.inputTotalCount(vehicleSpawner.getTotalVehicleCount());
            stats.updateCurVehicleCount(vehicleSpawner.getVehicleStream());
            stats.calculateAvgVelocity(vehicleSpawner.getVehicleStream());
            stats.inputExplosionCount(explosionCounter);
            stats.inputSpeedCameraRevenue(speedCameraRevenue);

            this.repaint();
        });
        moveTimer.start();

        
        // ---------------------- Static objects ----------------------
        // road positions and dimensions are specified in updateViewport()
        roadN = new Road(1);
        roadE = new Road(2);
        roadMid = new Road(1);
        roadW = new Road(2);
        roadE2 = new Road(2);
        roadS2 = new Road(1);
        roadW2 = new Road(2);
        intersection = new Road(0);
        intersection2 = new Road(0);

        // draw stop line only at end facing intersection.. idk man 
        roadN.setStopLines(false, true);
        roadE.setStopLines(true, false);
        roadE2.setStopLines(true, false);
        roadMid.setStopLines(true, true);
        roadW.setStopLines(false, true);
        roadW2.setStopLines(false, true);
        roadS2.setStopLines(true, false);

        this.trafficLight1 = new TrafficLight(250, 10);  // top left
        this.trafficLight2 = new TrafficLight(500, 10);  // top right
        this.trafficLight3 = new TrafficLight(250, 305); // bottom left
        this.trafficLight4 = new TrafficLight(500, 305); // bottom right
        this.trafficLight5 = new TrafficLight(250, 405);  // top left
        this.trafficLight6 = new TrafficLight(500, 405);  // top right
        this.trafficLight7 = new TrafficLight(250, 700); // bottom left
        this.trafficLight8 = new TrafficLight(500, 700); // bottom right
        trafficLightController = new TrafficLightController(
           trafficLight1,
           trafficLight2,
           trafficLight3,
           trafficLight4,
           trafficLight5,
           trafficLight6,
           trafficLight7,
           trafficLight8
        );
        trafficLightController.start();
       
        this.busStop = new BusStop(150, 310);//bottom left bus stop

        // place pothole halfway along road
        pothole = new Pothole(
            width * 0.57,
            height * 0.6,
            INITIAL_POTHOLE_SIZE,
            INITIAL_POTHOLE_SIZE,
            VEHICLES_BEFORE_POTHOLE,
            POTHOLE_GROWTH_CHANCE
        );
        // one camera per horizontal lane, all lined up at the same x
        speedCameras = List.of(
            new SpeedCamera(610, 40, 144),    // top road very top
            new SpeedCamera(610, 320, 256),   // top road next to the police car ( the orignal one)
            new SpeedCamera(610, 430, 544),   // bottom road
            new SpeedCamera(610, 720, 656)    // very bottom road ... hope this makes sense
        );
        policeCar = new PoliceCar(new Point(700, 340), 30, 50, 180);
        policeCar.setPosition(700, 340);
        //explosion = new Explosion(200, 550, this);  // pass panel for callbacks/repaint
        

    }

    public void beginResizePause() {
        if (!pausedForResize) {
            pausedForResize = true;
            trafficLightController.pause();
        }
        repaint();
    }

    public void endResizePause() {
        if (pausedForResize) {
            pausedForResize = false;
            trafficLightController.resume();
            repaint();
        }
    }

    private boolean shouldStopAtRed(Vehicle vehicle) {
        // check each intersection against its own lights + stop-line positions
        return shouldStopAtIntersection(vehicle, 0.25,
                    trafficLight1, trafficLight2, trafficLight3, trafficLight4)
                || shouldStopAtIntersection(vehicle, 0.75,
                    trafficLight5, trafficLight6, trafficLight7, trafficLight8);
    }

    // share rules with lower intersection lol
    private boolean shouldStopAtIntersection(Vehicle vehicle, double centreY,
            TrafficLight eastLight, TrafficLight southLight,
            TrafficLight northLight, TrafficLight westLight) {
        Point p = vehicle.hitBox.getPointAhead();

        double x = p.getX();
        double y = p.getY();

        int direction = (int) Math.round(vehicle.getDirection());

        // offset stop lines
        double topIntersection = height * (centreY - 0.125);
        double bottomIntersection = height * (centreY + 0.125);
        double leftIntersection = width * 0.375;
        double rightIntersection = width * 0.625;

        double detectionDistance = 15;

        // vehicle travelling SOUTH
        if (direction == 90 && x >= leftIntersection && x <= rightIntersection) {
            if (southLight.getLightState() == 2 || southLight.getLightState() == 1) {
                return y >= topIntersection - detectionDistance
                        && y < topIntersection;
            }
        }

        // vehicle travelling NORTH including the lower bottom-left signal
        if (direction == 270 && x >= leftIntersection && x <= rightIntersection) {
            if (northLight.getLightState() == 2 || northLight.getLightState() == 1) {
                return y <= bottomIntersection + detectionDistance
                        && y > bottomIntersection;
            }
        }

        // limit WEST checks to this road so the other intersection light cannot stop it
        if (direction == 180 && y >= topIntersection && y <= bottomIntersection) {
            if (westLight.getLightState() == 2 || westLight.getLightState() == 1) {
                return x <= rightIntersection + detectionDistance
                        && x > rightIntersection;
            }
        }

        // limit EAST checks to this road and use its own approach signal
        if (direction == 0 && y >= topIntersection && y <= bottomIntersection) {
            if (eastLight.getLightState() == 2 || eastLight.getLightState() == 1) {
                return x >= leftIntersection - detectionDistance
                        && x < leftIntersection;
            }
        }

        return false;
    }

    private void createExplosion(Vehicle firstVehicle, Vehicle secondVehicle) {
        // place explosion halfway between vehicles then remove them 
        double x = (firstVehicle.getPosition().getX() + secondVehicle.getPosition().getX()) / 2;
        double y = (firstVehicle.getPosition().getY() + secondVehicle.getPosition().getY()) / 2;
        explosions.add(new Explosion((int) x, (int) y, this));
        removeVehicle(firstVehicle);
        removeVehicle(secondVehicle);
    }

    private void removeVehicle(Vehicle vehicle) {
        for (EntityStore<Vehicle> lane : vehicleSpawner.getVehicleList()) {
            lane.remove(vehicle);
        }
    }

    private boolean isBlockedByExplosion(Vehicle vehicle) {
        // stop vehicles near explosion 
        for (Explosion explosion : explosions) {
            if (explosion.blocks(vehicle.getPosition())
                    || explosion.blocks(vehicle.getHitBox().getPointAhead())) {
                return true;
            }
        }
        return false;
    }

    // extend road and intersection to resized window 
    private void updateViewport(double left, double top, double right, double bottom) {
        double intersectionLeft1 = width * 0.375;
        double intersectionRight1 = width * 0.625;
        double intersectionTop1 = height * (0.25-0.125);
        double intersectionBottom1 = height * (0.25+0.125);

        double intersectionLeft2 = width * 0.375;
        double intersectionRight2 = width * 0.625;
        double intersectionTop2 = height * (0.75-0.125);
        double intersectionBottom2 = height * (0.75+0.125);

        double roadWidth = width * 0.25;
        double roadHeight = height * 0.25;

        // intersections
        intersection.setBounds(intersectionLeft1, intersectionTop1, roadWidth, roadHeight);
        intersection2.setBounds(intersectionLeft2, intersectionTop2, roadWidth, roadHeight);

        // vertical roads
        roadN.setBounds(intersectionLeft1, top, roadWidth, intersectionTop1 - top);
        roadMid.setBounds(intersectionLeft1, intersectionBottom1, roadWidth, intersectionTop2 - intersectionBottom1);
        roadS2.setBounds(intersectionLeft2, intersectionBottom2, roadWidth, bottom - intersectionBottom2);

        // horizontal roads
        roadW.setBounds(left, intersectionTop1, intersectionLeft1 - left, roadHeight);
        roadW2.setBounds(left, intersectionTop2, intersectionLeft2 - left, roadHeight);
        roadE.setBounds(intersectionRight1, intersectionTop1, right - intersectionRight1, roadHeight);
        roadE2.setBounds(intersectionRight2, intersectionTop2, right - intersectionRight2, roadHeight);

        // spawning and despawning
        vehicleSpawner.updateViewportBounds(left, top, right, bottom);
        stats.updateViewportBounds(width, height);
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);            // paints JPanel stuff like the background
        // draw disposable graphics copy so swing one is not changed
        Graphics2D g2d = (Graphics2D) g.create();
        // preserve curves and images when assets are enlarged etc 
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // use one uniform scale so all assets keep proportions
        double scale = Math.min(getWidth() / (double) width, getHeight() / (double) height);

        // centre simulation when window and simulation have different aspect ratios
        double xOffset = (getWidth() - width * scale) / 2.0;
        double yOffset = (getHeight() - height * scale) / 2.0;

        // convert panel edges into coords 
        double viewportLeft = -xOffset / scale;
        double viewportTop = -yOffset / scale;
        double viewportRight = (getWidth() - xOffset) / scale;
        double viewportBottom = (getHeight() - yOffset) / scale;

        // synchronise all size dependent simulation bounds before resizing
        updateViewport(viewportLeft, viewportTop, viewportRight, viewportBottom);

        Graphics2D g2dUncentred = (Graphics2D) g2d.create();    // for components that should be placed relative to 0,0

        // view transform only; physics are in logical units
        g2d.translate(xOffset, yOffset);
        g2d.scale(scale, scale);
        g2dUncentred.scale(scale, scale);

        // draw components
        roadW.draw(g2d);
        roadMid.draw(g2d);
        roadE.draw(g2d);
        roadN.draw(g2d);
        intersection.draw(g2d);
        intersection2.draw(g2d);
        roadW2.draw(g2d);
        roadS2.draw(g2d);
        roadE2.draw(g2d);

        pothole.draw(g2d);
        speedCameras.forEach(camera -> camera.draw(g2d));
        trafficLight1.draw(g2d);
        trafficLight2.draw(g2d);
        trafficLight3.draw(g2d);
        trafficLight4.draw(g2d);
        trafficLight5.draw(g2d);
        trafficLight6.draw(g2d);
        trafficLight7.draw(g2d);
        trafficLight8.draw(g2d);
        busStop.draw(g2d);
        policeCar.draw(g2d);
        //explosion.draw(g2d);    // test explosion !!!!! remove this to not show explosion :(
        

        vehicleSpawner.getVehicleStream().forEach(v -> v.draw(g2d));

        for (Explosion explosion : explosions) {
            explosion.draw(g2d);
        }

        stats.draw(g2dUncentred);

        // release graphics copy
        g2d.dispose();
        Toolkit.getDefaultToolkit().sync();

        // prevent pause notice from looking weird 
        if (pausedForResize) {
            drawPauseOverlay((Graphics2D) g);
        }
    }

    // draw pause overlay
    private void drawPauseOverlay(Graphics2D g) {
        Graphics2D overlay = (Graphics2D) g.create();
        overlay.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // dim background
        overlay.setColor(new Color(0, 0, 0, 105));
        overlay.fillRect(0, 0, getWidth(), getHeight());

        int cardWidth = Math.min(330, Math.max(230, getWidth() - 40));
        int cardHeight = 116;
        int cardX = (getWidth() - cardWidth) / 2;
        int cardY = (getHeight() - cardHeight) / 2;

        // draw pause card
        overlay.setColor(new Color(24, 29, 36, 235));
        overlay.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 22, 22);
        overlay.setColor(new Color(255, 255, 255, 210));
        overlay.setStroke(new BasicStroke(2f));
        overlay.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 22, 22);

        // draw pause symbol
        int pauseX = cardX + 28;
        int pauseY = cardY + 28;
        overlay.setColor(Color.WHITE);
        overlay.fillRoundRect(pauseX, pauseY, 12, 38, 4, 4);
        overlay.fillRoundRect(pauseX + 22, pauseY, 12, 38, 4, 4);

        overlay.setFont(getFont().deriveFont(Font.BOLD, 17f));
        overlay.drawString("SIMULATION PAUSED", cardX + 80, cardY + 48);
        overlay.setFont(getFont().deriveFont(Font.PLAIN, 13f));
        overlay.setColor(new Color(220, 225, 230));
        overlay.drawString("Window is being resized.", cardX + 80, cardY + 73);
        overlay.dispose();
    }

    // Key stroke listeners
    @Override public void keyPressed(KeyEvent e) {
        // System.out.println("Key pressed: " + e.getKeyChar());
        if (e.getKeyCode() == KeyEvent.VK_S) stats.keyPressed();
        if (e.getKeyCode() == KeyEvent.VK_R) {
            explosionCounter = 0;
            speedCameraRevenue = 0;
            vehicleSpawner.resetVehicleCount();
            stats.reset();
        }
    }

    // Not used rn but KeyListener interface requires implementations
    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}
}
