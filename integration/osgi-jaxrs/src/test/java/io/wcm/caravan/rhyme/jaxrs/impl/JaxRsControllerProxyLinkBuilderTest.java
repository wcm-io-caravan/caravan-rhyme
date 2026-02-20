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
package io.wcm.caravan.rhyme.jaxrs.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsLinkBuilder;

@SuppressFBWarnings("URF_UNREAD_FIELD")
class JaxRsControllerProxyLinkBuilderTest {

  public static class ParamBean {

    @PathParam("foo")
    String foo;

    @QueryParam("bar")
    String bar;

    @QueryParam("withDefault")
    @DefaultValue("defaultValue")
    String withDefault;

  }

  public static class JaxRsComponent {

    @GET
    @Path("/foo")
    public Response foo() {
      return Response.ok().build();
    }

    @GET
    @Path("/test")
    public Response withStringQueryParam(@QueryParam("foo") String foo) {
      return Response.ok(foo).build();
    }

    @GET
    @Path("/test")
    public Response withTwoStringQueryParams(@QueryParam("foo") String foo, @QueryParam("bar") String bar) {
      return Response.ok(foo + bar).build();
    }

    @GET
    @Path("/test")
    public Response withRequiredFooAndOptionalBarStringQueryParams(@QueryParam("foo") String foo, @QueryParam("bar") @DefaultValue("defaultBar") String bar) {
      return Response.ok(foo + bar).build();
    }

    @GET
    @Path("/test")
    public Response withListQueryParam(@QueryParam("foo") List<String> foo) {
      return Response.ok(foo).build();
    }

    @GET
    @Path("/test")
    public Response withListAndStringQueryParam(@QueryParam("foo") List<String> foo, @QueryParam("bar") String bar) {
      return Response.ok(foo + bar).build();
    }

    @GET
    @Path("/test")
    public Response withTwoListQueryParams(@QueryParam("foo") List<String> foo, @QueryParam("bar") List<String> bar) {
      return Response.ok(foo + "" + bar).build();
    }

    @GET
    @Path("/test/{foo}")
    public Response withStringPathParam(@PathParam("foo") String foo) {
      return Response.ok(foo).build();
    }

    @GET
    @Path("/test/{foo}/{bar}")
    public Response withTwoStringPathParams(@PathParam("foo") String foo, @PathParam("bar") String bar) {
      return Response.ok(foo + bar).build();
    }

    @GET
    @Path("/test/{foo}")
    public Response withBeanParam(@BeanParam ParamBean params) {
      return Response.ok(params).build();
    }

    public String withoutAnnotation() {
      return null;
    }
  }

  private Link buildLinkTo(Consumer<JaxRsComponent> methodCall) {
    JaxRsLinkBuilder<JaxRsComponent> linkBuilder = JaxRsLinkBuilder.create("", JaxRsComponent.class);
    return linkBuilder.buildLinkTo(methodCall);
  }

  private AbstractCharSequenceAssert<?, String> assertLinkUrlFor(Consumer<JaxRsComponent> methodCall) {

    Link link = buildLinkTo(methodCall);

    return assertThat(link.getHref());
  }

  @Test
  void path_from_annotation_should_be_used() {

    assertLinkUrlFor(r -> r.foo())
        .isEqualTo("/foo");
  }

  @Test
  void fail_if_no_path_annotation_is_present() {

    Throwable ex = catchThrowable(() -> buildLinkTo(resource -> resource.withoutAnnotation()));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to build link");

    assertThat(ex.getCause())
        .hasMessageContaining("must have a @Path annotation");
  }

  @Test
  void one_resolved_required_query_param() {

    assertLinkUrlFor(r -> r.withStringQueryParam("123"))
        .isEqualTo("/test?foo=123");
  }

  @Test
  void one_unresolved_required_query_param() {

    assertLinkUrlFor(r -> r.withStringQueryParam(null))
        .isEqualTo("/test{?foo}");
  }

  @Test
  void two_required_query_params_both_resolved() {

    assertLinkUrlFor(r -> r.withTwoStringQueryParams("123", "456"))
        .isEqualTo("/test?foo=123&bar=456");
  }

  @Test
  void two_required_query_params_first_is_resolved() {

    assertLinkUrlFor(r -> r.withTwoStringQueryParams("123", null))
        .isEqualTo("/test?foo=123{&bar}");
  }

  @Test
  void two_required_query_params_second_is_resolved() {

    assertLinkUrlFor(r -> r.withTwoStringQueryParams(null, "456"))
        .isEqualTo("/test?bar=456{&foo}");
  }

  @Test
  void two_required_query_params_none_resolved() {

    assertLinkUrlFor(r -> r.withTwoStringQueryParams(null, null))
        .isEqualTo("/test{?foo,bar}");
  }

