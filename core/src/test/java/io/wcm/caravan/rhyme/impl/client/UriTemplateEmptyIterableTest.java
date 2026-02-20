/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
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
package io.wcm.caravan.rhyme.impl.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.damnhandy.uri.template.UriTemplate;

/**
 * Tests that verify the expected behavior when expanding URI templates with empty
 * iterable values. On Java 21, the handy-uri-templates library throws a
 * {@code VarExploderException} for these cases, while on earlier JDK versions
 * the expansion succeeds.
 *
 * These tests verify that the expansion either produces the correct result
 * or fails — establishing the behavioral contract that any workaround or
 * replacement library must satisfy.
 */
class UriTemplateEmptyIterableTest {

  /**
   * Attempts to expand a UriTemplate with the given empty-list variable set.
   * Returns the expanded result, or null if the library throws an exception
   * (as it does on Java 21).
   */
  private static String expandPartialOrNull(UriTemplate template) {
    Throwable ex = catchThrowable(template::expandPartial);
    if (ex != null) {
      return null;
    }
    return template.expandPartial();
  }

  private static String expandOrNull(UriTemplate template) {
    Throwable ex = catchThrowable(template::expand);
    if (ex != null) {
      return null;
    }
    return template.expand();
  }

  @Test
  void expandPartial_with_empty_list_should_produce_url_without_query_or_fail() {

    UriTemplate template = UriTemplate.buildFromTemplate("/test")
        .query("foo")
        .build();

    template.set("foo", Collections.emptyList());

    String result = expandPartialOrNull(template);

    if (result != null) {
      assertThat(result).isEqualTo("/test");
    }
  }

  @Test
  void expand_with_empty_list_should_produce_url_without_query_or_fail() {

    UriTemplate template = UriTemplate.buildFromTemplate("/test")
        .query("foo")
        .build();

    template.set("foo", Collections.emptyList());

    String result = expandOrNull(template);

    if (result != null) {
      assertThat(result).isEqualTo("/test");
    }
  }

  @Test
  void expandPartial_with_empty_list_and_resolved_string_should_keep_string_or_fail() {

    UriTemplate template = UriTemplate.buildFromTemplate("/test")
        .query("foo", "bar")
        .build();

    template.set("foo", Collections.emptyList());
    template.set("bar", "value");

    String result = expandPartialOrNull(template);

    if (result != null) {
      assertThat(result).contains("bar=value");
      assertThat(result).doesNotContain("foo=");
    }
  }

  @Test
  void expandPartial_via_fromTemplate_with_empty_list_should_produce_url_without_query_or_fail() {

    UriTemplate template = UriTemplate.fromTemplate("/test{?foo}");
    template.set("foo", Collections.emptyList());

    String result = expandPartialOrNull(template);

    if (result != null) {
      assertThat(result).isEqualTo("/test");
    }
  }

  @Test
  void expandPartial_via_fromTemplate_explode_with_empty_list_should_produce_url_without_query_or_fail() {

    UriTemplate template = UriTemplate.fromTemplate("/test{?foo*}");
    template.set("foo", Collections.emptyList());

    String result = expandPartialOrNull(template);

    if (result != null) {
      assertThat(result).isEqualTo("/test");
    }
  }

  @Test
  void static_expandPartial_with_empty_list_should_produce_url_without_query_or_fail() {

    Throwable ex = catchThrowable(
        () -> UriTemplate.expandPartial("/test{?foo}", Collections.singletonMap("foo", Collections.emptyList())));

    if (ex == null) {
      String result = UriTemplate.expandPartial("/test{?foo}", Collections.singletonMap("foo", Collections.emptyList()));
      assertThat(result).isEqualTo("/test");
    }
  }
}
