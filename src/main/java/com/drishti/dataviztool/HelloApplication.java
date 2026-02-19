package com.drishti.dataviztool;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
// import javafx.stage.StageStyle; // <--- MAKE SURE THIS IS NOT IMPORTED IF YOU DON'T USE IT
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // IMPORTANT: Ensure you DO NOT have this line uncommented or present:
        // stage.initStyle(StageStyle.UNDECORATED); // <--- REMOVE OR COMMENT THIS LINE IF IT EXISTS

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 750); // Initial window size
        stage.setTitle("DataViz Tool");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
