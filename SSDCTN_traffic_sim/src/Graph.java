import java.util.ArrayList;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Arrays;


public class Graph {
    // setting up file reading
    private BufferedReader br;

    // arraylist of vertices, start points, and finish points - could be a stream
    private ArrayList<Vertex> vertexArr;
    private ArrayList<Vertex> startArr;
    private ArrayList<Vertex> finishArr;
    private HashMap<Vertex, Vertex> finishPointUnreachableByStartPoint;
    private Random rand;
    ArrayList<Integer> preferedFinish0 = new ArrayList<Integer>(Arrays.asList(32,32,32,32,32,32,32,31,31,31,31,31,31,33,34,35));
    ArrayList<Integer> preferedFinish1 = new ArrayList<Integer>(Arrays.asList(33,33,33,33,33,33,33,32,32,32,32,32,32,30,34,35));
    ArrayList<Integer> preferedFinish2 = new ArrayList<Integer>(Arrays.asList(30,30,30,30,30,30,30,35,35,35,35,35,35,34,33,31));
    ArrayList<Integer> preferedFinish3 = new ArrayList<Integer>(Arrays.asList(31,31,31,31,31,31,31,30,30,30,30,30,30,32,34,35));
    ArrayList<Integer> preferedFinish4 = new ArrayList<Integer>(Arrays.asList(35,35,35,35,35,35,35,32,32,32,32,32,32,30,31,33));
    ArrayList<Integer> preferedFinish5 = new ArrayList<Integer>(Arrays.asList(34,34,34,34,34,34,34,32,32,32,32,32,32,30,33,31));
    HashMap<Vertex, ArrayList<Integer>> mapStartToFinishPref = new HashMap<>();

    // constructor
    public Graph() throws IOException{
        // setting up the arrays
        vertexArr = new ArrayList<>();
        startArr = new ArrayList<>();
        finishArr = new ArrayList<>();
        // add start vertices to graph
        String vertexDesc;
        Vertex vertex;
        br = new BufferedReader(new FileReader("bin/resources/GraphStartVertices.txt"));
        br.readLine();
        while((vertexDesc = br.readLine()) != null){
            vertex = createStartVertexFromFile(vertexDesc);
            vertexArr.add(vertex);
            startArr.add(vertex);
        }
        // add turn vertices to graph
        br = new BufferedReader(new FileReader("bin/resources/GraphTurnVertices.txt"));
        br.readLine();
        while((vertexDesc = br.readLine()) != null){
            vertex = createTurnVertexFromFile(vertexDesc);
            vertexArr.add(vertex);
        }
        // add finish vertices to graph
        br = new BufferedReader(new FileReader("bin/resources/GraphFinishVertices.txt"));
        br.readLine();
        while((vertexDesc = br.readLine()) != null){
            vertex = createFinishVertexFromFile(vertexDesc);
            vertexArr.add(vertex);
            finishArr.add(vertex);
        }

        mapStartToFinishPref = mapStartToFinishWithPreference();

        finishPointUnreachableByStartPoint = mapFinishToStart();
        rand = new Random();

    }

    //maps every start point to their finish point
    private HashMap<Vertex, Vertex> mapFinishToStart(){
        HashMap<Vertex, Vertex> map = new HashMap<>();
        for(int i = 0; i < startArr.size() && i < finishArr.size(); i++){
            map.put(startArr.get(i), finishArr.get(i));
        }
        return map;
    }

    private HashMap<Vertex, ArrayList<Integer>> mapStartToFinishWithPreference(){
        HashMap<Vertex, ArrayList<Integer>> mapStartToFinishPref = new HashMap<>();
        mapStartToFinishPref.put(vertexArr.get(0), preferedFinish0);
        mapStartToFinishPref.put(vertexArr.get(1), preferedFinish1);
        mapStartToFinishPref.put(vertexArr.get(2), preferedFinish2);
        mapStartToFinishPref.put(vertexArr.get(3), preferedFinish3);
        mapStartToFinishPref.put(vertexArr.get(4), preferedFinish4);
        mapStartToFinishPref.put(vertexArr.get(5), preferedFinish5);
        return mapStartToFinishPref;
    }
    

