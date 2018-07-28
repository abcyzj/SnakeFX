package logic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import main.SceneController;
import network.Client;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Vector;

public class SlaveLogicController implements LogicController {
    private Canvas gameCanvas;
    private SceneController sceneController;
    private String host;
    private int port;
    private Client client;
    private Text infoLabel;
    private Channel channel;
    private Snake snakeA;
    private Snake snakeB;
    private Vector<Snake> snakes = new Vector<>();
    private Vector<Hole> holes = new Vector<>();
    private Vector<Egg> eggs = new Vector<>();
    private Vector<Bush> bushes = new Vector<>();

    public SlaveLogicController(Canvas gameCanvas, SceneController sceneController, Text infoLabel, String host, int port) {
        this.gameCanvas = gameCanvas;
        this.sceneController = sceneController;
        this.infoLabel = infoLabel;
        this.host = host;
        this.port = port;
        Constants.CANVAS_HEIGHT  = gameCanvas.getHeight();
        Constants.CANVAS_WIDTH = gameCanvas.getWidth();
        startClient();
    }

    @Override
    public void start() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void onKeyPressed(KeyEvent event) {

    }

    @Override
    public void setSpeed(int speed) {

    }

    @Override
    public void exit() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        client.close();
    }

    public void startClient() {
        client = new Client(this, host, port);
        ChannelFuture connectFuture = client.connect();
        connectFuture.addListener((ChannelFuture F) -> {
           if(F.isSuccess()) {
               infoLabel.setVisible(false);
           }
           else {
               infoLabel.setText("无法连接，请返回主页重试");
           }
        });
        infoLabel.setVisible(true);
        infoLabel.setText("正在连接");
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void onMsgReceived(JSONObject msg) {
        String type = (String)msg.get("type");
        switch(type) {
            case "static-pos":
                setStaticPos(msg);
                break;
            case "sync":
                syncCanvas(msg);
                break;
        }
    }

    private void setStaticPos(JSONObject msg) {
        holes.clear();
        bushes.clear();
        JSONArray JSONHoles = (JSONArray)msg.get("holes");
        for(int i = 0; i < JSONHoles.length(); i++) {
            JSONObject JSONHole = (JSONObject)JSONHoles.get(i);
            holes.add(new Hole(JSONHole.getDouble("X"), JSONHole.getDouble("Y")));
        }
        JSONArray JSONBushes = (JSONArray)msg.get("bushes");
        for(int i = 0; i < JSONBushes.length(); i++) {
            JSONObject JSONBush = (JSONObject)JSONBushes.get(i);
            bushes.add(new Bush(JSONBush.getDouble("X"), JSONBush.getDouble("Y")));
        }
        renderCanvas();
    }

    private void syncCanvas(JSONObject msg) {
        if(snakes.isEmpty()) {
            snakeA = new Snake(0, 0, Snake.Direction.RIGHT, Constants.SNAKE_A_COLOR);
            snakeB = new Snake(0, 0, Snake.Direction.RIGHT, Constants.SNAKE_B_COLOR);
            snakes.add(snakeA);
            snakes.add(snakeB);
        }

        snakeA.updateFromJSONObject(msg.getJSONObject("snakeA"));
        snakeB.updateFromJSONObject(msg.getJSONObject("snakeB"));

        eggs.clear();
        JSONArray JSONEggs = msg.getJSONArray("eggs");
        for(int i = 0; i < JSONEggs.length(); i++) {
            JSONObject JSONEgg = JSONEggs.getJSONObject(i);
            eggs.add(new Egg(JSONEgg.getDouble("X"), JSONEgg.getDouble("Y")));
        }
        renderCanvas();
    }

    private void renderCanvas() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        for(Snake snake: snakes) {
            snake.render(gc);
        }
        for(Hole hole: holes) {
            hole.render(gc);
        }
        for(Bush bush: bushes) {
            bush.render(gc);
        }
        for(Egg egg: eggs) {
            egg.render(gc);
        }
    }
}
