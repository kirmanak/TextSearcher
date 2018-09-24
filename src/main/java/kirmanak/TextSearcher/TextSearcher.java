package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

@Getter
@Log4j2
public class TextSearcher {
    private final Path rootFolder;
    private final String extension;
    private final String text;
    private final ForkJoinPool pool = new ForkJoinPool();

    /**
     * Creates a new instance of TextSearcher
     *
     * @param rootFolder Path from which to start the searching
     * @param extension  Extension of target files
     * @param text       Text to search
     */
    public TextSearcher(final Path rootFolder, final String extension, final String text) {
        log.traceEntry("TextSearcher(rootFolder = {}, extension = {}, text = {})", rootFolder, extension, text);
        if (!rootFolder.toFile().isDirectory() || !rootFolder.toFile().canExecute()) {
            throw new IllegalArgumentException("Root folder must be a executable directory.");
        }
        if (extension.contains(".")) {
            throw new IllegalArgumentException("Extension must not contain a dot.");
        }
        this.rootFolder = rootFolder.normalize();
        this.extension = extension;
        this.text = text;
        log.traceExit();
    }

    /**
     * Finds all files with the required extension and the required text in them
     *
     * @return a list containing all files with the required extension and the required text
     */
    public List<Path> getFiles() {
        log.traceEntry("Enter TextSearcher.getFiles()");
        return log.traceExit(getFilesWithExtension().stream()
                .map((file) -> (Callable<Optional<Path>>) () -> testFile(file))
                .map(pool::submit)
                .map(ForkJoinTask::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList())
        );
    }

    /**
     * Tests whether the file contains required text or not
     *
     * @param file the file to be tested
     * @return returns Optional containing this file if it does, empty otherwise
     */
    private Optional<Path> testFile(final Path file) {
        log.traceEntry("Enter testFile(file = {})", file);
        final List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (final IOException err) {
            return Optional.empty();
        }
        return log.traceExit(lines.stream()
                .map((line) -> (Callable<Boolean>) () -> line.contains(getText()))
                .map(pool::submit)
                .anyMatch(ForkJoinTask::join) ? Optional.of(file) : Optional.empty()
        );
    }

    /**
     * Iterates over all sub-directories to create a list containing all files with the required extension.
     *
     * @return list containing all files with extension given by this::getExtension
     */
    private List<Path> getFilesWithExtension() {
        log.traceEntry("getFilesWithExtension()");
        final LinkedList<File> directories = new LinkedList<>(Collections.singletonList(getRootFolder().toFile()));
        final List<Path> correspondingFiles = new ArrayList<>();
        while (!directories.isEmpty()) {
            final File head;
            try {
                head = directories.pop();
            } catch (final NoSuchElementException err) {
                log.error("directories.pop() has thrown NoSuchElementException = {}", err);
                continue;
            }
            final File[] files = head.listFiles();
            if (files == null) {
                log.warn("Skipping {} is not accessible. Skip", head);
                continue;
            }
            for (final File file : files) {
                if (file.isDirectory() && file.canExecute()) {
                    directories.add(file);
                } else if (file.getName().endsWith(getExtension())) {
                    correspondingFiles.add(file.toPath());
                }
            }
        }
        return log.traceExit(correspondingFiles);
    }
}
