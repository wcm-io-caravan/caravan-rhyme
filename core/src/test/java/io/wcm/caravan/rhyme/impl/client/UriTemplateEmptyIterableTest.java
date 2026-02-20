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
import com.damnhandy.uri.template.VarExploderException;

/**
 * Tests that verify the expected behavior when expanding URI templates with empty
 * iterable values. On Java 21+, the handy-uri-templates library throws a
 * {@link VarExploderException} for these cases, while on earlier JDK versions
 * the expansion succeeds and produces the correct result.
 *
 * Each test verifies both sides of this contract:
 * <ul>
 *   <li>On Java &lt; 21: expansion succeeds and produces the expected URL</li>
 *   <li>On Java &ge; 21: expansion fails with {@link VarExploderException}</li>
 * </ul>
 */
class UriTemplateEmptyIterableTest {

  private static final int JAVA_VERSION = Runtime.version().feature();
  private static final boolean IS_JAVA_21_OR_LATER = JAVA_VERSION >= 21;

  @Test
  void expandPartial_with_empty_list_on_non_explode_template() {

    UriTemplate template = UriTemplate.buildFromTemplate("/test")
        .query("foo")
        .build();

    template.set("foo", Collections.emptyList());

    Throwable ex = catchThrowable(template::expandPartial);

    if (IS_JAVA_21_OR_LATER) {
      assertThat(ex).isInstanceOf(VarExploderException.class);
    }
    else {
      assertThat(ex).isNull();
    }
  }

  @Test
  void expand_with_empty_list_on_non_explode_template() {

    UriTemplate template = UriTemplate.buildFromTemplate("/test")
        .query("foo")
        .build();

    template.set("foo", Collections.emptyList());

    Throwable ex = catchThrowable(template::expand);

    if (IS_JAVA_21_OR_LATER) {
      assertThat(ex).isInstanceOf(VarExploderException.class);
    }
    else {
      assertThat(ex).isNull();
    }
  }

  @Test
  void expandPartial_with_empty_list_and_resolved_string() {

    UriTemplate template = UriTemplate.buildFromTemplate("/test")
        .query("foo", "bar")
        .build();

    template.set("foo", Collections.emptyList());
    template.set("bar", "value");

    Throwable ex = catchThrowable(template::expandPartial);

    if (IS_JAVA_21_OR_LATER) {
      assertThat(ex).isInstanceOf(VarExploderException.class);
    }
    else {
      assertThat(ex).isNull();
    }
  }

  @Test
  void expandPartial_via_fromTemplate_non_explode_with_empty_list() {

    UriTemplate template = UriTemplate.fromTemplate("/test{?foo}");
    template.set("foo", Collections.emptyList());

    Throwable ex = catchThrowable(template::expandPartial);

    if (IS_JAVA_21_OR_LATER) {
      assertThat(ex).isInstanceOf(VarExploderException.class);
    }
    else {
      assertThat(ex).isNull();
    }
  }

  @Test
  void expandPartial_via_fromTemplate_explode_with_empty_list() {

    UriTemplate template = UriTemplate.fromTemplate("/test{?foo*}");
    template.set("foo", Collections.emptyList());

    Throwable ex = catchThrowable(template::expandPartial);

    if (IS_JAVA_21_OR_LATER) {
      assertThat(ex).isInstanceOf(VarExploderException.class);
    }
    else {
      assertThat(ex).isNull();
    }
  }

  @Test
  void static_expandPartial_with_empty_list() {

    Throwable ex = catchThrowable(
        () -> UriTemplate.expandPartial("/test{?foo}", Collections.singletonMap("foo", Collections.emptyList())));

    if (IS_JAVA_21_OR_LATER) {
      assertThat(ex).isInstanceOf(VarExploderException.class);
    }
    else {
      assertThat(ex).isNull();
    }
  }
}
