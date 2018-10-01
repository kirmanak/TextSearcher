package kirmanak.TextSearcher;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

@Getter
@RequiredArgsConstructor
@Log4j2
public class TreeGeneratorService extends Service<TreeItem<Path>> {
    private final List<Path> paths;
    private final Path root;

    @Override
    protected Task<TreeItem<Path>> createTask() {
        return new TreeGeneratorTask();
    }

    private class TreeGeneratorTask extends Task<TreeItem<Path>> {

        @Override
        protected TreeItem<Path> call() {
            return generateTree();
        }

        /**
         * Generates a root TreeItem containing all found files
         *
         * @return the root of TreeView
         */
        private TreeItem<Path> generateTree() {
            final EntryMessage m = log.traceEntry("generateTree() of {}", this);
            final TreeItem<Path> treeRoot = new TreeItem<>(getRoot());
            treeRoot.setExpanded(true);
            getPaths().stream().map(this::listOfPaths).forEach(list -> addPaths(treeRoot, list));
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
            final LinkedList<Path> pathsList = new LinkedList<>();
            pathsList.addFirst(path.getFileName());
            Path parent = path.getParent();
            while (!parent.equals(getRoot())) {
                pathsList.addFirst(parent.getFileName());
                parent = parent.getParent();
            }
            return log.traceExit(m, pathsList);
        }

        /**
         * Adds paths from the list to the provided treeRoot if they are not present
         *
         * @param treeRoot the treeRoot which should contain all paths from the list
         * @param list     paths to add to the treeRoot
         */
        private void addPaths(final TreeItem<Path> treeRoot, final List<Path> list) {
            final EntryMessage m = log.traceEntry("addPaths(treeRoot = {}, list = {})", treeRoot, list);
            TreeItem<Path> currentRoot = treeRoot;
            final int max = list.size();
            int counter = 0;
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
                updateProgress(counter, max);
            }
            log.traceExit(m);
        }
    }
}
