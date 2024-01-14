package io.github.perplexhub.rsql;

import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PathUtils {
    /**
     * If the beginning of the property path is mapped, replace it with the mapped value.
     *
     * @param path the original property path
     * @param mapping the property path mapper
     * @return the mapped property path
     */
    public static Optional<String> findMappingOnBeginning(String path, Map<String, String> mapping) {
        if(mapping == null || mapping.isEmpty()) {
            return Optional.empty();
        }
        return mapping.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(entry -> entry.getValue() + path.substring(entry.getKey().length()))
                .findFirst();
    }

    /**
     * If the whole property path is mapped, replace it with the mapped value.
     *
     * @param path the original property path
     * @param mapping the property path mapper
     * @return the mapped property path
     */
    public static Optional<String> findMappingOnWhole(String path, Map<String, String> mapping) {
        if(mapping == null || mapping.isEmpty()) {
            return Optional.empty();
        }
        return mapping.entrySet().stream()
                .filter(entry -> Objects.equals(path, entry.getKey()))
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * Try to find the best mapping for the property path by checking the whole path first, then the beginning,
     * and finally the original path.
     *
     * @param path the original property path
     * @param mapping the property path mapper
     * @return the mapped property path
     */
    public static String expectBestMapping(String path, Map<String, String> mapping) {
        return findMappingOnWhole(path, mapping)
                .or(() -> findMappingOnBeginning(path, mapping))
                .orElse(path);
    }
}
