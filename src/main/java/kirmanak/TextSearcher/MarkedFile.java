package kirmanak.TextSearcher;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a marked file: file and list of marked sub-strings.
 */
@Getter
@Log4j2
@RequiredArgsConstructor
public class MarkedFile {
    private final Path path;
    private final Collection<Markup> markups;
    private final List<String> lines;
    private TextFlow textFlow = null;

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
        final EntryMessage entryMessage = log.traceEntry("markup(lines = {}, text = {})", lines, text);
        final ArrayList<Markup> markups = new ArrayList<>(lines.size());
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
        markups.trimToSize();
        return log.traceExit(entryMessage, markups);
    }

    /**
     * Creates a TextFlow out of the MarkedFile
     *
     * @return a TextFlow with highlighted text
     */
    public TextFlow getTextFlow() {
        final EntryMessage entryMessage = log.traceEntry("getTextFlow() of {}", this);
        if (textFlow != null) {
            return textFlow;
        }
        final List<Text> textList = new ArrayList<>(lines.size() * 2);
        final Map<Integer, List<Markup>> markupsPerLine = markups.stream()
                .collect(Collectors.groupingBy(Markup::getLineNumber, Collectors.toList()));
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i).concat("\n");
            if (markupsPerLine.containsKey(i)) {
                textList.addAll(textsFromLine(line, markupsPerLine.get(i)));
            } else {
                textList.add(new Text(line));
            }
        }
        textFlow = new TextFlow(textList.toArray(new Text[0]));
        return log.traceExit(entryMessage, textFlow);
    }

    /**
     * Generates a list of Text instances from the given line and markups for it
     *
     * @param line           the line content
     * @param markupsPerLine the markups for the text in the line
     * @return list of Text instances (some have a color, some not)
     */
    private List<Text> textsFromLine(final String line, final List<Markup> markupsPerLine) {
        final EntryMessage entryMessage = log.traceEntry(
                "textsFromLine(line = {}, markupsPerLine = {}) of {}", line, markupsPerLine, this
        );
        final List<Text> textList = new ArrayList<>();
        final Iterator<Markup> iterator = markupsPerLine.iterator();
        Markup last = iterator.next();
        if (last.getRangeStart() > 0) {
            textList.add(new Text(line.substring(0, last.getRangeStart())));
        }
        textList.add(last.toText(line));
        while (iterator.hasNext()) {
            final Markup previous = last;
            last = iterator.next();
            if (last.getRangeStart() > previous.getRangeEnd()) {
                textList.add(new Text(line.substring(previous.getRangeEnd(), last.getRangeStart())));
            }
            textList.add(last.toText(line));
        }
        if (last.getRangeEnd() < line.length()) {
            textList.add(new Text(line.substring(last.getRangeEnd())));
        }
        return log.traceExit(entryMessage, textList);
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MarkedFile) {
            final MarkedFile markedFile = (MarkedFile) obj;
            return markedFile.getPath().equals(getPath());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getPath().toString();
    }
}
