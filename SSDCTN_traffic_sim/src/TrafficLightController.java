import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public class TrafficLightController {

    private final List<TrafficLightObserver> northSouthLights = new ArrayList<>();
    private final List<TrafficLightObserver> eastWestLights = new ArrayList<>();

    // one group per direction - each direction gets its own green so turning cars
    // never have oncoming traffic (order: south, west, north, east)

    private final List<List<TrafficLightObserver>> directions = new ArrayList<>();
    private int currentDirection = 0;

    private Timer timer;
    private int trafficPhase = 0;
    
    // track active phase so resize doesn't reset it
    private int remainingDelay;
    private long timerStartedAt;
    private boolean paused;

    private final int GREEN = 0;
    private final int YELLOW = 1;
    private final int RED = 2;

    private final int GREEN_TIME = 10000; // 20 seconds I think these times work best, other times glitch out
    private final int YELLOW_TIME = 4500; // 4 seconds

    public TrafficLightController(
        TrafficLight trafficLight1,
        TrafficLight trafficLight2,
        TrafficLight trafficLight3,
        TrafficLight trafficLight4,
        TrafficLight trafficLight5,
        TrafficLight trafficLight6,
        TrafficLight trafficLight7,
        TrafficLight trafficLight8
    ) {

        // register the active lights as observers for travel direction
        registerObserver(northSouthLights, trafficLight2);
        registerObserver(northSouthLights, trafficLight3);
        registerObserver(northSouthLights, trafficLight6);
        registerObserver(northSouthLights, trafficLight7);
        registerObserver(eastWestLights, trafficLight1);
        registerObserver(eastWestLights, trafficLight4);
        registerObserver(eastWestLights, trafficLight5);
        registerObserver(eastWestLights, trafficLight8);

        // Initial states – starts with vertical traffic flow
        notifyObservers(eastWestLights, RED);
        notifyObservers(northSouthLights, GREEN);
        // register the active lights as observers, one direction each
        directions.add(List.of(trafficLight2));   // southbound
        directions.add(List.of(trafficLight4));   // westbound
        directions.add(List.of(trafficLight3));   // northbound
        directions.add(List.of(trafficLight1));   // eastbound

        // Starting state - everything red except the first direction
        for (List<TrafficLightObserver> lights : directions) {
            notifyObservers(lights, RED);
        }
        notifyObservers(directions.get(currentDirection), GREEN);

        //trafficLight5.setLightState(RED); 
        //trafficLight8.setLightState(RED);
        //trafficLight6.setLightState(GREEN);
        //trafficLight7.setLightState(GREEN);

        timer = new Timer(GREEN_TIME, e -> changePhase());

        // Timer only fires once
        timer.setRepeats(false);
    }


    public void start() {
        // System.out.println("TrafficLightController.start() called");
        startTimer(GREEN_TIME);
    }

    // remember unused portion of current phase 
    public void pause() {
        if (!paused && timer.isRunning()) {
            long elapsed = (System.nanoTime() - timerStartedAt) / 1_000_000L;
            remainingDelay = Math.max(1, remainingDelay - (int) elapsed);
            timer.stop();
            paused = true;
        }
    }

    // continue interrupted phase 
    public void resume() {
        if (paused) {
            paused = false;
            startTimer(remainingDelay);
        }
    }


    private void changePhase() {
        // System.out.println("TrafficLightController.changePhase() called");
        switch (trafficPhase) {

            // current direction GREEN -> YELLOW (everyone else is still red)
            case 0:
                notifyObservers(directions.get(currentDirection), YELLOW);
                trafficPhase = 1;
                startTimer(YELLOW_TIME);
                break;

            // current direction RED -> next direction GREEN
            case 1:
                notifyObservers(directions.get(currentDirection), RED);
                currentDirection = (currentDirection + 1) % directions.size();
                notifyObservers(directions.get(currentDirection), GREEN);
                trafficPhase = 0;
                startTimer(GREEN_TIME);
                break;
        }
    }


    private void startTimer(int delay) {
        // record countdown duration and start time 
        remainingDelay = delay;
        timerStartedAt = System.nanoTime();
        timer.setInitialDelay(delay);
        timer.restart();
    }

    
    // ----- Observer Pattern -----
    private void registerObserver(List<TrafficLightObserver> list, TrafficLight light) {
        list.add(light);
    }

    private void unregisterObserver(List<TrafficLightObserver> list, TrafficLight light) {
        list.remove(light);
    }

    private void notifyObservers(List<TrafficLightObserver> lights, int state) {
        // send one state change to every light in the selected direction
        for (TrafficLightObserver light : lights) {
            light.updateLightState(state);
        }
    }
}
