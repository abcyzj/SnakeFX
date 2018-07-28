package main;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import logic.LogicController;
import logic.MasterLogicController;
import logic.SlaveLogicController;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class SceneController implements Initializable {
    @FXML
    private StackPane gameArea;
    @FXML
    private AnchorPane funcArea;
    @FXML
    private Canvas meshBackground;
    @FXML
    private Canvas gameCanvas;
    @FXML
    private Button pauseResumeBtn;
    @FXML
    private Text inHoleLabel;
    @FXML
    private Text scoreLabel;
    @FXML
    private Text snakeNumLabel;
    @FXML
    private Slider speedSlider;
    @FXML
    private Button homeBtn;
    @FXML
    private Button createServerBtn;
    @FXML
    private Button connectBtn;
    @FXML
    private AnchorPane homeScene;
    @FXML
    private TextField addressField;
    @FXML
    private TextField portField;
    @FXML
    private GridPane infoPane;
    @FXML
    private Text infoLabel;

    private LogicController logicController;

    public enum GameState {GAME_RUNNING, BEFORE_GAME, GAME_PAUSED}
    private GameState state = GameState.BEFORE_GAME;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initMeshBackground();
        initButtons();
        initEvents();
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

    private void initButtons() {
        pauseResumeBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            if(event.getButton() == MouseButton.PRIMARY) {
                if(state == GameState.GAME_RUNNING) {
                    pauseResumeBtn.setText("继续");
                    logicController.pause();
                    state = GameState.GAME_PAUSED;
                }
                else if(state == GameState.GAME_PAUSED) {
                    pauseResumeBtn.setText("暂停");
                    logicController.start();
                    state = GameState.GAME_RUNNING;
                }
            }
        });
        pauseResumeBtn.setDisable(true);

        homeBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            if(event.getButton() == MouseButton.PRIMARY) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setContentText("您确定要返回主页？");

                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == ButtonType.OK) {
                    if(logicController != null) {
                        logicController.exit();
                        logicController = null;
                        pauseResumeBtn.setDisable(true);
                        state = GameState.BEFORE_GAME;
                        homeBtn.setDisable(true);
                        homeScene.setVisible(true);
                        infoLabel.setVisible(false);
                    }
                }
            }
        });
        homeBtn.setDisable(true);

        createServerBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            if(event.getButton() == MouseButton.PRIMARY) {
                startAsMaster();
            }
        });

        connectBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            if(event.getButton() == MouseButton.PRIMARY) {
                Pattern IPPattern = Pattern
                        .compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");
                if(!IPPattern.matcher(addressField.getText()).matches()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("请输入合法的IP地址");
                    alert.showAndWait();
                    return;
                }

                Pattern portPattern = Pattern
                        .compile("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$");
                if(!portPattern.matcher(portField.getText()).matches()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("请输入合法的端口号");
                    alert.showAndWait();
                    return;
                }

                startAsSlave();
            }
        });
    }

    private void startAsMaster() {
        homeScene.setVisible(false);
        logicController = new MasterLogicController(gameCanvas, infoLabel, this);
        state = GameState.GAME_RUNNING;
    }

    private void startAsSlave() {
        homeScene.setVisible(false);
        String addr = addressField.getText();
        int port = Integer.parseInt(portField.getText());
        logicController = new SlaveLogicController(gameCanvas, this, infoLabel, addressField.getText(), port);
    }

    public void enableGameButtons() {
        homeBtn.setDisable(false);
        pauseResumeBtn.setDisable(false);
        speedSlider.setDisable(false);
    }

    public void togglePauseBtn() {
        if(pauseResumeBtn.getText().equals("暂停")) {
            pauseResumeBtn.setText("继续");
        }
        else {
            pauseResumeBtn.setText("暂停");
        }
    }


    public void initKeyEvents(Stage stage) {
        stage.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
    }

    private void onKeyPressed(KeyEvent event) {
        switch(state) {
            case GAME_RUNNING:
                logicController.onKeyPressed(event);
                event.consume();
                break;
        }
    }

    private void initEvents() {
        speedSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            int roundValue = newValue.intValue();
            speedSlider.setValue(roundValue);
            logicController.setSpeed(roundValue);
        }));
        speedSlider.setDisable(true);
    }

    public void notifyInHole(boolean inHole) {
        inHoleLabel.setVisible(inHole);
    }

    public void notifyScore(int score) {
        scoreLabel.setText("你的分数：" + score);
    }

    public void notifySnakeNum(int snakeNum) {
        snakeNumLabel.setText("剩余" + snakeNum + "条蛇");
    }

    public void exit() {
        if(logicController != null) {
            logicController.exit();
        }
    }
}
