package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a marked file: file and list of marked sub-strings.
 */
@Getter
@Log4j2
@ToString
@RequiredArgsConstructor
public class MarkedFile {
    private final Path path;
    private final List<Markup> markups;

    /**
     * Marks the passed file if the required text is present
     *
     * @param path the file to be marked
     * @param text the required text
     * @return the marked file if file has been opened and text is present, empty otherwise
     */
    public static Optional<MarkedFile> of(final Path path, final String text) {
        final EntryMessage entryMessage = log.traceEntry("MarkedFile(path = {}, text = {})", path, text);
        final List<Markup> markups = new ArrayList<>();
        final int length = text.length();
        final List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (final IOException err) {
            log.error(entryMessage, err);
            return log.traceExit(entryMessage, Optional.empty());
        }
        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            final String line = lines.get(lineNumber);
            int rangeStart = line.indexOf(text);
            while (rangeStart >= 0) {
                markups.add(new Markup(lineNumber, rangeStart, length));
                rangeStart = line.indexOf(text, rangeStart + 1);
            }
        }
        return log.traceExit(
                entryMessage, markups.isEmpty() ? Optional.empty() : Optional.of(new MarkedFile(path, markups))
        );
    }
}
