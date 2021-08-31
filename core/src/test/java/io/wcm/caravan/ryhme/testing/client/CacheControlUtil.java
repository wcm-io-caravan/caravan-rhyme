package io.wcm.caravan.ryhme.testing.client;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheControlUtil {

  private static final Logger log = LoggerFactory.getLogger(CacheControlUtil.class);

  private static final String REGEX = ".*max-age=([0-9]+).*";

  private static final Pattern PATTERN = Pattern.compile(REGEX);

  static Optional<Integer> parseMaxAge(String cacheControl) {

    if (StringUtils.isBlank(cacheControl)) {
      return Optional.empty();
    }

    Matcher matcher = PATTERN.matcher(cacheControl);
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
