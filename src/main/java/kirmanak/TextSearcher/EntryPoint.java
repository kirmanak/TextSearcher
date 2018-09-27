package kirmanak.TextSearcher;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Paths;

@Log4j2
public class EntryPoint extends Application {
    private final TextField PATH_FIELD = new TextField();
    private final TextField TEXT_FIELD = new TextField();
    private final TextField EXTENSION_FIELD = new TextField();
    private final Text TEXT = new Text();

    public static void main(String[] args) {
        launch();
    }

    public void start(final Stage primaryStage) {
        final EntryMessage entryMessage = log.traceEntry("start(primaryStage = {}) of {}", primaryStage, this);
        final Button actionButton = new Button("Search");
        actionButton.setOnAction(this::onSearchRequest);
        final GridPane gridPane = new GridPane();
        gridPane.addRow(0, TEXT);
        gridPane.addRow(1, new Label("Path:"), PATH_FIELD);
        gridPane.addRow(2, new Label("Extension:"), EXTENSION_FIELD);
        gridPane.addRow(3, new Label("Text:"), TEXT_FIELD);
        gridPane.add(actionButton, 1, 4);
        final Scene scene = new Scene(gridPane);
        primaryStage.setTitle("TextSearcher");
        primaryStage.setScene(scene);
        primaryStage.show();
        log.traceExit(entryMessage);
    }

    /**
     * Initializes a TextSearcher and passes it to performSearch()
     *
     * @param actionEvent the event which has requested search
     * @throws IllegalArgumentException if an error is happened during the initialization
     */
    private void onSearchRequest(final ActionEvent actionEvent) {
        final EntryMessage entryMessage =
                log.traceEntry("onSearchRequest(actionEvent = {}) of {}", actionEvent, this);
        final String path = PATH_FIELD.getText();
        final String extension = EXTENSION_FIELD.getText();
        final String text = TEXT_FIELD.getText();
        final TextSearcher textSearcher;
        try {
            textSearcher = new TextSearcher(Paths.get(path), extension, text);
        } catch (final IllegalArgumentException err) {
            log.error(entryMessage, err);
            return;
        }
        search(textSearcher);
        log.traceExit(entryMessage);
    }

    /**
     * Performs the search and passes the result if any to unwrapFiles()
     *
     * @param searcher initialized TextSearcher instance
     */
    private void search(final TextSearcher searcher) {
        final EntryMessage entryMessage = log.traceEntry("search(searcher = {}) of {}", searcher, this);
        try {
            searcher.search(this::showFile);
        } catch (final IOException e) {
            log.error(entryMessage, e);
        }
        log.traceExit(entryMessage);
    }

    /**
     * Shows the content of found file in the GUI
     *
     * @param file file to show
     */
    private void showFile(final MarkedFile file) {
        final EntryMessage entryMessage = log.traceEntry("showFiles(file = {}) of {}", file, this);
        final StringBuilder builder = new StringBuilder();
        file.getLines().stream().map((line) -> String.format("%s%n", line)).forEach(builder::append);
        TEXT.setText(builder.toString());
        log.traceExit(entryMessage);
    }
}
