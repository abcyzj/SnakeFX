package logic;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import main.GameSceneController;

import java.util.Vector;

public class LogicController {
    private Canvas gameCanvas;
    private GameSceneController sceneController;
    private Vector<Sprite> sprites;
    private Timeline timeline;
    private Snake snakeA;
    private Snake snakeB;
    private Vector<Snake> snakes;
    private Vector<Hole> holes;

    public LogicController(Canvas gameCanvas, GameSceneController sceneController) {
        this.gameCanvas = gameCanvas;
        gameCanvas.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        this.sceneController = sceneController;
        Constants.CANVAS_HEIGHT = gameCanvas.getHeight();
        Constants.CANVAS_WIDTH = gameCanvas.getWidth();
        initSprites();
    }

    private void initSprites() {
        //TODO
        sprites = new Vector<>();
        snakes = new Vector<>();
        holes = new Vector<>();

        snakeA = new Snake(100, 100, Snake.Direction.RIGHT, Color.RED);
        snakeB = new Snake(200, 300, Snake.Direction.DOWN, Color.BLUE);
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
    }

    private void updateCanvas() {
        //TODO
        for(Snake snake: snakes) {
            if(snake.getCurHole() != null) {//目前在洞中
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

        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, Constants.CANVAS_WIDTH, Constants.CANVAS_HEIGHT);
        for(Sprite S: sprites) {
            S.render(gc);
            S.update();
        }
    }

    public void start() {
        timeline = new Timeline(new KeyFrame(
                Duration.millis(10),
                ae -> updateCanvas()
        ));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void pause() {
        timeline.pause();
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
