package logic;

import javafx.scene.input.KeyEvent;

public interface LogicController {
    void start();
    void pause();
    void setSpeed(int speed);
    void onKeyPressed(KeyEvent event);
    void exit();
}
