package io.wcm.caravan.rhyme.impl.client.http;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Ordering;

import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;

/**
 * Extracts the relevant information from the HTTP headers given to
 * {@link HttpClientCallback#onHeadersAvailable(int, Map)}
 */
class HttpHeadersParser {

  private static final String MAX_AGE_REGEX = ".*max-age=([0-9]+).*";

  private static final Pattern MAX_AGE_PATTERN = Pattern.compile(MAX_AGE_REGEX);

  private final Map<String, ? extends Collection<String>> headers;

  HttpHeadersParser(Map<String, ? extends Collection<String>> headers) {
    this.headers = headers;
  }

  Optional<String> getContentType() {

    return findHeader("content-type");
  }

  Optional<Integer> getMaxAge() {

    return findHeaders("cache-control")
        .map(HttpHeadersParser::parseMaxAge)
        .filter(Objects::nonNull)
        .max(Ordering.natural());
  }

  private Optional<String> findHeader(String name) {

    return findHeaders(name)
        .findFirst();
  }

  Stream<String> findHeaders(String name) {
    return headers.entrySet().stream()
        .filter(entry -> StringUtils.equalsIgnoreCase(name, entry.getKey()))
        .flatMap(entry -> entry.getValue().stream());
  }

  private static Integer parseMaxAge(String cacheControl) {

    if (cacheControl.contains("immutable")) {
      return (int)Duration.ofDays(365).getSeconds();
    }

    Matcher matcher = MAX_AGE_PATTERN.matcher(cacheControl);
    if (!matcher.matches()) {
      return null;
    }

    long maxAge = Long.parseLong(matcher.group(1));
    if (maxAge > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int)maxAge;
  }

}
