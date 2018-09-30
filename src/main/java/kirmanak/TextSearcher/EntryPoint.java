package kirmanak.TextSearcher;

import javafx.application.Application;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class EntryPoint extends Application {
    private final ObservableList<Path> FILES = FXCollections.observableList(new ArrayList<>());
    private final TextField PATH_FIELD = new TextField("/home/kirmanak/logs");
    private final TextField TEXT_FIELD = new TextField("error");
    private final TextField EXTENSION_FIELD = new TextField("log");
    private final TabPane tabPane = new TabPane();
    private final Button ACTION_BUTTON = new Button("Search");

    public static void main(String[] args) {
        launch();
    }

    /**
     * Starts the GUI
     *
     * @param primaryStage the main window
     */
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
        ACTION_BUTTON.setOnAction(this::onSearchRequest);
        final VBox vBox = new VBox(
                new HBox(new Label("Path: "), PATH_FIELD),
                new HBox(new Label("Extension: "), EXTENSION_FIELD),
                new HBox(new Label("Text: "), TEXT_FIELD)
        );
        final GridPane gridPane = new GridPane();
        final ListView<Path> listView = new ListView<>();
        listView.setItems(FILES);
        listView.setMinWidth(300);
        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> selectionListener(oldValue, newValue));
        gridPane.add(listView, 0, 0);
        gridPane.add(tabPane, 1, 0);
        gridPane.addRow(1, vBox, ACTION_BUTTON);
        final Scene scene = new Scene(gridPane, 1366, 768);
        return log.traceExit(entryMessage, scene);
    }

    /**
     * Called when a new item is selected in the listView
     *
     * @param oldValue   the previously selected item
     * @param newValue   the new selected item
     */
    private void selectionListener(final Path oldValue, final Path newValue) {
        final EntryMessage m = log.traceEntry("selectionListener(oldValue = {}, newValue = {}", oldValue, newValue);
        if (newValue == null) {
            return;
        }
        final MarkedFileService service = new MarkedFileService(newValue, TEXT_FIELD.getText());
        service.setOnSucceeded(stateEvent -> addTab((TextArea) stateEvent.getSource().getValue(), newValue));
        service.start();
        log.traceExit(m);
    }

    /**
     * Adds a tab to the TabPane
     *
     * @param textArea the tab content
     * @param path     the tab name
     */
    private void addTab(final TextArea textArea, final Path path) {
        final EntryMessage m = log.traceEntry("addTab(textArea = {}) of {}", textArea, this);
        tabPane.getTabs().removeIf(tab -> tab.getText().equals(path.toString()));
        tabPane.getTabs().add(new Tab(path.toString(), textArea));
        log.traceExit(m);
    }

    /**
     * Initializes a TextSearchService and passes it to performSearch()
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
        final TextSearchService service;
        try {
            service = new TextSearchService(Paths.get(path), extension, text);
        } catch (final IllegalArgumentException err) {
            log.error(entryMessage, err);
            return;
        }
        //noinspection unchecked
        service.setOnSucceeded(stateEvent -> FILES.setAll((List<Path>) stateEvent.getSource().getValue()));
        service.start();
        ACTION_BUTTON.textProperty().bind(service.messageProperty());
        log.traceExit(entryMessage);
    }
}
