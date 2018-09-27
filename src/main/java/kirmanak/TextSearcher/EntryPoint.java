package kirmanak.TextSearcher;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;

@Log4j2
public class EntryPoint extends Application {
    private final TextField PATH_FIELD = new TextField();
    private final TextField TEXT_FIELD = new TextField();
    private final TextField EXTENSION_FIELD = new TextField();
    private final TextArea AREA = new TextArea();

    public static void main(String[] args) {
        launch();
    }

    public void start(final Stage primaryStage) {
        final EntryMessage entryMessage = log.traceEntry("start(primaryStage = {}) of {}", primaryStage, this);
        final Button actionButton = new Button("Search");
        actionButton.setOnAction(this::onSearchRequest);
        final GridPane gridPane = new GridPane();
        gridPane.addRow(0, AREA);
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
     * Performs the search and passes the result if any to showFiles()
     *
     * @param searcher initialized TextSearcher instance
     */
    private void search(final TextSearcher searcher) {
        final EntryMessage entryMessage = log.traceEntry("search(searcher = {}) of {}", searcher, this);
        final Collection<MarkedFile> markedFiles;
        try {
            markedFiles = searcher.getFiles();
        } catch (final IOException err) {
            log.error(entryMessage, err);
            return;
        }
        showFiles(markedFiles);
        log.traceExit(entryMessage);
    }

    /**
     * Shows the found files in TextArea
     *
     * @param markedFiles the found files
     */
    private void showFiles(final Collection<MarkedFile> markedFiles) {
        final EntryMessage entryMessage =
                log.traceEntry("showFiles(markedFiles = {}) of {}", markedFiles, this);
        final Iterator<MarkedFile> iterator = markedFiles.iterator();
        if (iterator.hasNext()) {
            final StringBuilder builder = new StringBuilder();
            for (final String line : iterator.next().getLines()) {
                builder.append(line);
            }
            AREA.setText(builder.toString());
        }
        log.traceExit(entryMessage);
    }
}
