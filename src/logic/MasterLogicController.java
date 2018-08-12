package logic;

/*
 * 主机逻辑控制器类
 */

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.util.Duration;
import main.SceneController;
import network.Server;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;

public class MasterLogicController implements LogicController {
    private Canvas gameCanvas;
    private Text infoLabel;
    private SceneController sceneController;
    private Timeline timeline;
    private static final double INIT_FRAME_TIME = 10;
    private double frameTime = 10;
    private Snake snakeA;
    private Snake snakeB;
    private Player playerA;
    private Player playerB;
    private Vector<Snake> snakes;
    private Vector<Hole> holes;
    private Vector<Egg> eggs;
    private Vector<Bush> bushes;
    private Vector<Explosion> explosions;
    private Server server;
    private Channel channel;
    private enum State {INIT, LISTENING, LISTEN_FAILED, IN_GAME, GAME_OVER, ABOUT_TO_EXIT}
    private State state = State.INIT;

    public MasterLogicController(Canvas gameCanvas, Text infoLabel, SceneController sceneController) {
        this.gameCanvas = gameCanvas;
        this.infoLabel = infoLabel;
        gameCanvas.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        this.sceneController = sceneController;
        Constants.CANVAS_HEIGHT = gameCanvas.getHeight();
        Constants.CANVAS_WIDTH = gameCanvas.getWidth();
        initSprites();
        initPlayers();
        startServer();
    }

    // 初始化游戏场景中的物件
    private void initSprites() {
        snakes = new Vector<>();
        holes = new Vector<>();
        eggs = new Vector<>();
        bushes = new Vector<>();
        explosions = new Vector<>();

        for(int i = 0; i < Constants.HOLE_NUM; i++) {
            Hole newHole;
            while(true) {
                double X = Math.random()*Constants.CANVAS_WIDTH;
                double Y = Math.random()*Constants.CANVAS_HEIGHT;
                newHole = new Hole(X, Y);
                boolean ok = true;
                for(Hole hole: holes) {
                    if(hole.overlaps(newHole)) {
                        ok = false;
                        break;
                    }
                }
                if(ok) {
                    break;
                }
            }
            holes.add(newHole);
        }

        Collections.shuffle(holes);
        Random rand = new Random();
        snakeA = new Snake(holes.elementAt(0), Snake.Direction.values()[rand.nextInt(Snake.Direction.values().length)], Constants.SNAKE_A_COLOR);
        snakeB = new Snake(holes.elementAt(1), Snake.Direction.values()[rand.nextInt(Snake.Direction.values().length)], Constants.SNAKE_B_COLOR);
        snakes.add(snakeA);
        snakes.add(snakeB);

        for(int i = 0; i < Constants.BUSH_WALL_NUM; i++) {
            Bush lastBush;
            while(true) {
                lastBush = new Bush(Math.random()*Constants.CANVAS_WIDTH, Math.random()*Constants.CANVAS_HEIGHT);
                for(Hole hole: holes) {
                    if(hole.overlaps(lastBush)) {
                        continue;
                    }
                }
                break;
            }
            bushes.add(lastBush);

            double[] dx = {0, 0, -Bush.bushSize, Bush.bushSize};
            double[] dy = {-Bush.bushSize, Bush.bushSize, 0, 0};
            for(int j = 0; j < Constants.MAX_BUSH_PER_WALL; j++) {
                int dir = rand.nextInt(4);
                Bush newBush = new Bush(lastBush.getPos().getX() + dx[dir], lastBush.getPos().getY() + dy[dir]);
                if(newBush.outOfCanvas()) {
                    continue;
                }
                boolean ok = true;
                for(Hole hole: holes) {
                    if(hole.overlaps(newBush)) {
                        ok = false;
                        break;
                    }
                }
                for(Bush bush: bushes) {
                    if(bush.overlaps(newBush)) {
                        ok = false;
                        break;
                    }
                }
                if(ok) {
                    lastBush = newBush;
                    bushes.add(newBush);
                }
            }
        }

        genEggs();
    }

    private void initPlayers() {
        playerA = new Player();
        playerB = new Player();
    }

    // 游戏场景更新函数
    private void updateCanvas() {
        tackleHole();
        tackleEgg();
        tackleCollision();
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        for(Snake snake: snakes) {
            snake.render(gc);
            snake.update();
        }
        for(Hole hole: holes) {
            hole.render(gc);
            hole.update();
        }
        for(Bush bush: bushes) {
            bush.render(gc);
            bush.update();
        }
        for(Egg egg: eggs) {
            egg.render(gc);
            egg.update();
        }
        for(Explosion explosion: explosions) {
            explosion.render(gc);
            explosion.update();
        }
        syncCanvas();
    }

