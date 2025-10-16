package io.github.perplexhub.rsql.jsonb;

import lombok.Builder;

/**
 * convenient way to define configuration, based on default values
 *
 * @param pathExists   Postgresql {@code jsonb_path_exists} function to use
 * @param pathExistsTz Postgresql {@code jsonb_path_exists_tz} function to use
 * @param useDateTime  enable temporal values support
 */
@Builder
public record JsonbConfiguration(String pathExists, String pathExistsTz, boolean useDateTime) {

    public static final JsonbConfiguration DEFAULT = JsonbConfiguration.builder().build();

    public static class JsonbConfigurationBuilder {
        JsonbConfigurationBuilder() {
            pathExists = "jsonb_path_exists";
            pathExistsTz = "jsonb_path_exists_tz";
            useDateTime = false;
        }
    }

    @Override
    public String toString() {
        return String.format("pathExists:%s,pathExistsTz:%s,useDateTime:%b", pathExists, pathExistsTz, useDateTime);
    }
}