  @Test
  void template_does_not_expand_if_required_and_optional_param_is_unresolved() {

    assertLinkUrlFor(r -> r.withRequiredFooAndOptionalBarStringQueryParams(null, null))
        .isEqualTo("/test{?foo,bar}");
  }

  @Test
  void template_does_not_expand_if_only_optional_param_is_resolved() {

    assertLinkUrlFor(r -> r.withRequiredFooAndOptionalBarStringQueryParams(null, "456"))
        .isEqualTo("/test?bar=456{&foo}");
  }

  @Test
  void template_does_not_expand_if_only_additional_param_is_resolved() {

    JaxRsLinkBuilder<JaxRsComponent> linkBuilder = new JaxRsControllerProxyLinkBuilder<JaxRsComponent>("", JaxRsComponent.class)
        .withAdditionalQueryParameters(ImmutableMap.of("finger", "print"));

    Link link = linkBuilder.buildLinkTo(r -> r.withRequiredFooAndOptionalBarStringQueryParams(null, null));

    assertThat(link.getHref())
        .isEqualTo("/test?finger=print{&foo,bar}");
  }

  @Test
  void template_expands_if_only_optional_param_is_unresolved() {

    assertLinkUrlFor(r -> r.withRequiredFooAndOptionalBarStringQueryParams("123", null))
        .isEqualTo("/test?foo=123");
  }

  @Test
  void one_resolved_path_param() {

    assertLinkUrlFor(r -> r.withStringPathParam("123"))
        .isEqualTo("/test/123");
  }

  @Test
  void one_unresolved_path_param() {

    assertLinkUrlFor(r -> r.withStringPathParam(null))
        .isEqualTo("/test/{foo}");
  }

  @Test
  void two_path_params_both_resolved() {

    assertLinkUrlFor(r -> r.withTwoStringPathParams("123", "456"))
        .isEqualTo("/test/123/456");
  }

  @Test
  void two_path_params_first_is_resolved() {

    assertLinkUrlFor(r -> r.withTwoStringPathParams("123", null))
        .isEqualTo("/test/123/{bar}");
  }

  @Test
  void two_path_params_second_is_resolved() {

    assertLinkUrlFor(r -> r.withTwoStringPathParams(null, "456"))
        .isEqualTo("/test/{foo}/456");
  }

  @Test
  void two_path_params_none_resolved() {

    assertLinkUrlFor(r -> r.withTwoStringPathParams(null, null))
        .isEqualTo("/test/{foo}/{bar}");
  }

  @Test
  void one_null_list_query_param() {

    assertLinkUrlFor(r -> r.withListQueryParam(null))
        .isEqualTo("/test{?foo*}");
  }

  @Test
  void one_empty_list_query_param() {

    assertLinkUrlFor(r -> r.withListQueryParam(Collections.emptyList()))
        .isEqualTo("/test");
  }

  @Test
  void one_populated_list_query_param() {

    assertLinkUrlFor(r -> r.withListQueryParam(ImmutableList.of("123", "456")))
        .isEqualTo("/test?foo=123&foo=456");
  }

  @Test
  void one_null_list_query_param_before_string_param() {

    assertLinkUrlFor(r -> r.withListAndStringQueryParam(null, "123"))
        .isEqualTo("/test?bar=123{&foo*}");
  }

  @Test
  void one_empty_list_query_param_before_string_param() {

    assertLinkUrlFor(r -> r.withListAndStringQueryParam(Collections.emptyList(), "123"))
        .isEqualTo("/test?bar=123");
  }

  @Test
  void one_populated_list_query_param_before_string_param() {

    assertLinkUrlFor(r -> r.withListAndStringQueryParam(ImmutableList.of("123", "456"), "789"))
        .isEqualTo("/test?foo=123&foo=456&bar=789");
  }

  @Test
  void null_bean_param_should_result_in_full_template_to_be_written() {

    assertLinkUrlFor(r -> r.withBeanParam(null))
        .isEqualTo("/test/{foo}{?bar,withDefault}");
  }

  @Test
  void one_bean_param_should_result_in_full_template_to_be_written() {

    ParamBean params = new ParamBean();

    assertLinkUrlFor(r -> r.withBeanParam(params))
        .isEqualTo("/test/{foo}{?bar,withDefault}");
  }

  @Test
  void one_bean_param_with_path_and_query_field_set() {

    ParamBean params = new ParamBean();
    params.foo = "123";
    params.bar = "456";

    assertLinkUrlFor(r -> r.withBeanParam(params))
        .isEqualTo("/test/123?bar=456");
  }

  @Test
  void one_bean_param_with_replacement_for_default_value() {

    ParamBean params = new ParamBean();
    params.foo = "123";
    params.bar = "456";
    params.withDefault = "replacement";

    assertLinkUrlFor(r -> r.withBeanParam(params))
        .isEqualTo("/test/123?bar=456&withDefault=replacement");
  }

