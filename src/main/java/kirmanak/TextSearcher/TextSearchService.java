package kirmanak.TextSearcher;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.nio.file.Path;
import java.util.List;

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
        final EntryMessage entryMessage = log.traceEntry("TextSearchService(rootFolder = {}, extension = {}, text = {})", rootFolder, extension, text);
        if (!rootFolder.toFile().isDirectory() || !rootFolder.toFile().canExecute()) {
            final IllegalArgumentException err = new IllegalArgumentException("Root folder must be a executable directory.");
            log.error(entryMessage, err);
            throw err;
        }
        if (extension.isEmpty() || extension.contains(".")) {
            final IllegalArgumentException err = new IllegalArgumentException("Extension is incorrect.");
            log.error(entryMessage, err);
            throw err;
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
}
