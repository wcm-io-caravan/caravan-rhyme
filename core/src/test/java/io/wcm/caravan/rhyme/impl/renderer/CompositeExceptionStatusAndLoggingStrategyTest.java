/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.impl.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;


public class CompositeExceptionStatusAndLoggingStrategyTest {

  private static final ExceptionStatusAndLoggingStrategy EXTRACT_NOTHING = new ExtractNothing();
  private static final ExceptionStatusAndLoggingStrategy EXTRACT_500 = new Extract500();
  private static final ExceptionStatusAndLoggingStrategy EXTRACT_404 = new Extract404();
  private static final ReturnNull RETURN_NULL = new ReturnNull();

  private static final ExceptionStatusAndLoggingStrategy LOG_NOTHING_AS_WARNING = new LogNothingAsWarning();
  private static final ExceptionStatusAndLoggingStrategy LOG_EVERYTHING_AS_WARNING = new LogEverythingAsWarning();

  private static final ExceptionStatusAndLoggingStrategy REMOVE_NOTHING = new RemoveNothing();
  private static final ExceptionStatusAndLoggingStrategy REMOVE_VALUE = new RemoveValue();
  private static final ExceptionStatusAndLoggingStrategy REMOVE_FOO = new RemoveFoo();

  private ExceptionStatusAndLoggingStrategy createComposite(ExceptionStatusAndLoggingStrategy... strategies) {

    return new CompositeExceptionStatusAndLoggingStrategy(ImmutableList.copyOf(strategies));
  }

  private Integer extractStatusCode(ExceptionStatusAndLoggingStrategy... strategies) {

    return createComposite(strategies).extractStatusCode(new RuntimeException());
  }

  @Test
  public void extractStatusCode_first_matches() throws Exception {

    assertThat(extractStatusCode(EXTRACT_404, EXTRACT_NOTHING)).isEqualTo(404);
  }

  @Test
  public void extractStatusCode_second_matches() throws Exception {

    assertThat(extractStatusCode(EXTRACT_NOTHING, EXTRACT_404)).isEqualTo(404);
  }

  @Test
  public void extractStatusCode_both_match() throws Exception {

    assertThat(extractStatusCode(EXTRACT_500, EXTRACT_404)).isEqualTo(500);
  }

  @Test
  public void extractStatusCode_nothing_matches() throws Exception {

    assertThat(extractStatusCode(EXTRACT_NOTHING, EXTRACT_NOTHING)).isNull();
  }

  private boolean logAsWarning(ExceptionStatusAndLoggingStrategy... strategies) {

    return createComposite(strategies).logAsCompactWarning(new RuntimeException());
  }

  @Test
  public void logAsCompactWarning_first_true() throws Exception {

    assertThat(logAsWarning(LOG_EVERYTHING_AS_WARNING, LOG_NOTHING_AS_WARNING)).isTrue();
  }

  @Test
  public void logAsCompactWarning_second_true() throws Exception {

    assertThat(logAsWarning(LOG_NOTHING_AS_WARNING, LOG_EVERYTHING_AS_WARNING)).isTrue();
  }

  @Test
  public void logAsCompactWarning_both_false() throws Exception {

    assertThat(logAsWarning(LOG_NOTHING_AS_WARNING, LOG_NOTHING_AS_WARNING)).isFalse();
  }

  @Test
  public void logAsCompactWarning_both_true() throws Exception {

    assertThat(logAsWarning(LOG_EVERYTHING_AS_WARNING, LOG_EVERYTHING_AS_WARNING)).isTrue();
  }

  private String removeInfo(ExceptionStatusAndLoggingStrategy... strategies) {

    return createComposite(strategies).getErrorMessageWithoutRedundantInformation(new RuntimeException("foo=123"));
  }

  @Test
  public void getErrorMessageWithoutRedundantInformation_only_first_matches() throws Exception {

    assertThat(removeInfo(REMOVE_FOO, REMOVE_NOTHING)).isEqualTo("=123");
  }

  @Test
  public void getErrorMessageWithoutRedundantInformation_only_second_matches() throws Exception {

    assertThat(removeInfo(REMOVE_NOTHING, REMOVE_FOO)).isEqualTo("=123");
  }

  @Test
  public void getErrorMessageWithoutRedundantInformation_first_removes_more() throws Exception {

    assertThat(removeInfo(REMOVE_VALUE, REMOVE_FOO)).isEqualTo("foo");
  }

  @Test
  public void getErrorMessageWithoutRedundantInformation_second_removes_more() throws Exception {

    assertThat(removeInfo(REMOVE_FOO, REMOVE_VALUE)).isEqualTo("foo");
  }

  @Test
  public void getErrorMessageWithoutRedundantInformation_handles_null() throws Exception {

    assertThat(removeInfo(RETURN_NULL, RETURN_NULL)).isEmpty();
  }

  private static class LogEverythingAsWarning implements ExceptionStatusAndLoggingStrategy {

    @Override
    public boolean logAsCompactWarning(Throwable error) {
      return true;
    }
  }

  private static class LogNothingAsWarning implements ExceptionStatusAndLoggingStrategy {

    @Override
    public boolean logAsCompactWarning(Throwable error) {
      return false;
    }
  }

  private static class ExtractNothing implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {
      return null;
    }
  }

  private static class Extract404 implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {
      return 404;
    }
  }

  private static class Extract500 implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {
      return 500;
    }
  }

  private static class RemoveNothing implements ExceptionStatusAndLoggingStrategy {

    @Override
    public String getErrorMessageWithoutRedundantInformation(Throwable error) {
      return error.getMessage();
    }
  }

  private static class RemoveFoo implements ExceptionStatusAndLoggingStrategy {

    @Override
    public String getErrorMessageWithoutRedundantInformation(Throwable error) {
      return StringUtils.remove(error.getMessage(), "foo");
    }
  }

  private static class RemoveValue implements ExceptionStatusAndLoggingStrategy {

    @Override
    public String getErrorMessageWithoutRedundantInformation(Throwable error) {
      return StringUtils.substringBefore(error.getMessage(), "=");
    }
  }

  private static class ReturnNull implements ExceptionStatusAndLoggingStrategy {

    @Override
    public String getErrorMessageWithoutRedundantInformation(Throwable error) {
      return null;
    }
  }

}