  // --- Java 21 workaround: empty iterable query params are skipped from the template ---

  @Test
  void empty_list_query_param_with_unresolved_string_param() {

    assertLinkUrlFor(r -> r.withListAndStringQueryParam(Collections.emptyList(), null))
        .isEqualTo("/test{?bar}");
  }

  @Test
  void empty_list_query_param_with_unresolved_string_param_and_null_list() {

    assertLinkUrlFor(r -> r.withTwoListQueryParams(Collections.emptyList(), null))
        .isEqualTo("/test{?bar*}");
  }

  @Test
  void two_empty_list_query_params() {

    assertLinkUrlFor(r -> r.withTwoListQueryParams(Collections.emptyList(), Collections.emptyList()))
        .isEqualTo("/test");
  }

  @Test
  void one_populated_and_one_empty_list_query_param() {

    assertLinkUrlFor(r -> r.withTwoListQueryParams(ImmutableList.of("a", "b"), Collections.emptyList()))
        .isEqualTo("/test?foo=a&foo=b");
  }

  @Test
  void one_empty_and_one_populated_list_query_param() {

    assertLinkUrlFor(r -> r.withTwoListQueryParams(Collections.emptyList(), ImmutableList.of("x")))
        .isEqualTo("/test?bar=x");
  }

  // --- base URL prefix ---

  @Test
  void base_url_is_prepended_to_path() {

    JaxRsLinkBuilder<JaxRsComponent> linkBuilder = JaxRsLinkBuilder.create("/api", JaxRsComponent.class);

    Link link = linkBuilder.buildLinkTo(r -> r.foo());

    assertThat(link.getHref()).isEqualTo("/api/foo");
  }

  @Test
  void base_url_is_prepended_with_query_params() {

    JaxRsLinkBuilder<JaxRsComponent> linkBuilder = JaxRsLinkBuilder.create("/api", JaxRsComponent.class);

    Link link = linkBuilder.buildLinkTo(r -> r.withStringQueryParam("val"));

    assertThat(link.getHref()).isEqualTo("/api/test?foo=val");
  }

  @Test
  void base_url_is_prepended_with_path_and_query_template() {

    JaxRsLinkBuilder<JaxRsComponent> linkBuilder = JaxRsLinkBuilder.create("/api", JaxRsComponent.class);

    Link link = linkBuilder.buildLinkTo(r -> r.withTwoStringQueryParams(null, null));

    assertThat(link.getHref()).isEqualTo("/api/test{?foo,bar}");
  }

  // --- special characters in query param values ---

  @Test
  void special_characters_in_query_param_are_encoded() {

    assertLinkUrlFor(r -> r.withStringQueryParam("hello world"))
        .contains("foo=hello%20world");
  }

  @Test
  void special_characters_in_path_param_are_encoded() {

    assertLinkUrlFor(r -> r.withStringPathParam("a/b"))
        .isEqualTo("/test/a%2Fb");
  }

  // --- mixed path and query with partial expansion ---

  @Test
  void path_param_resolved_with_unresolved_query_param() {

    ParamBean params = new ParamBean();
    params.foo = "123";

    assertLinkUrlFor(r -> r.withBeanParam(params))
        .isEqualTo("/test/123{?bar,withDefault}");
  }

  @Test
  void path_param_unresolved_with_resolved_query_param() {

    ParamBean params = new ParamBean();
    params.bar = "456";

    assertLinkUrlFor(r -> r.withBeanParam(params))
        .isEqualTo("/test/{foo}?bar=456{&withDefault}");
  }

  public static final class FinalJaxRsComponent {
    // no code required as this fails on proxy instantiation
  }

  @Test
  void should_fail_with_HalApiDeveloperException_if_consumer_throws_exception() {

    RuntimeException cause = new RuntimeException("failed");

    Throwable t = catchThrowable(() -> buildLinkTo((res) -> {
      throw cause;
    }));

    assertThat(t).isInstanceOf(HalApiDeveloperException.class)
        .hasCauseReference(cause);
  }

  @Test
  void should_fail_with_HalApiDeveloperException_if_controller_class_is_final() {

    Throwable t = catchThrowable(() -> JaxRsLinkBuilder.create("", FinalJaxRsComponent.class));

    assertThat(t).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to create proxy subclass");
  }

  static class NonPublicJaxRsComponent {
    // no code required as this fails on proxy instantiation
  }

  @Test
  void should_fail_with_HalApiDeveloperException_if_controller_class_is_not_public() {

    Throwable t = catchThrowable(() -> JaxRsLinkBuilder.create("", NonPublicJaxRsComponent.class));

    assertThat(t).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to create proxy subclass");
  }
}