    //create start vertices
    private Vertex createStartVertexFromFile(String vertexDesc){
        Vertex vertex;
        Start start;
        ArrayList<Integer> adjVertices = new ArrayList<>();
        String[] vertexDescStrings = vertexDesc.split(",");
        int ID = Integer.parseInt(vertexDescStrings[0]);
        double x = Double.parseDouble(vertexDescStrings[2]);
        double y = Double.parseDouble(vertexDescStrings[3]);
        for(int i = 4; i < vertexDescStrings.length; i++){
            adjVertices.add(Integer.parseInt(vertexDescStrings[i]));
        }
        start = new Start(new Point(x, y), ID);
        vertex = new Vertex(start, adjVertices);
        return vertex;
    }

    // create turn Vertices
    private Vertex createTurnVertexFromFile(String vertexDesc){
        Vertex vertex;
        Turn turn;
        ArrayList<Integer> adjVertices = new ArrayList<>();
        String[] vertexDescStrings = vertexDesc.split(",");
        int ID = Integer.parseInt(vertexDescStrings[0]);
        double x = Double.parseDouble(vertexDescStrings[2]);
        double y = Double.parseDouble(vertexDescStrings[3]);
        float radius = Float.parseFloat(vertexDescStrings[4]) * App.width;
        String direction = vertexDescStrings[5];
        int turnDirection = 0;
        switch (direction){
            case "left":
                turnDirection = 0;
                break;
            case "right":
                turnDirection = 1;
                break;
            case "straight":
                turnDirection = 2;
                break;
        }
        float startAngle = Float.parseFloat(vertexDescStrings[6]);
        for(int i = 7; i < vertexDescStrings.length; i++){
            adjVertices.add(Integer.parseInt(vertexDescStrings[i]));
        }
        turn = new Turn(ID, new Point(x, y), radius, turnDirection, startAngle);
        vertex = new Vertex(turn, adjVertices);
        return vertex;
    }

    // create finish vertices
    private Vertex createFinishVertexFromFile(String vertexDesc){
        Vertex vertex;
        Finish finish;
        ArrayList<Integer> adjVertices = new ArrayList<>();
        String[] vertexDescStrings = vertexDesc.split(",");
        int ID = Integer.parseInt(vertexDescStrings[0]);
        double x = Double.parseDouble(vertexDescStrings[2]);
        double y = Double.parseDouble(vertexDescStrings[3]);
        finish = new Finish(new Point(x, y), ID);
        vertex = new Vertex(finish, adjVertices);
        return vertex;
    }


    //gives a vehicle a random finish point based in their start point
    public Vertex getRandomFinish(Vertex start){
        /*Vertex nonUsableFinishPoint = finishPointUnreachableByStartPoint.get(start);
        int random = nonUsableFinishPoint.getID();
        while(random == nonUsableFinishPoint.getID()){
            random = rand.nextInt(30, 36);
        }
        Vertex finish = getVertex(random);*/

        int random = rand.nextInt(0, mapStartToFinishPref.get(start).size());
        Vertex finish = getVertex(mapStartToFinishPref.get(start).get(random));
        return finish;
    }

    public ArrayList<Integer> getAdjacentVertices(int ID){
        return getVertex(ID).getAdjVertexArr();

    }

    public Vertex getVertex(int ID){
        for(int i = 0; i < vertexArr.size(); i++){
            if(vertexArr.get(i).getTurn() != null && ID == vertexArr.get(i).getTurn().getID()){
                return vertexArr.get(i);
            }
            else if(vertexArr.get(i).getStart() != null && ID == vertexArr.get(i).getStart().getID()){
                return vertexArr.get(i);
            }
            else if(vertexArr.get(i).getFinish() != null && ID == vertexArr.get(i).getFinish().getID()){
                return vertexArr.get(i);
            }
        }
        return null;
    }

    public ArrayList<Vertex> getVertexArr(){
        return vertexArr;
    }

    public ArrayList<Vertex> getStartArr(){
        return startArr;
    }

    public ArrayList<Vertex> getFinishArr(){
        return finishArr;
    }
}
