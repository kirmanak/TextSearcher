package kirmanak.TextSearcher;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Represents a marked file: file and list of marked sub-strings.
 */
@Getter
@Log4j2
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode
public class MarkedFile {
    private final Path path;
    private final Collection<Markup> markups;
    private final List<String> lines;

    /**
     * Marks the passed file if the required text is present
     *
     * @param path the file to be marked
     * @param text the required text
     * @return the marked file if file has been opened and text is present, empty otherwise
     */
    public static Optional<MarkedFile> of(final Path path, final String text) {
        final EntryMessage entryMessage = log.traceEntry("of(path = {}, text = {})", path, text);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            return log.traceExit(entryMessage, Optional.empty());
        }
        final List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (final IOException err) {
            log.error(entryMessage, err);
            return log.traceExit(entryMessage, Optional.empty());
        }
        final Collection<Markup> markups = markup(lines, text);
        return log.traceExit(
                entryMessage, markups.isEmpty() ? Optional.empty() : Optional.of(new MarkedFile(path, markups, lines))
        );
    }

    /**
     * Creates markup for the given strings
     *
     * @param lines the given strings
     * @param text  the text to search
     * @return a set of markups
     */
    private static Collection<Markup> markup(final Collection<String> lines, final String text) {
        final EntryMessage entryMessage = log.traceEntry("markup(path = {}, text = {})", lines, text);
        final Collection<Markup> markups = new ArrayList<>(lines.size());
        final int length = text.length();
        int lineNumber = 0;
        for (final String line : lines) {
            int rangeStart = line.indexOf(text);
            while (rangeStart >= 0) {
                markups.add(new Markup(lineNumber, rangeStart, length));
                rangeStart = line.indexOf(text, rangeStart + 1);
            }
            lineNumber++;
        }
        return log.traceExit(entryMessage, markups);
    }
}
