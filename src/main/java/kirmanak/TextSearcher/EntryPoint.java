package kirmanak.TextSearcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

@Log4j2
public class EntryPoint extends Application {
    private final ObservableList<MarkedFile> FILES =
            FXCollections.synchronizedObservableList(FXCollections.observableList(new ArrayList<>()));
    private final TextField PATH_FIELD = new TextField();
    private final TextField TEXT_FIELD = new TextField();
    private final TextField EXTENSION_FIELD = new TextField("log");
    private final TextFlow TEXT_FLOW = new TextFlow();

    public static void main(String[] args) {
        launch();
    }

    public void start(final Stage primaryStage) {
        final EntryMessage entryMessage = log.traceEntry("start(primaryStage = {}) of {}", primaryStage, this);
        FILES.addListener(this::updateView);
        primaryStage.setTitle("TextSearcher");
        primaryStage.setScene(constructScene());
        primaryStage.show();
        log.traceExit(entryMessage);
    }

    /**
     * Constructs the main scene to be displayed
     *
     * @return the constructed Scene
     */
    private Scene constructScene() {
        final EntryMessage entryMessage = log.traceEntry("constructScene() of {}", this);
        final Button actionButton = new Button("Search");
        actionButton.setOnAction(this::onSearchRequest);
        final GridPane gridPane = new GridPane();
        gridPane.addRow(0, TEXT_FLOW);
        gridPane.addRow(1, new Label("Path:"), PATH_FIELD);
        gridPane.addRow(2, new Label("Extension:"), EXTENSION_FIELD);
        gridPane.addRow(3, new Label("Text:"), TEXT_FIELD);
        gridPane.add(actionButton, 1, 4);
        final Scene scene = new Scene(gridPane);
        return log.traceExit(entryMessage, scene);
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
            searcher.search(FILES::add);
        } catch (final IOException e) {
            log.error(entryMessage, e);
        }
        log.traceExit(entryMessage);
    }

    /**
     * Updates the content view in the GUI
     *
     * @param change change happened in the list
     */
    private void updateView(final ListChangeListener.Change<? extends MarkedFile> change) {
        final EntryMessage entryMessage = log.traceEntry("updateView(change = {}) of {}", change, this);
        Platform.runLater(() -> TEXT_FLOW.getChildren().setAll(FILES.get(0).toTextFlow().getChildren()));
        log.traceExit(entryMessage);
    }
}
