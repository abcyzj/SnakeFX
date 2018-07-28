package logic;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;

import static logic.Snake.Direction.*;

public class Snake implements Sprite {
    public static double cubeHeight = 30;
    public static double cubeWidth = 30;
    public static final int INIT_LENGTH = 20;
    public static final double BODY_DISTANCE = 1;//每个蛇身圆圈之间相隔距离
    public static final int HOLE_FRAME_NUM = 100;//钻洞持续的帧数
    public static final int EAT_BODIES = 10;//每吃一个蛋增加的圆圈数目
    public static double speed = 2;//每一帧蛇头移动的距离
    public enum Direction {LEFT, RIGHT, UP, DOWN}
    private Point2D head;
    private LinkedList<Point2D> bodies;
    private Color color;
    public Direction direction;
    private Hole curHole = null;
    private int inHoleFrame = 0;//在洞内经历的帧数

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

    public Snake(Hole birthHole, Direction direction, Color color) {
        this(birthHole.pos.getX(), birthHole.pos.getY(), direction, color);
        this.getOutOfHole(birthHole);
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();
        gc.setFill(color);
        gc.fillOval(head.getX() - cubeWidth/2, head.getY() - cubeHeight/2, cubeWidth, cubeHeight);
        for(Point2D P: bodies) {
            gc.fillOval(P.getX() - cubeWidth/2, P.getY() - cubeHeight/2, cubeWidth, cubeHeight);
        }
        double eyeX1 = head.getX(), eyeY1 = head.getY();
        double eyeX2 = head.getX(), eyeY2 = head.getY();
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
        gc.fillOval(eyeX1 - cubeWidth/12, eyeY1 - cubeHeight/12, cubeWidth/6, cubeHeight/6);
        gc.fillOval(eyeX2 - cubeWidth/12, eyeY2 - cubeHeight/12, cubeWidth/6, cubeHeight/6);
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

        if(curHole == null) {//只有不在洞内的时候才考虑出界
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
        }

        if(head.distance(bodies.getFirst()) > BODY_DISTANCE) {
            bodies.addFirst(head);
            bodies.removeLast();
        }

        if(curHole != null) {
            inHoleFrame++;
        }
    }

    public void getInHole(Hole hole) {
        inHoleFrame = 0;
        curHole = hole;
        head = new Point2D(10*Constants.CANVAS_WIDTH, 10*Constants.CANVAS_HEIGHT);//将头移动到画布外
    }

    public void getOutOfHole(Hole hole) {
        inHoleFrame = 0;
        curHole = null;
        double dx = 0, dy = 0;
        switch(direction) {
            case RIGHT:
                dx = Hole.holeSize/2;
                break;
            case LEFT:
                dx = -Hole.holeSize/2;
                break;
            case UP:
                dy = -Hole.holeSize/2;
                break;
            case DOWN:
                dy = Hole.holeSize/2;
                break;
        }
        head = hole.pos.add(dx, dy);
    }

    public boolean mayGetOutOfHole() {
        return curHole != null && inHoleFrame > HOLE_FRAME_NUM;
    }

    public Point2D getHead() {
        return head;
    }

    public Hole getCurHole() {
        return curHole;
    }

    public void eatAnEgg() {
        Point2D tail = bodies.getLast();
        for(int i = 0; i < EAT_BODIES; i++) {
            bodies.addLast(new Point2D(tail.getX(), tail.getY()));
        }
    }

    public boolean inMyBody(Point2D P) {
        boolean inBody = false;
        if(!P.equals(head)) {
            for(Point2D body: bodies) {
                if(body.distance(P) < cubeWidth/2) {
                    inBody = true;
                    break;
                }
            }
        }
        else {
            for(Point2D body: bodies.subList(INIT_LENGTH, bodies.size())) {
                if(body.distance(P) < cubeWidth/2) {
                    inBody = true;
                    break;
                }
            }
        }
        return inBody;
    }

    public int getLength() {
        return bodies.size()/10;
    }

    public Color getColor() {
        return color;
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        JSONArray JSONBodies = new JSONArray();
        for(Point2D body: bodies) {
            JSONObject JSONBody = new JSONObject();
            JSONBody.put("X", body.getX());
            JSONBody.put("Y", body.getY());
            JSONBodies.put(JSONBody);
        }
        obj.put("bodies", JSONBodies);
        obj.put("direction", direction.toString());
        obj.put("headX", head.getX());
        obj.put("headY", head.getY());
        return obj;
    }

    public void updateFromJSONObject(JSONObject obj) {
        JSONArray JSONBodies = obj.getJSONArray("bodies");
        bodies.clear();
        for(int i = 0; i < JSONBodies.length(); i++) {
            JSONObject JSONBody = JSONBodies.getJSONObject(i);
            bodies.add(new Point2D(JSONBody.getDouble("X"), JSONBody.getDouble("Y")));
        }
        String dir = obj.getString("direction");
        switch(dir) {
            case "UP":
                direction = UP;
                break;
            case "DOWN":
                direction = DOWN;
                break;
            case "LEFT":
                direction = LEFT;
                break;
            case "RIGHT":
                direction = RIGHT;
                break;
        }
        double headX = obj.getDouble("headX");
        double headY = obj.getDouble("headY");
        head = new Point2D(headX, headY);
    }
}
