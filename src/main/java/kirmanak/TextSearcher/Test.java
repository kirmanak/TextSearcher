package kirmanak.TextSearcher;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.Paths;

@Log4j2
public class Test {
    public static void main(String[] args) {
        final EntryMessage entryMessage = log.traceEntry("main(args = {})", (Object[]) args);
        final TextSearcher textSearcher = new TextSearcher(Paths.get("/home/kirmanak/logs"), "log", "error");
        try {
            textSearcher.getFiles().forEach(System.out::println);
        } catch (final IOException e) {
            log.error(entryMessage, e);
        }
        log.traceExit(entryMessage);
    }
}
