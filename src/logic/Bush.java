package logic;

/*
 * 灌木丛类
 */

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import org.json.JSONObject;

public class Bush implements Sprite {
    public static Image bushImg = new Image("img/bush.png");
    public static final double bushSize = 60;
    private double X, Y;

    public Bush(double X, double Y) {
        this.X = X;
        this.Y = Y;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();
        gc.drawImage(bushImg, X - bushSize/2, Y - bushSize/2, bushSize, bushSize);
        gc.restore();
    }

    @Override
    public void update() {
        //DO NOTHING
    }

    public boolean inside(Point2D P) {
        return P.distance(X, Y) < bushSize/2;
    }

    public Point2D getPos() {
        return new Point2D(X, Y);
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("X", X);
        obj.put("Y", Y);
        return obj;
    }

    public boolean outOfCanvas() {
        return X < 0 || X > Constants.CANVAS_WIDTH || Y < 0 || Y > Constants.CANVAS_HEIGHT;
    }

    public boolean overlaps(Bush bush) {
        return bush.getPos().distance(getPos()) < bushSize;
    }

    public boolean overlaps(Egg egg) {
        return getPos().distance(egg.getPos()) < bushSize;
    }
}
