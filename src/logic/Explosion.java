package logic;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import org.json.JSONObject;

public class Explosion implements Sprite {
    private Point2D pos;
    private double enlargeRatio = 1;
    public static double explosionSize = 100;
    public static double enlargeSpeed = 1 + 1e-3;
    private static Image explosionImg = new Image("img/explosion.png");

    public Explosion(double X, double Y) {
        pos = new Point2D(X, Y);
    }

    public Explosion(JSONObject obj) {
        double X = obj.getDouble("X");
        double Y = obj.getDouble("Y");
        pos = new Point2D(X, Y);
        enlargeRatio = obj.getDouble("enlargeRatio");
    }

    @Override
    public void render(GraphicsContext gc) {
        double size = explosionSize*enlargeRatio;
        gc.drawImage(explosionImg, pos.getX() - size/2, pos.getY() - size/2, size, size);
    }

    @Override
    public void update() {
        enlargeRatio *= enlargeSpeed;
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("enlargeRatio", enlargeRatio);
        obj.put("X", pos.getX());
        obj.put("Y", pos.getY());
        return obj;
    }
}