    // 发送游戏场景信息给网络通信模块
    private void syncCanvas() {
        JSONObject info = new JSONObject();
        info.put("type", "sync");
        info.put("snakeA", snakeA.toJSONObject());
        info.put("snakeB", snakeB.toJSONObject());
        JSONArray JSONEggs = new JSONArray();
        for(Egg egg: eggs) {
            JSONEggs.put(egg.toJSONObject());
        }
        info.put("eggs", JSONEggs);
        JSONArray JSONExplosions = new JSONArray();
        for(Explosion explosion: explosions) {
            JSONExplosions.put(explosion.toJSONObject());
        }
        info.put("explosions", JSONExplosions);
        channel.writeAndFlush(info.toString());
    }

    private void tackleHole() {
        boolean inHole = false;
        for(Snake snake: snakes) {
            if(snake.getCurHole() != null) {//目前在洞中
                inHole = true;
                if(!snake.mayGetOutOfHole()) {
                    continue;
                }
                for(Hole hole: holes) {
                    if(snake.getCurHole() != hole) {
                        snake.getOutOfHole(hole);
                        break;
                    }
                }
            }
            else {
                for(Hole hole: holes) {
                    if(hole.isInHole(snake.getHead())) {
                        snake.getInHole(hole);
                    }
                }
            }
        }
        sceneController.notifyInHole(inHole);
        JSONObject msg = new JSONObject();
        msg.put("type", "inHole");
        msg.put("inHole", inHole);
        channel.writeAndFlush(msg.toString());
    }

    private void tackleEgg() {
        for(Snake snake: snakes) {
            for(Egg egg: eggs) {
                if(egg.isInEgg(snake.getHead())) {
                    snake.eatAnEgg();
                    if(snake == snakeA) {
                        playerA.score += 1;
                    }
                    else {
                        playerB.score += 1;
                    }
                    eggs.remove(egg);
                    if(eggs.isEmpty()) {
                        Timeline genEggTimeline = new Timeline(new KeyFrame(
                                Duration.millis(Constants.GEN_EGG_TIME),
                                ae -> genEggs()
                        ));
                        genEggTimeline.play();
                    }
                    break;
                }
            }
        }
        sceneController.notifyScore(playerA.score);
        notifyOpponentScore(playerB.score);
    }

    private void genEggs() {
        for(int i = 0; i < 2; i++) {
            double X, Y;
            Egg newEgg;
            while(true) {
                X = Math.random()*Constants.CANVAS_WIDTH;
                Y = Math.random()*Constants.CANVAS_HEIGHT;
                newEgg = new Egg(X, Y);
                boolean ok = true;
                for(Bush bush: bushes) {
                    if(bush.overlaps(newEgg)) {
                        ok = false;
                        break;
                    }
                }
                for(Hole hole: holes) {
                    if(hole.overlaps(newEgg)) {
                        ok = false;
                        break;
                    }
                }
                if(ok) {
                    break;
                }
            }
            eggs.add(newEgg);
        }
    }

    private void notifyOpponentScore(int score) {
        JSONObject msg = new JSONObject();
        msg.put("type", "score");
        msg.put("score", score);
        channel.writeAndFlush(msg.toString());
    }

    // 处理碰撞
    private void tackleCollision() {
        Vector<Snake> deadSnakes = new Vector<>();
        for(Snake snakeA: snakes) {
            if(snakeA.getCurHole() != null) {//在洞中的蛇不可能碰撞
                continue;
            }
            for(Snake snakeB: snakes) {
                if(snakeB.inMyBody(snakeA.getHead())) {
                    deadSnakes.add(snakeA);
                    break;
                }
            }

            for(Bush bush: bushes) {
                if(bush.inside(snakeA.getHead())) {
                    deadSnakes.add(snakeA);
                    break;
                }
            }
        }
        for(Snake deadSnake: deadSnakes) {
            addExplosion(deadSnake.getHead());
            revive(deadSnake);
        }
    }

    private void addExplosion(Point2D pos) {
        Explosion newExplosion = new Explosion(pos.getX(), pos.getY());
        explosions.add(newExplosion);
        Timeline removeTimeline = new Timeline(new KeyFrame(
                Duration.millis(Constants.EXPLOSION_TIME),
                ae -> explosions.remove(newExplosion)
        ));
        removeTimeline.play();
    }

