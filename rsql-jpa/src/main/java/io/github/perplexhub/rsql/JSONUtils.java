package io.github.perplexhub.rsql;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * Find some common types from a JSON string</br>
 * Date & Time follows ISO8601 format.</br>
 * As Numeric type can be Integer or Double, we return Double as it is the most common type.</br>
 */
@SuppressWarnings("rawtypes")
public class JSONUtils {
    private final static Map<String, Class> regexToJsonTypeMap = Map.ofEntries(
            //Date & Time
            Map.entry("^\\d{4}-\\d{2}-\\d{2}$", LocalDate.class),
            Map.entry("^\\d{2}:\\d{2}:\\d{2}$", LocalTime.class),
            Map.entry("^\\d{2}:\\d{2}:\\d{2}\\.\\d+$", LocalTime.class),
            Map.entry("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$", LocalDateTime.class),
            Map.entry("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+$", LocalDateTime.class),
            //Numeric
            Map.entry("^\\d+$", Double.class),
            Map.entry("^\\d+\\.\\d+$", Double.class)
    );

    /**
     * Find the most common type from a JSON string if it follows ISO8601 format or is a number
     *
     * @param value the JSON string
     * @return the most common type
     */
    protected static Class getJsonType(String value) {
        //Ensure that the value is not null
        if (value == null) {
            return String.class;
        }
        //Check all the args matching the same type
        return regexToJsonTypeMap.entrySet().stream()
                .filter(entry -> value.matches(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .filter(
                        firstFoundType -> regexToJsonTypeMap.entrySet().stream()
                                .anyMatch(entry -> entry.getValue().equals(firstFoundType) && value.matches(entry.getKey()))
                )
                .orElse(String.class);

    }
}
