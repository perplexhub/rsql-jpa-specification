package io.github.perplexhub.rsql.jsonb;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * convenient way to define configuration, based on default values
 */
@Builder
@Accessors(fluent = true)
@Getter
public class JsonbExtractorSupport implements JsonbExtractor {

    @Builder.Default
    private final String pathExists = JsonbExtractor.DEFAULT.pathExists();
    @Builder.Default
    private final String pathExistsTz = JsonbExtractor.DEFAULT.pathExistsTz();
    @Builder.Default
    private final boolean useDateTime = JsonbExtractor.DEFAULT.useDateTime();

    @Override
    public String toString() {
        return String.format("pathExists:%s,pathExistsTz:%s,useDateTime:%b", pathExists, pathExistsTz, useDateTime);
    }
}
