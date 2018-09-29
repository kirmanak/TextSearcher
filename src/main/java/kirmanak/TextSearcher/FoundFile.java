package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a marked file: file and list of marked sub-strings.
 */
@Log4j2
@Getter
@RequiredArgsConstructor
public class FoundFile {
    private final Path path;
    private final String content;

    /**
     * Marks the passed file if the required text is present
     *
     * @param path the file to be marked
     * @param text the required text
     * @return the marked file if file has been opened and text is present, empty otherwise
     */
    public static Optional<FoundFile> of(final Path path, final String text) {
        final EntryMessage entryMessage = log.traceEntry("of(path = {}, text = {})", path, text);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            return log.traceExit(entryMessage, Optional.empty());
        }
        try {
            return log.traceExit(entryMessage, check(path, Files.lines(path), text));
        } catch (final IOException err) {
            log.error(entryMessage, err);
            return log.traceExit(entryMessage, Optional.empty());
        }
    }

    /**
     * Checks whether the provided stream contains the provided text. If yes, creates a FoundFile instance
     *
     * @param path        the path to the file containing the provided stream
     * @param linesStream the lines stream containing lines to be checked
     * @param text        the text to be found in the stream
     * @return empty if an error has happened or the file does not contain the text
     */
    private static Optional<FoundFile> check(final Path path, final Stream<String> linesStream, final String text) {
        final EntryMessage m = log.traceEntry("check(path = {}, linesStream = {}, text = {})", path, linesStream, text);
        final Iterator<String> linesIterator = linesStream.iterator();
        final StringBuilder stringBuilder = new StringBuilder();
        while (linesIterator.hasNext()) {
            final String line = linesIterator.next();
            stringBuilder.append(line).append("\n");
            if (line.contains(text)) {
                linesIterator.forEachRemaining(string -> stringBuilder.append(string).append("\n"));
                return log.traceExit(m, Optional.of(new FoundFile(path, stringBuilder.toString())));
            }
        }
        return log.traceExit(m, Optional.empty());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof FoundFile && getPath().equals(((FoundFile) obj).getPath());
    }

    @Override
    public String toString() {
        return getPath().toString();
    }
}
