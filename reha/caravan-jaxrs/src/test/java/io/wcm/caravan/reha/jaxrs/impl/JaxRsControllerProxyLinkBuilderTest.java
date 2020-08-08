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
package io.wcm.caravan.reha.jaxrs.impl;

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
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.client.HalApiDeveloperException;
import io.wcm.caravan.reha.jaxrs.api.JaxRsLinkBuilder;


public class JaxRsControllerProxyLinkBuilderTest {

  public static class ParamBean {

    @PathParam("foo")
    String foo;

    @QueryParam("bar")
    String bar;

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
      return Response.ok().build();
    }

    @GET
    @Path("/test")
    public Response withTwoStringQueryParams(@QueryParam("foo") String foo, @QueryParam("bar") String bar) {
      return Response.ok().build();
    }

    @GET
    @Path("/test")
    public Response withRequiredFooAndOptionalBarStringQueryParams(@QueryParam("foo") String foo, @QueryParam("bar") @DefaultValue("defaultBar") String bar) {
      return Response.ok().build();
    }

    @GET
    @Path("/test")
    public Response withListQueryParam(@QueryParam("foo") List<String> foo) {
      return Response.ok().build();
    }

    @GET
    @Path("/test")
    public Response withListAndStringQueryParam(@QueryParam("foo") List<String> foo, @QueryParam("bar") String bar) {
      return Response.ok().build();
    }

    @GET
    @Path("/test/{foo}")
    public Response withStringPathParam(@PathParam("foo") String foo) {
      return Response.ok().build();
    }

    @GET
    @Path("/test/{foo}/{bar}")
    public Response withTwoStringPathParams(@PathParam("foo") String foo, @PathParam("bar") String bar) {
      return Response.ok().build();
    }

    @GET
    @Path("/test/{foo}")
    public Response withBeanParam(@BeanParam ParamBean params) {
      return Response.ok().build();
    }