    // 复活蛇
    private Snake revive(Snake oldSnake) {
        snakes.remove(oldSnake);
        Random rand = new Random();
        Hole birthHole = holes.elementAt(rand.nextInt(holes.size()));
        Snake.Direction[] directions = Snake.Direction.values();
        Snake.Direction direction = directions[rand.nextInt(directions.length)];
        Snake newSnake = new Snake(birthHole, direction, oldSnake.getColor());
        if(snakeA == oldSnake) {
            snakeA = newSnake;
            playerA.remainingSnakes -= 1;
        }
        else {
            snakeB = newSnake;
            playerB.remainingSnakes -= 1;
        }
        snakes.add(newSnake);
        if(playerA.remainingSnakes < 0 || playerB.remainingSnakes < 0) {
            tackleWin();
        }
        else {
            sceneController.notifySnakeNum(playerA.remainingSnakes);
            notifyOpponentSnakeNum(playerB.remainingSnakes);
        }
        return newSnake;
    }

    private void tackleWin() {
        state = State.GAME_OVER;
        pause();
        infoLabel.setVisible(true);
        infoLabel.setText("胜负已分");
        sceneController.getPauseResumeBtn().setDisable(true);
        sceneController.getSpeedSlider().setDisable(true);
        sceneController.getChatInputField().setDisable(true);
        sceneController.getChatSendBtn().setDisable(true);
        JSONObject msg = new JSONObject();
        msg.put("type", "result");
        if(playerA.remainingSnakes < 0 && playerB.remainingSnakes < 0) {
            infoLabel.setText("平局");
            msg.put("result", "draw");
        }
        else if(playerA.remainingSnakes < 0) {
            infoLabel.setText("很遗憾，您输掉了游戏");
            msg.put("result", "win");
        }
        else if(playerB.remainingSnakes < 0) {
            infoLabel.setText("您赢得了游戏");
            msg.put("result", "lose");
        }
        channel.writeAndFlush(msg.toString());
    }

    private void notifyOpponentSnakeNum(int snakeNum) {
        JSONObject msg = new JSONObject();
        msg.put("type", "snakeNum");
        msg.put("snakeNum", snakeNum);
        channel.writeAndFlush(msg.toString());
    }

