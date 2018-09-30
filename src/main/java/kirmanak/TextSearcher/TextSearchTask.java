package kirmanak.TextSearcher;

import javafx.concurrent.Task;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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

@Log4j2
@RequiredArgsConstructor
@Getter
public class TextSearchTask extends Task<List<Path>> {
    private final Path rootFolder;
    private final String extension;
    private final String text;

    @Override
    protected List<Path> call() throws IOException {
        final EntryMessage m = log.traceEntry("call() of {}", this);
        updateMessage("Looking for files...");
        final List<Future<Optional<Path>>> futures = walk();
        updateMessage("Reading files...");
        final List<Path> result = getResult(futures);
        updateMessage("Done");
        return log.traceExit(m, result);
    }

    /**
     * Calls future.get() and handles the result
     *
     * @param futures the futures to be unwrapped
     * @return the unwrapping results
     */
    private List<Path> getResult(final List<Future<Optional<Path>>> futures) {
        final EntryMessage m = log.traceEntry("getResult(futures = {}) of {}", futures, this);
        final int FILE_COUNT = futures.size();
        final ArrayList<Path> result = new ArrayList<>(FILE_COUNT);
        int counter = 0;
        for (final Future<Optional<Path>> future : futures) {
            try {
                future.get().ifPresent(result::add);
            } catch (final InterruptedException err) {
                log.error(m, err);
                Thread.currentThread().interrupt();
            } catch (final ExecutionException err) {
                log.error(m, err);
            }
            counter++;
            updateProgress(counter, FILE_COUNT);
        }
        result.trimToSize();
        return log.traceExit(m, result);
    }

    /**
     * Creates a callable which tests whether the provided file has the required text
     *
     * @param path the path to be checked
     * @return the callable to be executed
     */
    private Callable<Optional<Path>> pathToCallable(final Path path) {
        final EntryMessage m = log.traceEntry("pathToCallable(path = {})", path);
        return log.traceExit(m, () -> {
            final boolean result = Files.lines(path).anyMatch(line -> line.contains(getText()));
            return (result) ? Optional.of(path) : Optional.empty();
        });
    }

    /**
     * Walks through the file system starting from the root path
     *
     * @return list of futures with results of the file reading
     * @throws IOException if any I/O error has happened
     */
    private List<Future<Optional<Path>>> walk() throws IOException {
        final EntryMessage m = log.traceEntry("walk() of {}", this);
        final ExecutorService executorService = Executors.newWorkStealingPool();
        return log.traceExit(m, Files.walk(getRootFolder(), FileVisitOption.FOLLOW_LINKS)
                .filter(path -> path.toString().endsWith(getExtension()))
                .map(this::pathToCallable)
                .map(executorService::submit)
                .collect(Collectors.toList()));
    }
}
