package io.wcm.caravan.rhyme.impl.client.http;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpHeadersParser {

  private static final Logger log = LoggerFactory.getLogger(HttpHeadersParser.class);

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

    return findHeader("cache-control")
        .flatMap(HttpHeadersParser::parseMaxAge);
  }

  private Optional<String> findHeader(String name) {

    return headers.entrySet().stream()
        .filter(entry -> StringUtils.equalsIgnoreCase(name, entry.getKey()))
        .flatMap(entry -> entry.getValue().stream())
        .findFirst();
  }

  private static Optional<Integer> parseMaxAge(String cacheControl) {

    if (StringUtils.isBlank(cacheControl)) {
      return Optional.empty();
    }

    Matcher matcher = MAX_AGE_PATTERN.matcher(cacheControl);
    if (!matcher.matches()) {
      return Optional.empty();
    }

    try {
      return Optional.of(Integer.parseInt(matcher.group(1)));
    }
    catch (RuntimeException ex) {
      log.warn("failed to parse max-age from cache-control header with value {} ", cacheControl, ex);
      return Optional.empty();
    }
  }

}
