package io.wcm.caravan.rhyme.awslambda.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.hal.resource.Link;

class LambdaLinkBuilderImplTest {

  @Test
  void should_return_plain_link_when_no_variables_are_added() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .build();

    assertThat(link.getHref())
        .isEqualTo("/items");
  }

  @Test
  void should_return_fully_expanded_link_when_variable_is_non_null() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("page", 1)
        .build();

    assertThat(link.getHref())
        .isEqualTo("/items?page=1");
  }

  @Test
  void should_return_template_link_when_variable_is_null() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("page", null)
        .build();

    assertThat(link.getHref())
        .isEqualTo("/items{?page}");
  }

  @Test
  void should_partially_expand_when_some_variables_are_null() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("page", 1)
        .addQueryVariable("size", null)
        .build();

    assertThat(link.getHref())
        .isEqualTo("/items?page=1{&size}");
  }

  @Test
  void should_handle_multiple_non_null_variables() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("page", 1)
        .addQueryVariable("size", 10)
        .build();

    assertThat(link.getHref())
        .isEqualTo("/items?page=1&size=10");
  }

  @Test
  void should_handle_multiple_null_variables() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("page", null)
        .addQueryVariable("size", null)
        .build();

    assertThat(link.getHref())
        .isEqualTo("/items{?page,size}");
  }

  @Test
  void should_handle_integer_variable_values() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("offset", 42)
        .build();

    assertThat(link.getHref())
        .isEqualTo("/items?offset=42");
  }

  // --- Java 21 empty iterable issue ---

  @Test
  void should_not_fail_with_empty_list_as_only_variable() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("ids", Collections.emptyList())
        .build();

    assertThat(link.getHref()).isEqualTo("/items");
  }

  @Test
  void should_not_fail_with_empty_list_and_resolved_string_variable() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("ids", Collections.emptyList())
        .addQueryVariable("filter", "active")
        .build();

    assertThat(link.getHref()).contains("filter=active");
    assertThat(link.getHref()).doesNotContain("ids=");
  }

  @Test
  void should_not_fail_with_empty_list_and_null_variable() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("ids", Collections.emptyList())
        .addQueryVariable("page", null)
        .build();

    assertThat(link.getHref()).doesNotContain("ids=");
  }

  @Test
  void should_not_fail_with_empty_array_as_only_variable() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("ids", new String[0])
        .build();

    assertThat(link.getHref()).isEqualTo("/items");
  }

  @Test
  void should_handle_populated_array_variables_java21_workaround() {

    Link link = new LambdaLinkBuilderImpl("/items")
        .addQueryVariable("ids", new String[] { "1", "2" })
        .build();

    assertThat(link.getHref()).isEqualTo("/items?ids=1,2");
  }
}
