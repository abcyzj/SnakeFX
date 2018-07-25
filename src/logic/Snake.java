package logic;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.LinkedList;

public class Snake implements Sprite {
    public static double cubeHeight = 30;
    public static double cubeWidth = 30;
    public static final int INIT_LENGTH = 100;
    public static final double BODY_DISTANCE = 1;//每个蛇身圆圈之间相隔距离
    public static double speed = 2;//每一帧蛇头移动的距离
    public enum Direction {LEFT, RIGHT, UP, DOWN};
    private Point2D head;
    private LinkedList<Point2D> bodies;
    private Color color;
    public Direction direction;

    public Snake(double headX, double headY, Direction direction, Color color) {
        this.direction = direction;
        this.color = color;
        head = new Point2D(headX, headY);
        bodies = new LinkedList<>();
        double dx = 0, dy = 0;
        switch(direction) {
            case UP:
                dx = 0;
                dy = -BODY_DISTANCE;
                break;
            case DOWN:
                dx = 0;
                dy = BODY_DISTANCE;
                break;
            case LEFT:
                dx = BODY_DISTANCE;
                dy = 0;
                break;
            case RIGHT:
                dx = - BODY_DISTANCE;
                dy = 0;
                break;
        }
        for(int i = 0; i < INIT_LENGTH; i++) {
            bodies.add(new Point2D(headX + i*dx, headY + i*dy));
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();
        gc.setFill(color);
        gc.fillOval(head.getX(), head.getY(), cubeWidth, cubeHeight);
        for(Point2D P: bodies) {
            gc.fillOval(P.getX(), P.getY(), cubeWidth, cubeHeight);
        }
        double eyeX1 = head.getX() + cubeWidth/2, eyeY1 = head.getY() + cubeHeight/2;
        double eyeX2 = head.getX() + cubeWidth/2, eyeY2 = head.getY() + cubeHeight/2;
        switch(direction) {
            case RIGHT:
                eyeX1 += cubeWidth/4;
                eyeY1 -= cubeHeight/4;
                eyeX2 += cubeWidth/4;
                eyeY2 += cubeHeight/4;
                break;
            case LEFT:
                eyeX1 -= cubeWidth/4;
                eyeY1 -= cubeHeight/4;
                eyeX2 -= cubeWidth/4;
                eyeY2 += cubeHeight/4;
                break;
            case UP:
                eyeX1 -= cubeWidth/4;
                eyeY1 -= cubeHeight/4;
                eyeX2 += cubeWidth/4;
                eyeY2 -= cubeHeight/4;
                break;
            case DOWN:
                eyeX1 -= cubeWidth/4;
                eyeY1 += cubeHeight/4;
                eyeX2 += cubeWidth/4;
                eyeY2 += cubeHeight/4;
        }
        gc.setFill(Color.web("#fff"));
        gc.fillOval(eyeX1 - cubeWidth/8, eyeY1 - cubeHeight/8, cubeWidth/4, cubeHeight/4);
        gc.fillOval(eyeX2 - cubeWidth/8, eyeY2 - cubeHeight/8, cubeWidth/4, cubeHeight/4);
        gc.setFill(Color.web("#333"));
        gc.fillOval(eyeX1 - cubeWidth/16, eyeY1 - cubeHeight/16, cubeWidth/8, cubeHeight/8);
        gc.fillOval(eyeX2 - cubeWidth/16, eyeY2 - cubeHeight/16, cubeWidth/8, cubeHeight/8);
        gc.restore();
    }

    @Override
    public void update() {
        double dx = 0, dy = 0;
        switch(direction) {
            case UP:
                dx = 0;
                dy = -speed;
                break;
            case DOWN:
                dx = 0;
                dy = speed;
                break;
            case LEFT:
                dx = -speed;
                dy = 0;
                break;
            case RIGHT:
                dx = speed;
                dy = 0;
                break;
        }

        head = head.add(dx, dy);

        if(head.getX() > Constants.CANVAS_WIDTH) {
            head = new Point2D(0, head.getY());
        }
        if(head.getY() > Constants.CANVAS_HEIGHT) {
            head = new Point2D(head.getX(), 0);
        }
        if(head.getX() < 0) {
            head = new Point2D(Constants.CANVAS_WIDTH, head.getY());
        }
        if(head.getY() < 0) {
            head = new Point2D(head.getX(), Constants.CANVAS_HEIGHT);
        }

        if(head.distance(bodies.getFirst()) > BODY_DISTANCE) {
            bodies.addFirst(head);
            bodies.removeLast();
        }
    }
}
