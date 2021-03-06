package logic;

/*
 * 蛇洞类
 */

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.json.JSONObject;

public class Hole implements Sprite {
    public static final double holeSize = 50;
    public Point2D pos;

    public Hole(double X, double Y) {
        pos = new Point2D(X, Y);
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();
        gc.setFill(Color.rgb(101, 92, 47));
        gc.fillOval(pos.getX() - holeSize*0.6, pos.getY() - holeSize*0.6, holeSize*1.2, holeSize*1.2);
        gc.setFill(Color.BLACK);
        gc.fillOval(pos.getX() - holeSize/2, pos.getY() - holeSize/2, holeSize, holeSize);
        gc.restore();
    }

    @Override
    public void update() {
        //DO NOTHING
    }

    public boolean isInHole(Point2D point) {
        return pos.distance(point) < holeSize/2;
    }

    public boolean overlaps(Hole hole) {
        return pos.distance(hole.pos) < holeSize;
    }

    public boolean overlaps(Bush bush) {
        return pos.distance(bush.getPos()) < Bush.bushSize*2;
    }

    public boolean overlaps(Egg egg) {
        return pos.distance(egg.getPos()) < holeSize;
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("X", pos.getX());
        obj.put("Y", pos.getY());
        return obj;
    }
}
