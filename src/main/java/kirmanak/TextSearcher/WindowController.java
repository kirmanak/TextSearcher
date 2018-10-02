package kirmanak.TextSearcher;

import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Log4j2
@Getter
@Setter
@ToString
public class WindowController {
    // TODO: add error handling
    @FXML
    private TextField pathField;
    @FXML
    private TextField extensionField;
    @FXML
    private TextField textField;
    @FXML
    private TabPane tabPane;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TreeView<Path> treeView;
    @FXML
    private Stage primaryStage;
    private Path root = null;

    /**
     * Shows content of the provided path
     *
     * @param newValue the new selected item
     */
    private void showContent(final Path newValue) {
        final EntryMessage m = log.traceEntry("showContent(newValue = {})", newValue);
        final TextViewService service = new TextViewService(newValue, getTextField().getText());
        service.setOnRunning(this::onTaskRunning);
        service.setOnFailed(this::onTaskFailed);
        service.setOnSucceeded(stateEvent -> {
            addTab(service.getValue(), newValue);
            getProgressIndicator().setVisible(false);
        });
        service.start();
        log.traceExit(m);
    }

    /**
     * Calculates the path for the item based on the tree structure and calls selectionListener in case of success
     *
     * @param item item to start
     */
    private void selectionListener(final TreeItem<Path> item) {
        final EntryMessage entryMessage = log.traceEntry("selectionListener(item = {})", item);
        final Optional<Path> optionalPath = getRoot();
        if (optionalPath.isPresent()) {
            final Path path = optionalPath.get();
            TreeItem<Path> current = item;
            Path result = item.getValue();
            while (!current.getValue().equals(path)) {
                current = current.getParent();
                result = current.getValue().resolve(result);
            }
            showContent(result);
        }
        log.traceExit(entryMessage);
    }

    /**
     * Adds a tab to the TabPane
     *
     * @param textArea the tab content
     * @param path     the tab name
     */
    private void addTab(final TextArea textArea, final Path path) {
        final EntryMessage m = log.traceEntry(
                "addTab(textArea = {}, path = {})", textArea, path
        );
        getTabPane().getTabs().removeIf(tab -> tab.getText().equals(path.toString()));
        getTabPane().getTabs().add(new Tab(path.toString(), textArea));
        getTabPane().getSelectionModel().selectLast();
        log.traceExit(m);
    }

    /**
     * Creates a TextSearchService and starts the search
     */
    @FXML
    protected void onSearchRequest() {
        final EntryMessage entryMessage = log.traceEntry("onSearchRequest()");
        final Optional<Path> optionalRoot = getRoot();
        if (!optionalRoot.isPresent()) {
            return;
        }
        final Path root = optionalRoot.get();
        final TextSearchService service;
        try {
            service = new TextSearchService(root, getExtensionField().getText(), getTextField().getText());
        } catch (final IllegalArgumentException err) {
            log.error(entryMessage, err);
            return;
        }
        service.setOnRunning(this::onTaskRunning);
        service.setOnFailed(this::onTaskFailed);
        service.setOnSucceeded(event -> onSearchSucceeded(service.getValue()));
        service.start();
        log.traceExit(entryMessage);
    }

    /**
     * Generates a TreeView on successful search
     *
     * @param paths the search result
     */
    private void onSearchSucceeded(final List<Path> paths) {
        final EntryMessage entryMessage = log.traceEntry("onSearchSucceeded(paths = {})", paths);
        getProgressIndicator().setVisible(false);
        final TreeGeneratorService service = new TreeGeneratorService(paths, root);
        service.setOnRunning(this::onTaskRunning);
        service.setOnFailed(this::onTaskFailed);
        service.setOnSucceeded(event -> {
            getProgressIndicator().setVisible(false);
            getTreeView().setRoot(service.getValue());
        });
        service.start();
        log.traceExit(entryMessage);
    }

    /**
     * Handles running task showing progress indicator
     *
     * @param event the running task state
     */
    private void onTaskRunning(final WorkerStateEvent event) {
        final EntryMessage entryMessage = log.traceEntry("onSearchSucceeded(event = {})", event);
        getProgressIndicator().setVisible(true);
        getProgressIndicator().progressProperty().unbind();
        getProgressIndicator().progressProperty().bind(event.getSource().progressProperty());
        log.traceExit(entryMessage);
    }

    /**
     * Handles task execution errors
     *
     * @param event the failed task result
     */
    private void onTaskFailed(final WorkerStateEvent event) {
        final EntryMessage entryMessage = log.traceEntry("onTaskFailed(event = {})", event);
        getProgressIndicator().setVisible(false);
        log.traceExit(entryMessage);
    }

    /**
     * Gets root based on the text field and directory chooser
     *
     * @return the root folder to start search
     */
    private Optional<Path> getRoot() {
        final EntryMessage entryMessage = log.traceEntry("getRoot()");
        final String folderPath = getPathField().getText();
        if (!Optional.ofNullable(root).isPresent() && !folderPath.isEmpty()) {
            try {
                root = Paths.get(folderPath);
            } catch (final InvalidPathException err) {
                log.error(entryMessage, err);
                return log.traceExit(entryMessage, Optional.empty());
            }
        }
        if (Optional.ofNullable(root).isPresent() && Files.exists(root) && Files.isDirectory(root)) {
            return log.traceExit(entryMessage, Optional.of(root));
        }
        root = null;
        return log.traceExit(entryMessage, Optional.empty());
    }

    @FXML
    protected void showDirectoryChooser() {
        final EntryMessage entryMessage = log.traceEntry("showDirectoryChooser()");
        final DirectoryChooser chooser = new DirectoryChooser();
        final Optional<Path> optionalPath = getRoot();
        optionalPath.ifPresent(path -> chooser.setInitialDirectory(path.toFile()));
        final File file = chooser.showDialog(getPrimaryStage());
        Optional.ofNullable(file).ifPresent(newRoot -> root = file.toPath());
        Optional.ofNullable(root).ifPresent(path -> getPathField().setText(root.toString()));
        log.traceExit(entryMessage);
    }

    @FXML
    public void initialize() {
        final EntryMessage entryMessage = log.traceEntry("initialize()");
        getTreeView()
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) ->
                        Optional.ofNullable(newValue).ifPresent(this::selectionListener)
                );
        log.traceExit(entryMessage);
    }
}
