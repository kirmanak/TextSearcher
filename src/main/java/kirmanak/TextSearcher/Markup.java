package kirmanak.TextSearcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString
public class Markup {
    private final int lineNumber;
    private final int rangeStart;
    private final int rangeLength;
}
