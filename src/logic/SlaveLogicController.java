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
    private boolean aboutToExit = false;

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
        infoLabel.setVisible(false);
        JSONObject msg = new JSONObject();
        msg.put("type", "start");
        channel.writeAndFlush(msg.toString());
    }

    @Override
    public void pause() {
        infoLabel.setText("暂停中");
        infoLabel.setVisible(true);
        JSONObject msg = new JSONObject();
        msg.put("type", "pause");
        channel.writeAndFlush(msg.toString());
    }

    @Override
    public void onKeyPressed(KeyEvent event) {
        JSONObject msg = new JSONObject();
        msg.put("type", "keyPressed");
        switch(event.getCode()) {
            case UP:
                msg.put("key", "UP");
                break;
            case DOWN:
                msg.put("key", "DOWN");
                break;
            case LEFT:
                msg.put("key", "LEFT");
                break;
            case RIGHT:
                msg.put("key", "RIGHT");
                break;
        }
        channel.writeAndFlush(msg.toString());
    }

    @Override
    public void setSpeed(int speed) {
        JSONObject msg = new JSONObject();
        msg.put("type", "setSpeed");
        msg.put("speed", speed);
        channel.writeAndFlush(msg.toString());
    }

    @Override
    public void exit() {
        aboutToExit = true;
        client.close();
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
    }

    public void startClient() {
        client = new Client(this, host, port);
        ChannelFuture connectFuture = client.connect();
        connectFuture.addListener((ChannelFuture F) -> {
           if(F.isSuccess()) {
               infoLabel.setVisible(false);
               sceneController.enableGameButtons();
           }
           else {
               infoLabel.setText("无法连接，请返回主页重试");
               sceneController.getHomeBtn().setDisable(false);
           }
        });
        infoLabel.setVisible(true);
        infoLabel.setText("正在连接");
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void onMsgReceived(JSONObject msg) {
        if(aboutToExit) {
            return;
        }
        String type = msg.getString("type");
        switch(type) {
            case "static-pos":
                setStaticPos(msg);
                break;
            case "sync":
                syncCanvas(msg);
                break;
            case "setSpeed":
                sceneController.removeSliderEvents();
                sceneController.setSpeedSlider(msg.getInt("speed"));
                sceneController.initSliderEvents();
                break;
            case "pause":
                sceneController.togglePauseBtn(true);
                break;
            case "start":
                sceneController.togglePauseBtn(false);
                break;
            case "score":
                sceneController.notifyScore(msg.getInt("score"));
                break;
            case "snakeNum":
                sceneController.notifySnakeNum(msg.getInt("snakeNum"));
                break;
            case "inHole":
                sceneController.notifyInHole(msg.getBoolean("inHole"));
                break;
            case "result":
                String result = msg.getString("result");
                infoLabel.setVisible(true);
                sceneController.getPauseResumeBtn().setDisable(true);
                switch(result) {
                    case "draw":
                        infoLabel.setText("平局");
                        break;
                    case "win":
                        infoLabel.setText("您赢得了游戏");
                        break;
                    case "lose":
                        infoLabel.setText("很遗憾，您输掉了游戏");
                        break;
                }
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
