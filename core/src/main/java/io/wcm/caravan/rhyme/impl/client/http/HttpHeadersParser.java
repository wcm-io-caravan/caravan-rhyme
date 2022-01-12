package io.wcm.caravan.rhyme.impl.client.http;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;

/**
 * Extracts the relevant information from the HTTP headers given to
 * {@link HttpClientCallback#onHeadersAvailable(int, Map)}
 */
class HttpHeadersParser {

  private static final int ONE_YEAR_AS_SECONDS = (int)Duration.ofDays(365).getSeconds();

  private final Map<String, ? extends Collection<String>> headers;

  HttpHeadersParser(Map<String, ? extends Collection<String>> headers) {
    this.headers = ImmutableMap.copyOf(headers);
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

  static Integer parseMaxAge(String cacheControl) {

    if (cacheControl == null) {
      return null;
    }

    String lowerCase = cacheControl.toLowerCase();

    if (lowerCase.contains("immutable")) {
      return ONE_YEAR_AS_SECONDS;
    }

    if (lowerCase.contains("no-store")) {
      return 0;
    }

    return Stream.of(StringUtils.split(lowerCase, ","))
        .map(directive -> StringUtils.substringAfter(directive, "max-age="))
        .map(StringUtils::trimToNull)
        .filter(Objects::nonNull)
        .findFirst()
        .map(Long::parseLong)
        .map(longValue -> (int)Math.min(longValue, Integer.MAX_VALUE))
        .orElse(null);
  }
}
