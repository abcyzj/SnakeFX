package logic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.util.Duration;
import main.SceneController;
import network.Server;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.Vector;

public class MasterLogicController implements LogicController {
    private Canvas gameCanvas;
    private Text infoLabel;
    private SceneController sceneController;
    private Vector<Sprite> sprites;
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
    private Server server;
    private Channel channel;

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

    private void initSprites() {
        sprites = new Vector<>();
        snakes = new Vector<>();
        holes = new Vector<>();
        eggs = new Vector<>();
        bushes = new Vector<>();

        snakeA = new Snake(100, 100, Snake.Direction.RIGHT, Constants.SNAKE_A_COLOR);
        snakeB = new Snake(200, 300, Snake.Direction.DOWN, Constants.SNAKE_B_COLOR);
        sprites.add(snakeA);
        sprites.add(snakeB);
        snakes.add(snakeA);
        snakes.add(snakeB);

        Hole H1 = new Hole(300, 200);
        sprites.add(H1);
        holes.add(H1);
        Hole H2 = new Hole(500, 500);
        sprites.add(H2);
        holes.add(H2);

        Egg E1 = new Egg(600, 600);
        sprites.add(E1);
        eggs.add(E1);

        for(int i = 0; i < 5; i++) {
            Bush newBush = new Bush(800 + i*Bush.bushSize, 400 + Bush.bushSize);
            bushes.add(newBush);
            sprites.add(newBush);
        }
    }

    private void initPlayers() {
        playerA = new Player();
        playerB = new Player();
    }

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
        syncCanvas();
    }

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
                    sprites.remove(egg);
                    Egg newEgg = new Egg(Math.random()*Constants.CANVAS_WIDTH, Math.random()*Constants.CANVAS_HEIGHT);
                    eggs.add(newEgg);
                    sprites.add(newEgg);
                    break;
                }
            }
        }
        sceneController.notifyScore(playerA.score);
    }

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
            revive(deadSnake);
        }
    }

    private Snake revive(Snake oldSnake) {
        sprites.remove(oldSnake);
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
        sprites.add(newSnake);
        sceneController.notifySnakeNum(playerA.remainingSnakes);
        return newSnake;
    }

    private void startServer() {
        server = new Server(this);
        ChannelFuture listenFuture = server.listen(0);
        listenFuture.addListener((ChannelFuture F) -> {
            if(F.isSuccess()) {
                InetSocketAddress address = (InetSocketAddress) F.channel().localAddress();
                String IP = server.getLocalIP();
                infoLabel.setText("正在端口" + address.getPort() + "上监听\n" + "IP:" + IP);
            }
            else {
                infoLabel.setText("无法监听，请返回主页重试");
            }
        });
        infoLabel.setVisible(true);
        infoLabel.setText("准备监听");
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void startGame() {
        start();
        channel.writeAndFlush(getStaticPos().toString());
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
        timeline.pause();
    }

    public void exit() {
        if(timeline != null) {
            timeline.stop();
        }
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        server.close();
    }

    public void setSpeed(int speed) {
        frameTime = INIT_FRAME_TIME/(1 + 0.2*speed);
        if(timeline.getStatus() == Animation.Status.RUNNING) {
            pause();
            start();
        }
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
            case UP:
                if(snakeB.direction != Snake.Direction.DOWN) {
                    snakeB.direction = Snake.Direction.UP;
                }
                break;
            case DOWN:
                if(snakeB.direction != Snake.Direction.UP) {
                    snakeB.direction = Snake.Direction.DOWN;
                }
                break;
            case LEFT:
                if(snakeB.direction != Snake.Direction.RIGHT) {
                    snakeB.direction = Snake.Direction.LEFT;
                }
                break;
            case RIGHT:
                if(snakeB.direction != Snake.Direction.LEFT) {
                    snakeB.direction = Snake.Direction.RIGHT;
                }
                break;
        }
    }
}
