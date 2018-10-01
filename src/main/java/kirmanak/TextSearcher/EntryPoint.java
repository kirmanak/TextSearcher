package kirmanak.TextSearcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;

@Log4j2
public class EntryPoint extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    /**
     * Starts the GUI
     *
     * @param primaryStage the main window
     */
    public void start(final Stage primaryStage) throws IOException {
        final EntryMessage entryMessage = log.traceEntry("start(primaryStage = {}) of {}", primaryStage, this);
        final FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        final Parent parent = loader.load();
        final WindowController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);
        primaryStage.setTitle("TextSearcher");
        primaryStage.setScene(new Scene(parent));
        primaryStage.show();
        log.traceExit(entryMessage);
    }

}
