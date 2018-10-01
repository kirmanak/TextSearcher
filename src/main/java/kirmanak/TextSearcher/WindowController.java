package kirmanak.TextSearcher;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Getter
@Setter
@ToString
public class WindowController {
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
     * "/home/user/folder/file.log" becomes "[user, folder, file.log]" if the root is "/home"
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
     * Called when a new item is selected in the treeView
     *
     * @param newValue the new selected item
     */
    private void selectionListener(final Path newValue) {
        final EntryMessage m = log.traceEntry("selectionListener(newValue = {})", newValue);
        final MarkedFileService service = new MarkedFileService(newValue, getTextField().getText());
        service.setOnRunning(event -> {
            getProgressIndicator().setVisible(true);
            getProgressIndicator().progressProperty().unbind();
            getProgressIndicator().progressProperty().bind(event.getSource().progressProperty());
        });
        service.setOnFailed(stateEvent -> getProgressIndicator().setVisible(false));
        service.setOnSucceeded(stateEvent -> {
            addTab((TextArea) stateEvent.getSource().getValue(), newValue);
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
    private void goUp(final TreeItem<Path> item) {
        final EntryMessage entryMessage = log.traceEntry("goUp(item = {})", item);
        final Optional<Path> optionalPath = getRoot();
        if (optionalPath.isPresent()) {
            final Path path = optionalPath.get();
            TreeItem<Path> current = item;
            Path result = item.getValue();
            while (!current.getValue().equals(path)) {
                current = current.getParent();
                result = current.getValue().resolve(result);
            }
            selectionListener(result);
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
                "addTab(textArea = {}, path = {}) of {}", textArea, path, this
        );
        getTabPane().getTabs().removeIf(tab -> tab.getText().equals(path.toString()));
        getTabPane().getTabs().add(new Tab(path.toString(), textArea));
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
            service = new TextSearchService(optionalRoot.get(), getExtensionField().getText(), getTextField().getText());
        } catch (final IllegalArgumentException err) {
            log.error(entryMessage, err);
            return;
        }
        service.setOnRunning(event -> {
            getProgressIndicator().setVisible(true);
            getProgressIndicator().progressProperty().unbind();
            getProgressIndicator().progressProperty().bind(event.getSource().progressProperty());
        });
        service.setOnFailed(stateEvent -> getProgressIndicator().setVisible(false));
        service.setOnSucceeded(stateEvent -> {
            //noinspection unchecked
            getTreeView().setRoot(generateTree((List<Path>) stateEvent.getSource().getValue()));
            getProgressIndicator().setVisible(false);
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
        final DirectoryChooser chooser = new DirectoryChooser();
        final Optional<Path> optionalPath = getRoot();
        optionalPath.ifPresent(path -> chooser.setInitialDirectory(path.toFile()));
        final File file = chooser.showDialog(getPrimaryStage());
        Optional.ofNullable(file).ifPresent(newRoot -> root = file.toPath());
        Optional.ofNullable(root).ifPresent(path -> getPathField().setText(root.toString()));
    }

    @FXML
    public void initialize() {
        getTreeView()
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) ->
                        Optional.ofNullable(newValue).ifPresent(this::goUp)
                );
    }
}
