package main;

/*
 * 入口类，创建窗口并加载FXML文件
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private SceneController sceneController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GameScene.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Snake");
        primaryStage.setResizable(true);
        Scene gameScene = new Scene(root);
        gameScene.getStylesheets().add(getClass().getResource("Snake.css").toExternalForm());
        primaryStage.setScene(gameScene);
        sceneController = loader.getController();
        sceneController.setStage(primaryStage);
        sceneController.initKeyEvents(primaryStage);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        sceneController.exit();//需要把网络线程关掉
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
