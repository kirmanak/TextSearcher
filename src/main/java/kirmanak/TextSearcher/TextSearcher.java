package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
     * @return a collection containing all files with the required extension and the required text
     */
    public void search(final Consumer<MarkedFile> callBack) throws IOException {
        final EntryMessage entryMessage = log.traceEntry("search(callBack = {}) of {}", callBack, this);
        final Stream<Path> paths = Files.walk(getRootFolder(), FileVisitOption.FOLLOW_LINKS);
        ForkJoinPool.commonPool().execute(() -> paths.map(path -> (Runnable) () -> {
                    final Optional<MarkedFile> optional = MarkedFile.of(path, getText());
                    optional.ifPresent(callBack);
                }).forEach(runnable -> ForkJoinPool.commonPool().execute(runnable))
        );

        log.traceExit(entryMessage);
    }
}
