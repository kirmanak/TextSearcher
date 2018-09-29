package kirmanak.TextSearcher;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Getter
@Log4j2
@ToString
public class TextSearchService extends Service<List<MarkedFile>> {
    private final Path rootFolder;
    private final String extension;
    private final String text;

    /**
     * Creates a new instance of TextSearchService
     *
     * @param rootFolder Path from which to start the searching
     * @param extension  Extension of target files
     * @param text       Text to search
     */
    public TextSearchService(final Path rootFolder, final String extension, final String text) throws IllegalArgumentException {
        final EntryMessage entryMessage = log.traceEntry("TextSearchService(rootFolder = {}, extension = {}, text = {})", rootFolder, extension, text);
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

    @Override
    protected Task<List<MarkedFile>> createTask() {
        return new Task<List<MarkedFile>>() {
            @Override
            protected List<MarkedFile> call() throws InterruptedException, ExecutionException, IOException {
                final EntryMessage m = log.traceEntry("call() of {}", this);
                final ExecutorService executorService = Executors.newWorkStealingPool();
                final List<Future<Optional<MarkedFile>>> futures =
                        Files.walk(getRootFolder(), FileVisitOption.FOLLOW_LINKS)
                                .map(path -> (Callable<Optional<MarkedFile>>) () -> MarkedFile.of(path, getText()))
                                .map(executorService::submit)
                                .collect(Collectors.toList());
                final int FILE_COUNT = futures.size();
                final ArrayList<MarkedFile> result = new ArrayList<>(FILE_COUNT);
                int counter = 0;
                for (final Future<Optional<MarkedFile>> future : futures) {
                    future.get().ifPresent(result::add);
                    counter++;
                    updateProgress(counter, FILE_COUNT);
                }
                result.trimToSize();
                return log.traceExit(m, result);
            }
        };
    }
}
