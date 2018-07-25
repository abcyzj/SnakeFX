package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GameScene.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Snake");
        primaryStage.setResizable(false);
        Scene gameScene = new Scene(root);
        gameScene.getStylesheets().add(getClass().getResource("Snake.css").toExternalForm());
        primaryStage.setScene(gameScene);
        GameSceneController gameSceneController = loader.getController();
        gameSceneController.initEvents(primaryStage);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
