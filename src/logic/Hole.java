package logic;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Hole implements Sprite {
    public static double holeSize = 80;
    public Point2D pos;

    public Hole(double X, double Y) {
        pos = new Point2D(X, Y);
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();
        gc.setFill(Color.BLACK);
        gc.fillOval(pos.getX() - holeSize/2, pos.getY() - holeSize/2, holeSize, holeSize);
        gc.restore();
    }

    @Override
    public void update() {
        //TODO
    }

    public boolean isInHole(Point2D point) {
        return pos.distance(point) < holeSize/2;
    }
}