    public String withoutAnnotation() {
      return null;
    }
  }

  private Link buildLinkTo(Consumer<JaxRsComponent> methodCall) {
    JaxRsControllerProxyLinkBuilder<JaxRsComponent> linkBuilder = new JaxRsControllerProxyLinkBuilder<JaxRsComponent>("", JaxRsComponent.class);
    return linkBuilder.buildLinkTo(methodCall);
  }

  private AbstractCharSequenceAssert<?, String> assertLinkUrlFor(Consumer<JaxRsComponent> methodCall) {

    Link link = buildLinkTo(methodCall);

    return assertThat(link.getHref());
  }

  @Test
  public void path_from_annotation_should_be_used() throws Exception {

    assertLinkUrlFor(r -> r.foo())
        .isEqualTo("/foo");
  }

  @Test
  public void fail_if_no_path_annotation_is_present() throws Exception {

    Throwable ex = catchThrowable(() -> buildLinkTo(resource -> resource.withoutAnnotation()));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageContaining("must have a @Path annotation");
  }

  @Test
  public void one_resolved_required_query_param() throws Exception {

    assertLinkUrlFor(r -> r.withStringQueryParam("123"))
        .isEqualTo("/test?foo=123");
  }

  @Test
  public void one_unresolved_required_query_param() throws Exception {

    assertLinkUrlFor(r -> r.withStringQueryParam(null))
        .isEqualTo("/test{?foo}");
  }

  @Test
  public void two_required_query_params_both_resolved() throws Exception {

    assertLinkUrlFor(r -> r.withTwoStringQueryParams("123", "456"))
        .isEqualTo("/test?foo=123&bar=456");
  }

  @Test
  public void two_required_query_params_first_is_resolved() throws Exception {

    assertLinkUrlFor(r -> r.withTwoStringQueryParams("123", null))
        .isEqualTo("/test?foo=123{&bar}");
  }

  @Test
  public void two_required_query_params_second_is_resolved() throws Exception {

    assertLinkUrlFor(r -> r.withTwoStringQueryParams(null, "456"))
        .isEqualTo("/test?bar=456{&foo}");
  }

  @Test
  public void two_required_query_params_none_resolved() throws Exception {

    assertLinkUrlFor(r -> r.withTwoStringQueryParams(null, null))
        .isEqualTo("/test{?foo,bar}");
  }

  @Test
  public void template_does_not_expand_if_required_and_optional_param_is_unresolved() throws Exception {

    assertLinkUrlFor(r -> r.withRequiredFooAndOptionalBarStringQueryParams(null, null))
        .isEqualTo("/test{?foo,bar}");
  }

  @Test
  public void template_does_not_expand_if_only_optional_param_is_resolved() throws Exception {

    assertLinkUrlFor(r -> r.withRequiredFooAndOptionalBarStringQueryParams(null, "456"))
        .isEqualTo("/test?bar=456{&foo}");
  }

  @Test
  public void template_does_not_expand_if_only_additional_param_is_resolved() throws Exception {

    JaxRsLinkBuilder<JaxRsComponent> linkBuilder = new JaxRsControllerProxyLinkBuilder<JaxRsComponent>("", JaxRsComponent.class)
        .withAdditionalQueryParameters(ImmutableMap.of("finger", "print"));

    Link link = linkBuilder.buildLinkTo(r -> r.withRequiredFooAndOptionalBarStringQueryParams(null, null));

    assertThat(link.getHref())
        .isEqualTo("/test?finger=print{&foo,bar}");
  }

  @Test
  public void template_expands_if_only_optional_param_is_unresolved() throws Exception {

    assertLinkUrlFor(r -> r.withRequiredFooAndOptionalBarStringQueryParams("123", null))
        .isEqualTo("/test?foo=123");
  }

  @Test
  public void one_resolved_path_param() throws Exception {

    assertLinkUrlFor(r -> r.withStringPathParam("123"))
        .isEqualTo("/test/123");
  }

  @Test
  public void one_unresolved_path_param() throws Exception {

    assertLinkUrlFor(r -> r.withStringPathParam(null))
        .isEqualTo("/test/{foo}");
  }

  @Test
  public void two_path_params_both_resolved() throws Exception {

    assertLinkUrlFor(r -> r.withTwoStringPathParams("123", "456"))
        .isEqualTo("/test/123/456");
  }

  @Test
  public void two_path_params_first_is_resolved() throws Exception {

    assertLinkUrlFor(r -> r.withTwoStringPathParams("123", null))
        .isEqualTo("/test/123/{bar}");
  }

  @Test
  public void two_path_params_second_is_resolved() throws Exception {

    assertLinkUrlFor(r -> r.withTwoStringPathParams(null, "456"))
        .isEqualTo("/test/{foo}/456");
  }

  @Test
  public void two_path_params_none_resolved() throws Exception {

    assertLinkUrlFor(r -> r.withTwoStringPathParams(null, null))
        .isEqualTo("/test/{foo}/{bar}");
  }

  @Test
  public void one_null_list_query_param() throws Exception {

    assertLinkUrlFor(r -> r.withListQueryParam(null))
        .isEqualTo("/test{?foo*}");
  }

  @Test
  public void one_empty_list_query_param() throws Exception {

    assertLinkUrlFor(r -> r.withListQueryParam(Collections.emptyList()))
        .isEqualTo("/test");
  }

  @Test
  public void one_populated_list_query_param() throws Exception {

    assertLinkUrlFor(r -> r.withListQueryParam(ImmutableList.of("123", "456")))
        .isEqualTo("/test?foo=123&foo=456");
  }

  @Test
  public void one_null_list_query_param_before_string_param() throws Exception {

    assertLinkUrlFor(r -> r.withListAndStringQueryParam(null, "123"))
        .isEqualTo("/test?bar=123{&foo*}");
  }

  @Test
  public void one_empty_list_query_param_before_string_param() throws Exception {

    assertLinkUrlFor(r -> r.withListAndStringQueryParam(Collections.emptyList(), "123"))
        .isEqualTo("/test?bar=123");
  }

  @Test
  public void one_populated_list_query_param_before_string_param() throws Exception {

    assertLinkUrlFor(r -> r.withListAndStringQueryParam(ImmutableList.of("123", "456"), "789"))
        .isEqualTo("/test?foo=123&foo=456&bar=789");
  }

  @Test
  public void one_unresolved_bean_param() throws Exception {

    assertLinkUrlFor(r -> r.withBeanParam(null))
        .isEqualTo("/test/{foo}{?bar}");
  }

  @Test
  public void one_bean_param_with_null_values() throws Exception {

    ParamBean params = new ParamBean();

    assertLinkUrlFor(r -> r.withBeanParam(params))
        .isEqualTo("/test/{foo}{?bar}");
  }

  @Test
  public void one_bean_param_with_path_and_query_field_set() throws Exception {

    ParamBean params = new ParamBean();
    params.foo = "123";
    params.bar = "456";

    assertLinkUrlFor(r -> r.withBeanParam(params))
        .isEqualTo("/test/123?bar=456");
  }
}
