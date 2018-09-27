package kirmanak.TextSearcher;

import javafx.scene.text.Text;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a marked file: file and list of marked sub-strings.
 */
@Log4j2
@Getter
public class MarkedFile {
    private final Path path;
    private final Collection<Markup> markups;
    private final List<String> lines;
    private final Text[] texts;

    public MarkedFile(final Path path, final Collection<Markup> markups, final List<String> lines) {
        final EntryMessage entryMessage = log.traceEntry(
                "MarkedFile(path = {}, markups = {}, lines = {})", path, markups, lines
        );
        this.path = path;
        this.markups = markups;
        this.lines = lines;
        texts = generateTexts().toArray(new Text[0]);
        log.traceExit(entryMessage);
    }

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
        try (final Stream<String> linesStream = Files.lines(path)) {
            return log.traceExit(entryMessage, markup(path, linesStream, text));
        } catch (final IOException err) {
            log.error(entryMessage, err);
            return log.traceExit(entryMessage, Optional.empty());
        }
    }

    /**
     * Creates markup for the given strings
     *
     * @param linesStream the given strings
     * @param text        the text to search
     * @return a set of markups
     */
    private static Optional<MarkedFile> markup(final Path path, final Stream<String> linesStream, final String text) {
        final EntryMessage entryMessage = log.traceEntry(
                "markup(path = {}, linesStream = {}, text = {})", path, linesStream, text
        );
        final ArrayList<Markup> markups = new ArrayList<>();
        final int length = text.length();
        final AtomicInteger lineNumber = new AtomicInteger(0);
        final List<String> lines = linesStream.peek((line) -> {
            int rangeStart = line.indexOf(text);
            while (rangeStart >= 0) {
                markups.add(new Markup(lineNumber.get(), rangeStart, length));
                rangeStart = line.indexOf(text, rangeStart + 1);
            }
            lineNumber.incrementAndGet();
        }).collect(Collectors.toList());
        markups.trimToSize();
        return log.traceExit(
                entryMessage, markups.isEmpty() ? Optional.empty() : Optional.of(new MarkedFile(path, markups, lines))
        );
    }

    /**
     * Generates list of Text instances containing the found file highlighted content
     *
     * @return list of the texts
     */
    private List<Text> generateTexts() {
        final EntryMessage entryMessage = log.traceEntry("generateTexts() of {}", this);
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
        return log.traceExit(entryMessage, textList);
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
        if (obj == null || obj.hashCode() != hashCode()) {
            return false;
        }
        if (obj instanceof MarkedFile) {
            final MarkedFile markedFile = (MarkedFile) obj;
            return getPath().equals(markedFile.getPath());
        }
        return false;
    }

    @Override
    public String toString() {
        return getPath().toString();
    }
}
