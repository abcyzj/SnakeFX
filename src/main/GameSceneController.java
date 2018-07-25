package main;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import logic.LogicController;

import java.net.URL;
import java.util.ResourceBundle;

public class GameSceneController implements Initializable {
    @FXML
    private StackPane gameArea;
    @FXML
    private AnchorPane funcArea;
    @FXML
    private Canvas meshBackground;
    @FXML
    private Canvas gameCanvas;

    private LogicController logicController;

    public enum GameState {GAME_RUNNING};
    private GameState state = GameState.GAME_RUNNING;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initMeshBackground();
        initLogicController();
    }

    private static final int ROW_NUM = 30;
    private static final int COL_NUM = 30;
    private static final Color DARK_GREEN = Color.rgb(36, 93, 25);
    private static final Color LIGHT_GREEN = Color.rgb(34, 105, 27);
    private void initMeshBackground() {
        double cubeWidth = meshBackground.getWidth()/COL_NUM;
        double cubeHeight = meshBackground.getHeight()/ROW_NUM;
        GraphicsContext gc = meshBackground.getGraphicsContext2D();
        for(int i = 0; i < ROW_NUM; i++) {
            for(int j = 0; j < COL_NUM; j++) {
                if((i + j)%2 == 0) {
                    gc.setFill(LIGHT_GREEN);
                }
                else {
                    gc.setFill(DARK_GREEN);
                }
                gc.fillRect(j*cubeWidth, i*cubeHeight, cubeWidth + 2, cubeHeight + 2);
            }
        }
    }

    private void initLogicController() {
        logicController = new LogicController(gameCanvas, this);
        logicController.start();
        state = GameState.GAME_RUNNING;
    }

    public void initEvents(Stage stage) {
        stage.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
    }

    private void onKeyPressed(KeyEvent event) {
        switch(state) {
            case GAME_RUNNING:
                logicController.onKeyPressed(event);
        }
    }
}