    private void startServer() {
        server = new Server(this);
        ChannelFuture listenFuture = server.listen(0);
        listenFuture.addListener((ChannelFuture F) -> {
            if(F.isSuccess()) {
                InetSocketAddress address = (InetSocketAddress) F.channel().localAddress();
                String IP = server.getLocalIP();
                infoLabel.setText("正在端口" + address.getPort() + "上监听\n" + "IP:" + IP);
                state = State.LISTENING;
            }
            else {
                infoLabel.setText("无法监听，请返回主页重试");
                state = State.LISTEN_FAILED;
            }
        });
        infoLabel.setVisible(true);
        infoLabel.setText("准备监听");
        sceneController.getHomeBtn().setDisable(false);
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void startGame() {
        state = State.IN_GAME;
        sceneController.getSnakeNumLabel().setVisible(true);
        sceneController.getScoreLabel().setVisible(true);
        sceneController.getChatSendBtn().setDisable(false);
        sceneController.getChatInputField().setDisable(false);
        channel.writeAndFlush(getStaticPos().toString());
        start();
    }

    //返回静态资源（除蛇、蛋以外的物体）
    private JSONObject getStaticPos() {
        JSONObject info = new JSONObject();
        info.put("type", "static-pos");
        JSONArray JSONHoles = new JSONArray();
        for(Hole hole: holes) {
            JSONHoles.put(hole.toJSONObject());
        }
        info.put("holes", JSONHoles);
        JSONArray JSONBushes = new JSONArray();
        for(Bush bush: bushes) {
            JSONBushes.put(bush.toJSONObject());
        }
        info.put("bushes", JSONBushes);
        return info;
    }

    public void start() {
        startWithoutSending();

        if(channel != null) {
            JSONObject msg = new JSONObject();
            msg.put("type", "start");
            channel.writeAndFlush(msg.toString());
        }
    }

    // 开始游戏，但不向网络中发送开始信息
    private void startWithoutSending() {
        timeline = new Timeline(new KeyFrame(
                Duration.millis(frameTime),
                ae -> updateCanvas()
        ));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        sceneController.enableGameButtons();
        infoLabel.setVisible(false);
    }

    public void pause() {
        pauseWithoutSending();

        if(channel != null) {
            JSONObject msg = new JSONObject();
            msg.put("type", "pause");
            channel.writeAndFlush(msg.toString());
        }
    }

    private void pauseWithoutSending() {
        if(timeline != null) {
            timeline.pause();
        }
        infoLabel.setText("暂停中");
        infoLabel.setVisible(true);
    }

    // 退出游戏，清理场景并关闭网络通信线程
    public void exit() {
        state = State.ABOUT_TO_EXIT;
        if(timeline != null) {
            timeline.stop();
        }
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        server.close();
    }

    public void setSpeed(int speed) {
        setSpeedWithoutSend(speed);

        JSONObject msg = new JSONObject();
        msg.put("type", "setSpeed");
        msg.put("speed", speed);
        channel.writeAndFlush(msg.toString());
    }

    public void setSpeedWithoutSend(int speed) {
        frameTime = INIT_FRAME_TIME/(1 + 0.2*speed);
        if(timeline.getStatus() == Animation.Status.RUNNING) {
            pause();
            start();
        }
        sceneController.removeSliderEvents();
        sceneController.setSpeedSlider(speed);
        sceneController.initSliderEvents();
    }

    public void onKeyPressed(KeyEvent event) {
        switch(event.getCode()) {
            case W:
                if(snakeA.direction != Snake.Direction.DOWN) {
                    snakeA.direction = Snake.Direction.UP;
                }
                break;
            case S:
                if(snakeA.direction != Snake.Direction.UP) {
                    snakeA.direction = Snake.Direction.DOWN;
                }
                break;
            case A:
                if(snakeA.direction != Snake.Direction.RIGHT) {
                    snakeA.direction = Snake.Direction.LEFT;
                }
                break;
            case D:
                if(snakeA.direction != Snake.Direction.LEFT) {
                    snakeA.direction = Snake.Direction.RIGHT;
                }
                break;
        }
    }

    private void onOpponentKeyPressed(String key) {
        switch(key) {
            case "UP":
                if(snakeB.direction != Snake.Direction.DOWN) {
                    snakeB.direction = Snake.Direction.UP;
                }
                break;
            case "DOWN":
                if(snakeB.direction != Snake.Direction.UP) {
                    snakeB.direction = Snake.Direction.DOWN;
                }
                break;
            case "LEFT":
                if(snakeB.direction != Snake.Direction.RIGHT) {
                    snakeB.direction = Snake.Direction.LEFT;
                }
                break;
            case "RIGHT":
                if(snakeB.direction != Snake.Direction.LEFT) {
                    snakeB.direction = Snake.Direction.RIGHT;
                }
                break;
        }
    }

    public void onMessageReceived(JSONObject msg) {
        if(state == State.ABOUT_TO_EXIT || state == State.GAME_OVER) {
            return;
        }
        String type = msg.getString("type");
        switch(type) {
            case "start":
                startWithoutSending();
                sceneController.togglePauseBtn(false);
                break;
            case "pause":
                pauseWithoutSending();
                sceneController.togglePauseBtn(true);
                break;
            case "setSpeed":
                int speed = msg.getInt("speed");
                setSpeedWithoutSend(speed);
                break;
            case "keyPressed":
                onOpponentKeyPressed(msg.getString("key"));
                break;
            case "msg":
                String originalContent = sceneController.getChatContentArea().getText();
                sceneController.getChatContentArea().setText(originalContent + "对方>>" + msg.getString("content") + "\n");
                break;
        }
    }

    public void onConnectionInactive() {
        if(state == State.ABOUT_TO_EXIT || state == State.GAME_OVER) {
            return;
        }

        pause();
        sceneController.getPauseResumeBtn().setDisable(true);
        sceneController.getSpeedSlider().setDisable(true);
        sceneController.getChatInputField().setDisable(true);
        sceneController.getChatSendBtn().setDisable(true);
        state = State.GAME_OVER;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("对方已经断线");
        alert.showAndWait();
    }

    @Override
    public void sendMessage(String msg) {
        if(channel != null) {
            String originalContent = sceneController.getChatContentArea().getText();
            sceneController.getChatContentArea().setText(originalContent + "我>>" + msg + "\n");
            JSONObject info = new JSONObject();
            info.put("type", "msg");
            info.put("content", msg);
            channel.writeAndFlush(info.toString());
        }
    }
}
