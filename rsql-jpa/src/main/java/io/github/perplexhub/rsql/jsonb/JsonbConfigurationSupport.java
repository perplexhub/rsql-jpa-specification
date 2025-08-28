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
public final class JsonbConfigurationSupport implements JsonbConfiguration {

    @Builder.Default
    private final String pathExists = JsonbConfiguration.DEFAULT.pathExists();
    @Builder.Default
    private final String pathExistsTz = JsonbConfiguration.DEFAULT.pathExistsTz();
    @Builder.Default
    private final boolean useDateTime = JsonbConfiguration.DEFAULT.useDateTime();

    @Override
    public String toString() {
        return String.format("pathExists:%s,pathExistsTz:%s,useDateTime:%b", pathExists, pathExistsTz, useDateTime);
    }
}
