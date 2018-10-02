package kirmanak.TextSearcher;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

@Log4j2
@Getter
@RequiredArgsConstructor
// TODO: use something faster
class TextViewService extends Service<TextArea> {
    private final Path path;
    private final String text;

    @Override
    protected Task<TextArea> createTask() {
        return new TextViewTask();
    }

    private class TextViewTask extends Task<TextArea> {
        @Override
        protected TextArea call() throws Exception {
            final EntryMessage m = log.traceEntry("call() of {}", this);
            return log.traceExit(m, readText(initializeTextArea()));
        }

        /**
         * Creates and configures a TextArea instance
         *
         * @return an configured TextArea instance
         */
        private TextArea initializeTextArea() {
            final EntryMessage m = log.traceEntry("initializeTextArea() of {}", this);
            final TextArea textArea = new TextArea();
            textArea.setEditable(false);
            return log.traceExit(m, textArea);
        }

        /**
         * Reads text to the provided TextArea from the file
         *
         * @param textArea the initialized TextArea instance
         * @return the provided text area containing the file content
         * @throws IOException if any I/O error has happened
         */
        private TextArea readText(final TextArea textArea) throws IOException {
            // TODO: highlight the found text
            final EntryMessage m = log.traceEntry("readText(textArea = {}) of {}", textArea, this);
            final Iterator<String> lineIterator = Files.lines(getPath()).iterator();
            while (lineIterator.hasNext()) {
                final String line = lineIterator.next();
                textArea.appendText(line);
                textArea.appendText("\n");
            }
            return log.traceExit(m, textArea);
        }
    }
}
