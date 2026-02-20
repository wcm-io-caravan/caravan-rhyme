package io.wcm.caravan.rhyme.awslambda.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.hal.resource.Link;

class LambdaLinkBuilderImplTest {

    @Test
    void build_should_return_absolute_path_when_no_variables_added() {
        LambdaLinkBuilderImpl builder = new LambdaLinkBuilderImpl("/movies");
        Link link = builder.build();
        assertThat(link.getHref()).isEqualTo("/movies");
    }

    @Test
    void build_should_append_query_template_and_expand_partially() {
        LambdaLinkBuilderImpl builder = new LambdaLinkBuilderImpl("/movies");
        builder.addQueryVariable("searchTerm", "the");
        builder.addQueryVariable("page", null);

        Link link = builder.build();
        assertThat(link.getHref()).isEqualTo("/movies?searchTerm=the{&page}");
    }

    @Test
    void build_should_expand_fully_if_all_variables_are_not_null() {
        LambdaLinkBuilderImpl builder = new LambdaLinkBuilderImpl("/movies");
        builder.addQueryVariable("searchTerm", "the");
        builder.addQueryVariable("page", 1);

        Link link = builder.build();
        assertThat(link.getHref()).isEqualTo("/movies?searchTerm=the&page=1");
    }

    @Test
    void build_should_not_expand_if_all_variables_are_null() {
        LambdaLinkBuilderImpl builder = new LambdaLinkBuilderImpl("/movies");
        builder.addQueryVariable("searchTerm", null);
        builder.addQueryVariable("page", null);

        Link link = builder.build();
        assertThat(link.getHref()).isEqualTo("/movies{?searchTerm,page}");
    }
}
