package com.acme.testcontainers.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class ResourceReader {

    private static final String HEADER_MATCHER_REGEX = "<header>(.*?)</header>";
    private static final String PROPERTIES_MATCHER_REGEX = "<properties>(.*?)</properties>";
    private static final String JMSX_MATCHER_REGEX = "<extensions>(.*?)</extensions>";

    public static String readResourceToString(final String resourcePath) {
        var normalisedPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        return Optional.ofNullable(ResourceReader.class.getResourceAsStream(normalisedPath))
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(reader -> reader.lines().collect(Collectors.joining(System.lineSeparator())))
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource '" + normalisedPath + "' does not exist in classpath"));
    }

    public static Map<String, String> getHeaderProperties(final String jmsMetadata) {
        return getProperties(jmsMetadata, HEADER_MATCHER_REGEX);
    }

    public static Map<String, String> getCustomProperties(final String jmsMetadata) {
        return getProperties(jmsMetadata, PROPERTIES_MATCHER_REGEX);
    }

    public static Map<String, String> getJmsExtensionsProperties(final String jmsMetadata) {
        return getProperties(jmsMetadata, JMSX_MATCHER_REGEX);
    }

    private static Map<String, String> getProperties(final String jmsMetadata, final String regexStr) {
        final var result = new HashMap<String, String>(25);

        final var matcher = Pattern.compile(regexStr, Pattern.DOTALL).matcher(jmsMetadata);
        if (matcher.find()) {
            Arrays.stream(matcher.group(1).split("\n"))
                    .map(s -> {
                        if (!s.trim().isEmpty() && s.contains("=")) {
                            final var arr = s.split("=");
                            return new Pair(arr[0].trim(), arr.length == 2 && arr[1] != null ? arr[1].trim() : null);
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .forEach(pair -> {
                        log.trace("property pair created - adding {} = {}", pair.key(), pair.value());
                        result.put(pair.key(), pair.value());
                    });
        };

        return result;
    }

    record Pair (String key, String value) { }
}
