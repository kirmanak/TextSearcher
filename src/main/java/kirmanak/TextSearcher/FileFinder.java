package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.EntryMessage;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;

@Log4j2
@Getter
class FileFinder extends SimpleFileVisitor<Path> {
    private final PathMatcher matcher;
    private final LinkedList<Path> paths = new LinkedList<>();

    FileFinder(final String pattern) {
        final EntryMessage m = log.traceEntry("FileFinder(pattern = {})", pattern);
        matcher = FileSystems.getDefault().getPathMatcher(String.format("glob:*.%s", pattern));
        log.traceExit(m);
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        if (attrs.isRegularFile() && getMatcher().matches(file.getFileName())) {
            paths.add(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
        log.error(exc);
        return FileVisitResult.CONTINUE;
    }
}
