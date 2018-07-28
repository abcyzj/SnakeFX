package logic;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import javafx.geometry.Point2D;
import org.json.JSONObject;

public class Egg implements Sprite {
    public static double eggSize = 50;
    private static Image eggImg = new Image("img/egg.png");
    private double X, Y;

    public Egg(double X, double Y) {
        this.X = X;
        this.Y = Y;
    }

    public boolean isInEgg(Point2D pos) {
        return pos.distance(X, Y) < eggSize/2;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();
        gc.drawImage(eggImg, X - eggSize/2, Y - eggSize/2, eggSize, eggSize);
        gc.restore();
    }

    @Override
    public void update() {
        //DO NOTHING
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("X", X);
        obj.put("Y", Y);
        return obj;
    }

    public Point2D getPos() {
        return new Point2D(X, Y);
    }
}
