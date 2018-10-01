package kirmanak.TextSearcher;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Getter
@ToString
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
    private Path root = null;

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
        final Parent parent = FXMLLoader.load(getClass().getResource("/main.fxml"));
        this.primaryStage = primaryStage;
        primaryStage.setTitle("TextSearcher");
        primaryStage.setScene(new Scene(parent));
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
        final Optional<Path> root = getRoot();
        if (!root.isPresent()) {
            final IllegalStateException err = new IllegalStateException(
                    "Can not generate a tree if the root is not present"
            );
            log.error(m, err);
            throw err;
        }
        final TreeItem<Path> treeRoot = new TreeItem<>(getRoot().get());
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
        final Optional<Path> optionalRoot = getRoot();
        if (!optionalRoot.isPresent()) {
            final IllegalStateException err = new IllegalStateException(
                    "Can not generate a list of paths if the root is not present"
            );
            log.error(m, err);
            throw err;
        }
        final Path root = optionalRoot.get();
        final LinkedList<Path> pathsList = new LinkedList<>();
        pathsList.addFirst(path.getFileName());
        Path parent = path.getParent();
        while (!parent.equals(root)) {
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
        final MarkedFileService service = new MarkedFileService(newValue, getTEXT_FIELD().getText());
        service.setOnRunning(event -> {
            getPROGRESS_INDICATOR().setVisible(true);
            getPROGRESS_INDICATOR().progressProperty().unbind();
            getPROGRESS_INDICATOR().progressProperty().bind(event.getSource().progressProperty());
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
        getTAB_PANE().getTabs().removeIf(tab -> tab.getText().equals(path.toString()));
        getTAB_PANE().getTabs().add(new Tab(path.toString(), textArea));
        log.traceExit(m);
    }

    /**
     * Creates a TextSearchService and starts the search
     */
    @FXML
    protected void onSearchRequest() {
        final EntryMessage entryMessage = log.traceEntry("onSearchRequest() of {}", this);
        final Optional<Path> optionalRoot = getRoot();
        if (!optionalRoot.isPresent()) {
            return;
        }
        final TextSearchService service;
        try {
            service = new TextSearchService(optionalRoot.get(), getEXTENSION_FIELD().getText(), getTEXT_FIELD().getText());
        } catch (final IllegalArgumentException err) {
            log.error(entryMessage, err);
            return;
        }
        service.setOnRunning(event -> {
            getPROGRESS_INDICATOR().setVisible(true);
            getPROGRESS_INDICATOR().progressProperty().unbind();
            getPROGRESS_INDICATOR().progressProperty().bind(event.getSource().progressProperty());
        });
        service.setOnSucceeded(stateEvent -> {
            //noinspection unchecked
            getTREE_VIEW().setRoot(generateTree((List<Path>) stateEvent.getSource().getValue()));
            getPROGRESS_INDICATOR().setVisible(false);
        });
        service.start();
        log.traceExit(entryMessage);
    }

    /**
     * Gets root based on the text field and directory chooser
     *
     * @return the root folder to start search
     */
    private Optional<Path> getRoot() {
        final EntryMessage entryMessage = log.traceEntry("getRoot() of {}", this);
        final String folderPath = getPATH_FIELD().getText();
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
        final DirectoryChooser chooser = new DirectoryChooser();
        final Optional<Path> optionalPath = getRoot();
        optionalPath.ifPresent(path -> chooser.setInitialDirectory(path.toFile()));
        final File file = chooser.showDialog(primaryStage);
        Optional.ofNullable(file).ifPresent(newRoot -> root = file.toPath());
        Optional.ofNullable(root).ifPresent(path -> getPATH_FIELD().setText(root.toString()));
    }
}
