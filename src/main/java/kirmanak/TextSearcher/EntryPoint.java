package kirmanak.TextSearcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;

@Log4j2
public class EntryPoint extends Application {
    private final ObservableList<FoundFile> FILES = FXCollections.synchronizedObservableList(
            FXCollections.observableList(new LinkedList<>())
    );
    private final TextField PATH_FIELD = new TextField("/home/kirmanak/logs");
    private final TextField TEXT_FIELD = new TextField("error");
    private final TextField EXTENSION_FIELD = new TextField("log");
    private final TextArea TEXT_AREA = new TextArea();

    public static void main(String[] args) {
        launch();
    }

    public void start(final Stage primaryStage) {
        final EntryMessage entryMessage = log.traceEntry("start(primaryStage = {}) of {}", primaryStage, this);
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
        final VBox vBox = new VBox(
                new HBox(new Label("Path: "), PATH_FIELD),
                new HBox(new Label("Extension: "), EXTENSION_FIELD),
                new HBox(new Label("Text: "), TEXT_FIELD)
        );
        final GridPane gridPane = new GridPane();
        final ListView<FoundFile> listView = new ListView<>();
        listView.setItems(FILES);
        listView.setMinWidth(300);
        listView.getSelectionModel().selectedItemProperty().addListener(this::selectionListener);
        gridPane.add(listView, 0, 0);
        TEXT_AREA.setEditable(false);
        gridPane.add(TEXT_AREA, 1, 0);
        gridPane.addRow(1, vBox, actionButton);
        final Scene scene = new Scene(gridPane, 1366, 768);
        return log.traceExit(entryMessage, scene);
    }

    /**
     * Called when a new item is selected in the listView
     *
     * @param observable the modified value
     * @param oldValue   the previously selected item
     * @param newValue   the new selected item
     */
    private void selectionListener(
            final ObservableValue<? extends FoundFile> observable,
            final FoundFile oldValue,
            final FoundFile newValue) {
        final EntryMessage m = log.traceEntry(
                "selectionListener(observable = {}, oldValue = {}, newValue = {})", observable, oldValue, newValue
        );
        if (newValue != null && !newValue.equals(oldValue)) {
            Platform.runLater(() -> TEXT_AREA.setText(newValue.getContent()));
        }
        log.traceExit(m);
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
            searcher.search(this::replaceFile);
        } catch (final IOException e) {
            log.error(entryMessage, e);
        }
        log.traceExit(entryMessage);
    }

    /**
     * Inserts a file to the collection. Replaces if it is present already
     *
     * @param file the file to be inserted
     */
    private void replaceFile(final FoundFile file) {
        final EntryMessage entryMessage = log.traceEntry("replaceFile(file = {}) of {}", file, this);
        final Iterator<FoundFile> iterator = FILES.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(file)) {
                Platform.runLater(iterator::remove);
                break;
            }
        }
        Platform.runLater(() -> FILES.add(file));
        log.traceExit(entryMessage);
    }
}
