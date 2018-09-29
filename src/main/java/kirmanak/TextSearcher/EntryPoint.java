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
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class EntryPoint extends Application {
    private final ObservableList<MarkedFile> FILES =
            FXCollections.synchronizedObservableList(FXCollections.observableList(new ArrayList<>()));
    private final TextField PATH_FIELD = new TextField("/home/kirmanak/logs");
    private final TextField TEXT_FIELD = new TextField("error");
    private final TextField EXTENSION_FIELD = new TextField("log");
    private final TextFlow TEXT_FLOW = new TextFlow();

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
        final ScrollPane textScrollPane = new ScrollPane(TEXT_FLOW);
        final GridPane gridPane = new GridPane();
        final ListView<MarkedFile> listView = new ListView<>();
        listView.setItems(FILES);
        listView.setMinWidth(300);
        listView.getSelectionModel().selectedItemProperty().addListener(this::selectionListener);
        gridPane.add(listView, 0, 0);
        gridPane.add(textScrollPane, 1, 0);
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
            final ObservableValue<? extends MarkedFile> observable,
            final MarkedFile oldValue,
            final MarkedFile newValue) {
        if (newValue.equals(oldValue)) {
            return;
        }
        Platform.runLater(() -> {
            final TextFlow newFlow = new TextFlow(newValue.getTexts());
            TEXT_FLOW.getChildren().setAll(newFlow.getChildren());
        });
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
        service.setOnSucceeded(stateEvent -> FILES.setAll((List<MarkedFile>) stateEvent.getSource().getValue()));
        service.start();
        log.traceExit(entryMessage);
    }
}
