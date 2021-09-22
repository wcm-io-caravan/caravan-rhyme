package io.wcm.caravan.rhyme.spring.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class CacheControlUtil {

  private static final String REGEX = ".*max-age=([0-9]+).*";

  private static final Pattern pattern = Pattern.compile(REGEX);

  public static Integer parseMaxAge(String cacheControl) {

    if (StringUtils.isBlank(cacheControl)) {
      return null;
    }

    Matcher matcher = pattern.matcher(cacheControl);
    if (!matcher.matches()) {
      return null;
    }

    return Integer.parseInt(matcher.group(1));
  }
}
