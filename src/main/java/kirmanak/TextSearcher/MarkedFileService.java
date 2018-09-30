package kirmanak.TextSearcher;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Path;

@Log4j2
@Getter
@RequiredArgsConstructor
public class MarkedFileService extends Service<TextArea> {
    private final Path path;
    private final String text;

    @Override
    protected Task<TextArea> createTask() {
        return new MarkedFileTask(getPath(), getText());
    }
}
