package kirmanak.TextSearcher;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
@Log4j2
@ToString
public class TextSearchService extends Service<List<Path>> {
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
        final EntryMessage entryMessage = log.traceEntry(
                "TextSearchService(rootFolder = {}, extension = {}, text = {})", rootFolder, extension, text
        );
        if (!Files.isDirectory(rootFolder) || !Files.isExecutable(rootFolder)) {
            throw new IllegalArgumentException("Root folder must be a executable directory.");
        }
        if (extension.isEmpty() || extension.contains(".")) {
            throw new IllegalArgumentException("Extension is incorrect.");
        }
        this.rootFolder = rootFolder.normalize();
        this.extension = extension;
        this.text = text;
        log.traceExit(entryMessage);
    }

    @Override
    protected Task<List<Path>> createTask() {
        return new TextSearchTask(getRootFolder(), getExtension(), getText());
    }

    public String getExtension() {
        return String.format(".%s", extension);
    }

    @RequiredArgsConstructor
    @Getter
    private class TextSearchTask extends Task<List<Path>> {
        private final Path rootFolder;
        private final String extension;
        private final String text;

        @Override
        protected List<Path> call() {
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
                getFuture(future, result::add);
                counter++;
                updateProgress(counter, FILE_COUNT);
            }
            result.trimToSize();
            return log.traceExit(m, result);
        }

        /**
         * Gets the future result catching the exceptions
         *
         * @param future   the future to be unwrapped
         * @param consumer the consumer of the result
         */
        private void getFuture(final Future<Optional<Path>> future, final Consumer<Path> consumer) {
            final EntryMessage m = log.traceEntry(
                    "getFuture(future = {}, consumer = {}) of {}", future, consumer, this
            );
            try {
                future.get().ifPresent(consumer);
            } catch (final InterruptedException err) {
                log.error(m, err);
                Thread.currentThread().interrupt();
            } catch (final ExecutionException err) {
                log.error(m, err);
            }
            log.traceExit(m);
        }

        /**
         * Creates a callable which tests whether the provided file has the required text
         *
         * @param path the path to be checked
         * @return the callable to be executed
         */
        private Callable<Optional<Path>> pathToCallable(final Path path) {
            final EntryMessage m = log.traceEntry("pathToCallable(path = {}) of {}", path, this);
            return log.traceExit(m, () -> {
                final EntryMessage callableEntryMessage = log.traceEntry(
                        "anonymous(path = {}) of {}", path, this
                );
                final boolean result;
                try {
                    result = Files.lines(path).anyMatch(line -> line.contains(getText()));
                } catch (final IOException err) {
                    log.error(m, err);
                    return log.traceExit(m, Optional.empty());
                }
                return log.traceExit(callableEntryMessage, result ? Optional.of(path) : Optional.empty());
            });
        }

        /**
         * Iterates over the provided paths creating list of futures
         *
         * @param iterator the iterator over the paths
         * @return list containing futures made out of the paths
         */
        private List<Future<Optional<Path>>> iterateThroughFiles(final Iterator<Path> iterator) {
            final EntryMessage m = log.traceEntry("iterateThroughFiles(iterator = {}) of {}", iterator, this);
            final ExecutorService executorService = Executors.newWorkStealingPool();
            final List<Future<Optional<Path>>> result = new LinkedList<>();
            while (iterator.hasNext()) {
                final Path current = iterator.next();
                if (shouldBeChecked(current)) {
                    result.add(executorService.submit(pathToCallable(current)));
                }
            }
            return log.traceExit(m, result);
        }

        /**
         * Checks whether the path should be read or not
         *
         * @param path the path to be checked
         * @return the decision
         */
        private boolean shouldBeChecked(final Path path) {
            final EntryMessage m = log.traceEntry("shouldBeChecked(path = {}) of {}", path, this);
            final boolean answer = path.toString().endsWith(getExtension())
                    && Files.exists(path)
                    && Files.isRegularFile(path)
                    && Files.isReadable(path);
            return log.traceExit(m, answer);
        }

        /**
         * Walks through the file system starting from the root path
         *
         * @return list of futures with results of the file reading
         */
        private List<Future<Optional<Path>>> walk() {
            final EntryMessage m = log.traceEntry("walk() of {}", this);
            final Stream<Path> stream;
            try {
                stream = Files.walk(getRootFolder(), FileVisitOption.FOLLOW_LINKS);
            } catch (final IOException err) {
                log.error(m, err);
                return log.traceExit(m, Collections.emptyList());
            }
            final List<Future<Optional<Path>>> result = iterateThroughFiles(stream.iterator());
            stream.close();
            return log.traceExit(m, result);
        }
    }
}
