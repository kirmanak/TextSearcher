package kirmanak.TextSearcher;

import lombok.extern.log4j.Log4j2;

import java.nio.file.Paths;

@Log4j2
public class Test {
    public static void main(String[] args) {
        log.traceEntry("Test.main()");
        final TextSearcher textSearcher = new TextSearcher(Paths.get("/"), "log", "error");
        textSearcher.getFiles().parallelStream().forEach((path) -> System.out.println(path.toString()));
        log.traceExit();
    }
}
