package logic;

import javafx.scene.canvas.GraphicsContext;

/*
 * Sprite为游戏场景中的物件接口，提供render和update函数
 */
public interface Sprite {
    void render(GraphicsContext gc);
    void update();
}
