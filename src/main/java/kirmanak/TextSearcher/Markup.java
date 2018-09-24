package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Represents a highlighted text: line number, start of highlighting and the length
 */
@Getter
@RequiredArgsConstructor
@ToString
public class Markup {
    private final int lineNumber;
    private final int rangeStart;
    private final int rangeLength;
}
