package kirmanak.TextSearcher;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
@Log4j2
@ToString
@EqualsAndHashCode
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
        final EntryMessage entryMessage = log.traceEntry(
                "TextSearcher(rootFolder = {}, extension = {}, text = {})", rootFolder, extension, text
        );
        if (!rootFolder.toFile().isDirectory() || !rootFolder.toFile().canExecute()) {
            final IllegalArgumentException err = new IllegalArgumentException(
                    "Root folder must be a executable directory."
            );
            log.error(entryMessage, err);
            throw err;
        }
        if (extension.isEmpty() || extension.contains(".")) {
            final IllegalArgumentException err = new IllegalArgumentException("Extension is incorrect.");
            log.error(entryMessage, err);
            throw err;
        }
        this.rootFolder = rootFolder.normalize();
        this.extension = String.format(".%s", extension);
        this.text = text;
        log.traceExit(entryMessage);
    }

    /**
     * Finds all files with the required extension and the required text in them
     *
     * @return a collection containing all files with the required extension and the required text
     */
    public void search(final Consumer<FoundFile> callBack) throws IOException {
        final EntryMessage entryMessage = log.traceEntry("search(callBack = {}) of {}", callBack, this);
        final Stream<Path> paths = Files.walk(getRootFolder(), FileVisitOption.FOLLOW_LINKS);
        final ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.execute(() ->
                paths.filter(path -> path.toString().endsWith(getExtension()))
                        .map(path -> checkFile(path, callBack))
                        .forEach(pool::execute)
        );

        log.traceExit(entryMessage);
    }

    /**
     * Creates a Runnable which checks the file and calls a callback if the file contains the text
     *
     * @param path     the path to the file
     * @param callBack the callBack to be called if the file contains the required text
     * @return the created Runnable instance
     */
    private Runnable checkFile(final Path path, final Consumer<FoundFile> callBack) {
        final EntryMessage entryMessage = log.traceEntry("checkFile(path = {}, callBack = {})", path, callBack);
        return log.traceExit(entryMessage, () -> FoundFile.of(path, getText()).ifPresent(callBack));
    }
}
