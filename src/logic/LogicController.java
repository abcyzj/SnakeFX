package logic;

/*
 * LogicController接口，由MasterLogicController和SlaveLogicController分别实现
 */

import javafx.scene.input.KeyEvent;

public interface LogicController {
    void start();
    void pause();
    void setSpeed(int speed);
    void onKeyPressed(KeyEvent event);
    void exit();
    void sendMessage(String msg);
}
