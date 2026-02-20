package io.wcm.caravan.rhyme.awslambda.impl;

import static org.assertj.core.api.Assertions.assertThat;

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
}
