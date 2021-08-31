package io.wcm.caravan.ryhme.testing.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class CacheControlUtil {

  private static final String REGEX = ".*max-age=([0-9]+).*";

  private static final Pattern PATTERN = Pattern.compile(REGEX);

  static Integer parseMaxAge(String cacheControl) {

    if (StringUtils.isBlank(cacheControl)) {
      return null;
    }

    Matcher matcher = PATTERN.matcher(cacheControl);
    if (!matcher.matches()) {
      return null;
    }

    return Integer.parseInt(matcher.group(1));
  }
}
