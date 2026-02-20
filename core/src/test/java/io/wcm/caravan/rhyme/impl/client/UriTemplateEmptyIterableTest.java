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
import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Test;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import com.damnhandy.uri.template.VarExploderException;

/**
 * Documents two Java 21 issues with handy-uri-templates:
 *
 * <h3>Issue 1: Empty iterable expansion</h3>
 * Expanding a query parameter template with an empty iterable value
 * throws a {@link VarExploderException} on Java 21.
 * This affects both explode and non-explode modifiers.
 *
 * <h3>Issue 2: Explode modifier in builder</h3>
 * Calling {@link UriTemplateBuilder#query(String...)} with a name containing
 * the explode modifier {@code *} (e.g. {@code "foo*"}) throws a
 * {@link PatternSyntaxException} on Java 21 because the {@code *} character
 * is invalid in Java regex named capturing groups.
 *
 * All modules using {@code UriTemplate.set()} with potentially empty iterables
 * must guard against this by skipping such variables before expansion.
 */
class UriTemplateEmptyIterableTest {

  // --- Issue 1: VarExploderException with empty iterables ---

  @Test
  void expandPartial_with_empty_list_on_non_explode_template_fails_on_java21() {

    UriTemplate template = UriTemplate.buildFromTemplate("/test")
        .query("foo")
        .build();

    template.set("foo", Collections.emptyList());

    Throwable ex = catchThrowable(template::expandPartial);

    assertThat(ex).isInstanceOf(VarExploderException.class);
  }

  @Test
  void expand_with_empty_list_on_non_explode_template_fails_on_java21() {

    UriTemplate template = UriTemplate.buildFromTemplate("/test")
        .query("foo")
        .build();

    template.set("foo", Collections.emptyList());

    Throwable ex = catchThrowable(template::expand);

    assertThat(ex).isInstanceOf(VarExploderException.class);
  }

  @Test
  void expandPartial_with_empty_list_and_resolved_string_fails_on_java21() {

    UriTemplate template = UriTemplate.buildFromTemplate("/test")
        .query("foo", "bar")
        .build();

    template.set("foo", Collections.emptyList());
    template.set("bar", "value");

    Throwable ex = catchThrowable(template::expandPartial);

    assertThat(ex).isInstanceOf(VarExploderException.class);
  }

  @Test
  void expandPartial_with_empty_list_via_fromTemplate_non_explode_fails_on_java21() {

    UriTemplate template = UriTemplate.fromTemplate("/test{?foo}");
    template.set("foo", Collections.emptyList());

    Throwable ex = catchThrowable(template::expandPartial);

    assertThat(ex).isInstanceOf(VarExploderException.class);
  }

  @Test
  void expandPartial_with_empty_list_via_fromTemplate_explode_fails_on_java21() {

    UriTemplate template = UriTemplate.fromTemplate("/test{?foo*}");
    template.set("foo", Collections.emptyList());

    Throwable ex = catchThrowable(template::expandPartial);

    assertThat(ex).isInstanceOf(VarExploderException.class);
  }

  // --- Issue 2: PatternSyntaxException with explode modifier in builder ---

  @Test
  void builder_query_with_explode_modifier_fails_on_java21() {

    Throwable ex = catchThrowable(() -> UriTemplate.buildFromTemplate("/test")
        .query("foo*")
        .build());

    assertThat(ex).isInstanceOf(PatternSyntaxException.class);
  }

  @Test
  void builder_query_with_explode_modifier_mixed_fails_on_java21() {

    Throwable ex = catchThrowable(() -> UriTemplate.buildFromTemplate("/test")
        .query("foo*", "bar")
        .build());

    assertThat(ex).isInstanceOf(PatternSyntaxException.class);
  }
}
