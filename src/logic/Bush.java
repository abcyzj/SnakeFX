package logic;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

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
}
