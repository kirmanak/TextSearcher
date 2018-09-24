package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

@Getter
@Log4j2
@ToString
public class TextSearcher {
    private final Path rootFolder;
    private final String extension;
    private final String text;
    private final ForkJoinPool pool = new ForkJoinPool();
    private final List<ForkJoinTask<Optional<MarkedFile>>> tasks = new ArrayList<>();

    /**
     * Creates a new instance of TextSearcher
     *
     * @param rootFolder Path from which to start the searching
     * @param extension  Extension of target files
     * @param text       Text to search
     */
    public TextSearcher(final Path rootFolder, final String extension, final String text) {
        final EntryMessage entryMessage = log.traceEntry("TextSearcher(rootFolder = {}, extension = {}, text = {})", rootFolder, extension, text);
        if (!rootFolder.toFile().isDirectory() || !rootFolder.toFile().canExecute()) {
            final IllegalArgumentException err = new IllegalArgumentException("Root folder must be a executable directory.");
            log.error(entryMessage, err);
            throw err;
        }
        if (extension.contains(".")) {
            final IllegalArgumentException err = new IllegalArgumentException("Extension must not contain a dot.");
            log.error(entryMessage, err);
            throw err;
        }
        this.rootFolder = rootFolder.normalize();
        this.extension = extension;
        this.text = text;
        log.traceExit(entryMessage);
    }

    /**
     * Finds all files with the required extension and the required text in them
     *
     * @return a list containing all files with the required extension and the required text
     */
    public List<MarkedFile> getFiles() {
        final EntryMessage entryMessage = log.traceEntry("getFiles() of {}", this);
        helper(getRootFolder().toFile());
        final List<MarkedFile> result = tasks.stream()
                .map(this::getResult)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        return log.traceExit(entryMessage, result);
    }

    /**
     * Recursively calls itself, submiting Callables marking files to the pool
     *
     * @param directory directory from which to start
     */
    private void helper(final File directory) {
        final EntryMessage entryMessage = log.traceEntry("helper(directory = {}) of {}", directory, this);
        final File[] files = directory.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    helper(file);
                } else {
                    tasks.add(pool.submit(() -> markFile(file.toPath())));
                }
            }
        }
        log.traceExit(entryMessage);
    }

    /**
     * Unwraps markup results from ForkJoinTask
     *
     * @param task the target ForkJoinTask
     * @return optional containing the markup results
     */
    private Optional<MarkedFile> getResult(final ForkJoinTask<Optional<MarkedFile>> task) {
        final EntryMessage entryMessage = log.traceEntry("getResult(task = {}) of {}", task, this);
        Optional<MarkedFile> result;
        try {
            result = task.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            result = Optional.empty();
        } catch (final ExecutionException e) {
            log.error("Error: {}", e);
            result = Optional.empty();
        }
        return log.traceExit(entryMessage, result);
    }

    /**
     * Markups the file
     *
     * @param path the target file
     * @return optional containing markup results
     */
    private Optional<MarkedFile> markFile(final Path path) {
        final EntryMessage entryMessage = log.traceEntry("markFile(path = {}) of {}", path, this);
        final MarkedFile marked;
        try {
            marked = new MarkedFile(path, getText());
        } catch (final IOException err) {
            return log.traceExit(entryMessage, Optional.empty());
        }
        return log.traceExit(entryMessage, Optional.of(marked));
    }
}
