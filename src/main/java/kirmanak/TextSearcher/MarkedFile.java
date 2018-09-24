package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Log4j2
@ToString
public class MarkedFile {
    private final Path path;
    private final List<Markup> markups = new ArrayList<>();

    public MarkedFile(final Path path, final String text) throws IOException {
        final EntryMessage entryMessage = log.traceEntry("MarkedFile(path = {}, text = {})", path, text);
        final int length = text.length();
        final List<String> lines = Files.readAllLines(path);
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            int rangeStart = line.indexOf(text);
            if (rangeStart < 0) {
                final IOException err = new IOException("File does not contain required text");
                log.error(entryMessage, err);
                throw err;
            }
            while (rangeStart >= 0) {
                markups.add(new Markup(i, rangeStart, length));
                rangeStart = line.indexOf(text, rangeStart + 1);
            }
        }
        this.path = path;
        log.traceExit(entryMessage);
    }
}
