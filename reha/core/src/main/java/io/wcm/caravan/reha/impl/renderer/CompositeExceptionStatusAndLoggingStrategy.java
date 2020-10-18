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
package io.wcm.caravan.reha.impl.renderer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.wcm.caravan.reha.api.spi.ExceptionStatusAndLoggingStrategy;

/**
 * Combines the behaviour of multiple {@link ExceptionStatusAndLoggingStrategy} instances
 */
public class CompositeExceptionStatusAndLoggingStrategy implements ExceptionStatusAndLoggingStrategy {

  private final List<ExceptionStatusAndLoggingStrategy> strategies;

  /**
   * @param strategies the strategies to combine
   */
  public CompositeExceptionStatusAndLoggingStrategy(List<ExceptionStatusAndLoggingStrategy> strategies) {
    this.strategies = strategies;
  }

  @Override
  public Integer extractStatusCode(Throwable error) {

    return strategies.stream()
        .map(s -> s.extractStatusCode(error))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @Override
  public boolean logAsCompactWarning(Throwable error) {

    return strategies.stream()
        .map(s -> s.logAsCompactWarning(error))
        .reduce(false, (logAsWarning1, logAsWarning2) -> logAsWarning1 || logAsWarning2);
  }

  @Override
  public String getErrorMessageWithoutRedundantInformation(Throwable error) {

    List<String> messageCandidates = strategies.stream()
        .map(s -> s.getErrorMessageWithoutRedundantInformation(error))
        .map(StringUtils::trimToEmpty)
        .collect(Collectors.toList());

    Collections.sort(messageCandidates, Comparator.comparing(String::length));

    return messageCandidates.get(0);
  }
}
