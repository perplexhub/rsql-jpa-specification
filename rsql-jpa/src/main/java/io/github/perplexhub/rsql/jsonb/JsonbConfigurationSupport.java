package io.github.perplexhub.rsql.jsonb;

import lombok.Builder;

/**
 * convenient way to define configuration, based on default values
 */
@Builder
public record JsonbConfigurationSupport(String pathExists, String pathExistsTz, boolean useDateTime) implements JsonbConfiguration {

    public static class JsonbConfigurationSupportBuilder {
        JsonbConfigurationSupportBuilder() {
            pathExists = DEFAULT.pathExists();
            pathExistsTz = DEFAULT.pathExistsTz();
            useDateTime = DEFAULT.useDateTime();
        }
    }

    @Override
    public String toString() {
        return String.format("pathExists:%s,pathExistsTz:%s,useDateTime:%b", pathExists, pathExistsTz, useDateTime);
    }
}
