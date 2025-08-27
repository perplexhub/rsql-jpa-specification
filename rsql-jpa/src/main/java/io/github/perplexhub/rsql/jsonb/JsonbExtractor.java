package io.github.perplexhub.rsql.jsonb;

/**
 * jsonb expression configuration
 */
public interface JsonbExtractor {

    JsonbExtractor DEFAULT = new JsonbExtractor() {

        @Override
        public String pathExists() {
            return "jsonb_path_exists";
        }

        @Override
        public String pathExistsTz() {
            return "jsonb_path_exists_tz";
        }

        @Override
        public boolean useDateTime() {
            return false;
        }

        @Override
        public String toString() {
            return String.format("pathExists:%s,pathExistsTz:%s,useDateTime:%b", pathExists(), pathExistsTz(), useDateTime());
        }
    };

    /**
     *
     * @return Postgresql {@code jsonb_path_exists} function to use
     */
    String pathExists();

    /**
     *
     * @return Postgresql {@code jsonb_path_exists_tz} function to use
     */
    String pathExistsTz();

    /**
     *
     * @return enable temporal values support
     */
    boolean useDateTime();
}
