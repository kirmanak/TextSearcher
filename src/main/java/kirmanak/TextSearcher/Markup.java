package kirmanak.TextSearcher;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

/**
 * Represents a highlighted text: line number, start of highlighting and the length
 */
@Getter
@RequiredArgsConstructor
@ToString
@Log4j2
public class Markup {
    private final static Paint FILL = Color.RED;
    private final int lineNumber;
    private final int rangeStart;
    private final int rangeLength;

    public int getRangeEnd() {
        return getRangeStart() + getRangeLength();
    }

    public Text toText(final String line) {
        final EntryMessage entryMessage = log.traceEntry("toText(line = {}) of {}", line, this);
        final Text text = new Text(line.substring(getRangeStart(), getRangeEnd()));
        text.setFill(FILL);
        return log.traceExit(entryMessage, text);
    }
}
