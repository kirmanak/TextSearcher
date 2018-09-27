package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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

    /**
     * Creates a new instance of TextSearcher
     *
     * @param rootFolder Path from which to start the searching
     * @param extension  Extension of target files
     * @param text       Text to search
     */
    public TextSearcher(final Path rootFolder, final String extension, final String text) throws IllegalArgumentException {
        final EntryMessage entryMessage = log.traceEntry("TextSearcher(rootFolder = {}, extension = {}, text = {})", rootFolder, extension, text);
        if (!rootFolder.toFile().isDirectory() || !rootFolder.toFile().canExecute()) {
            final IllegalArgumentException err = new IllegalArgumentException("Root folder must be a executable directory.");
            log.error(entryMessage, err);
            throw err;
        }
        if (extension.isEmpty() || extension.contains(".")) {
            final IllegalArgumentException err = new IllegalArgumentException("Extension is incorrect.");
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
     * @return a set containing all files with the required extension and the required text
     */
    public Collection<MarkedFile> getFiles() throws IOException {
        final EntryMessage entryMessage = log.traceEntry("getFiles() of {}", this);
        final ForkJoinPool pool = new ForkJoinPool();
        final Collection<MarkedFile> result = pool.invoke(ForkJoinTask.adapt(() ->
                Files.walk(getRootFolder(), FileVisitOption.FOLLOW_LINKS)
                        .map(path -> pool.submit(() -> MarkedFile.of(path, getText())))
                        .map(this::getResult)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()))
        );
        return log.traceExit(entryMessage, result);
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
}
