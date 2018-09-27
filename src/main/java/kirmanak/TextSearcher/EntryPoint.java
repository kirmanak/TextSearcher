package kirmanak.TextSearcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Paths;

@Log4j2
public class EntryPoint extends Application {
    public static void main(String[] args) {
        launch();
    }

    public void start(final Stage primaryStage) {
        final EntryMessage entryMessage = log.traceEntry("start(primaryStage = {})", primaryStage);
        final Label pathLabel = new Label("Path:");
        final TextField pathField = new TextField();
        final Label extensionLabel = new Label("Extension:");
        final TextField extensionField = new TextField();
        final Button actionButton = new Button("Search");
        actionButton.setOnAction((value) -> {
            final TextSearcher textSearcher;
            try {
                textSearcher = new TextSearcher(Paths.get(pathField.getText()), extensionField.getText(), "error");
            } catch (final IllegalArgumentException err) {
                log.error(entryMessage, err);
                return;
            }
            try {
                textSearcher.getFiles().forEach(System.out::println);
            } catch (final IOException e) {
                log.error(entryMessage, e);
            }
        });
        final GridPane gridPane = new GridPane();
        gridPane.addRow(0, pathLabel, pathField);
        gridPane.addRow(1, extensionLabel, extensionField);
        gridPane.add(actionButton, 1, 2);
        final Scene scene = new Scene(gridPane);
        primaryStage.setTitle("TextSearcher");
        primaryStage.setScene(scene);
        primaryStage.show();
        log.traceExit(entryMessage);
    }
}
