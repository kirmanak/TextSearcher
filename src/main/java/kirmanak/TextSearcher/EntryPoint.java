package kirmanak.TextSearcher;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

@Log4j2
public class EntryPoint extends Application {
    @FXML
    private TextField PATH_FIELD;
    @FXML
    private TextField EXTENSION_FIELD;
    @FXML
    private TextField TEXT_FIELD;
    @FXML
    private TabPane TAB_PANE;
    @FXML
    private ProgressIndicator PROGRESS_INDICATOR;
    @FXML
    private TreeView<Path> TREE_VIEW;
    private Stage primaryStage;
    private Path root;

    public static void main(String[] args) {
        launch();
    }

    /**
     * Starts the GUI
     *
     * @param primaryStage the main window
     */
    public void start(final Stage primaryStage) throws IOException {
        final EntryMessage entryMessage = log.traceEntry("start(primaryStage = {}) of {}", primaryStage, this);
        final Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
        this.primaryStage = primaryStage;
        primaryStage.setTitle("TextSearcher");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        log.traceExit(entryMessage);
    }

    /**
     * Adds paths from the list to the provided treeRoot if they are not present
     *
     * @param treeRoot the treeRoot which should contain all paths from the list
     * @param list     paths to add to the treeRoot
     */
    private static void addPaths(final TreeItem<Path> treeRoot, final List<Path> list) {
        final EntryMessage m = log.traceEntry("addPaths(treeRoot = {}, list = {})", treeRoot, list);
        TreeItem<Path> currentRoot = treeRoot;
        main:
        for (final Path path : list) {
            for (final TreeItem<Path> child : currentRoot.getChildren()) {
                if (child.getValue().equals(path)) {
                    currentRoot = child;
                    continue main;
                }
            }
            final TreeItem<Path> newRoot = new TreeItem<>(path);
            currentRoot.getChildren().add(newRoot);
            currentRoot = newRoot;
        }
        log.traceExit(m);
    }

    /**
     * Generates a root TreeItem containing all found files
     *
     * @param paths the paths to be wrapped
     * @return the root of TreeView
     */
    private TreeItem<Path> generateTree(final List<Path> paths) {
        final EntryMessage m = log.traceEntry("generateTree(paths = {}) of {}", paths, this);
        final TreeItem<Path> treeRoot = new TreeItem<>(this.root);
        treeRoot.setExpanded(true);
        paths.stream().map(this::listOfPaths).forEach(list -> addPaths(treeRoot, list));
        return log.traceExit(m, treeRoot);
    }

    /**
     * Generates a list of directories to find this path from the current root.
     * "/home/user/folder/file.log" becomes "[user, folder, file.log]" if the root is "home"
     *
     * @param path the path to the file
     * @return the list of paths
     */
    private List<Path> listOfPaths(final Path path) {
        final EntryMessage m = log.traceEntry("listOfPaths(path = {}) of {}", path, this);
        final LinkedList<Path> pathsList = new LinkedList<>();
        pathsList.addFirst(path.getFileName());
        Path parent = path.getParent();
        while (!parent.equals(this.root)) {
            pathsList.addFirst(parent.getFileName());
            parent = parent.getParent();
        }
        return log.traceExit(m, pathsList);
    }

    /**
     * Called when a new item is selected in the listView
     *
     * @param oldValue the previously selected item
     * @param newValue the new selected item
     */
    private void selectionListener(final Path oldValue, final Path newValue) {
        final EntryMessage m = log.traceEntry("selectionListener(oldValue = {}, newValue = {}", oldValue, newValue);
        if (newValue == null) {
            return;
        }
        final MarkedFileService service = new MarkedFileService(newValue, TEXT_FIELD.getText());
        service.setOnRunning(event -> {
            PROGRESS_INDICATOR.setVisible(true);
            PROGRESS_INDICATOR.progressProperty().unbind();
            PROGRESS_INDICATOR.progressProperty().bind(event.getSource().progressProperty());
        });
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
        TAB_PANE.getTabs().removeIf(tab -> tab.getText().equals(path.toString()));
        TAB_PANE.getTabs().add(new Tab(path.toString(), textArea));
        log.traceExit(m);
    }

    /**
     * Creates a TextSearchService and starts the search
     */
    @FXML
    protected void onSearchRequest() {
        final EntryMessage entryMessage = log.traceEntry("onSearchRequest() of {}", this);
        if (root == null) {
            try {
                root = Paths.get(PATH_FIELD.getText());
            } catch (final InvalidPathException err) {
                log.error(entryMessage, err);
                return;
            }
        }
        final String extension = EXTENSION_FIELD.getText();
        final String text = TEXT_FIELD.getText();
        final TextSearchService service;
        try {
            service = new TextSearchService(root, extension, text);
        } catch (final IllegalArgumentException err) {
            log.error(entryMessage, err);
            return;
        }
        service.setOnRunning(event -> {
            PROGRESS_INDICATOR.setVisible(true);
            PROGRESS_INDICATOR.progressProperty().unbind();
            PROGRESS_INDICATOR.progressProperty().bind(event.getSource().progressProperty());
        });
        service.setOnSucceeded(stateEvent -> {
            //noinspection unchecked
            TREE_VIEW.setRoot(generateTree((List<Path>) stateEvent.getSource().getValue()));
            PROGRESS_INDICATOR.setVisible(false);
        });
        service.start();
        log.traceExit(entryMessage);
    }

    @FXML
    protected void showDirectoryChooser() {
        final DirectoryChooser chooser = new DirectoryChooser();
        if (root != null) {
            chooser.setInitialDirectory(root.toFile());
        }
        final File file = chooser.showDialog(primaryStage);
        root = file == null ? null : file.toPath();
        PATH_FIELD.setText(root == null ? "" : root.toString());
    }
}
