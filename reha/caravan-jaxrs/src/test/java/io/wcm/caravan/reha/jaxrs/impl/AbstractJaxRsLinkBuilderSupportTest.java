/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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

import java.util.Map;

import javax.ws.rs.Path;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.resources.LinkableResource;


public abstract class AbstractJaxRsLinkBuilderSupportTest {

  protected static final String CONTEXT_PATH = "/context";

  protected static final String RESOURCE_PATH = "/test";

  protected static final String ABSOLUTE_RESOURCE_PATH = CONTEXT_PATH + RESOURCE_PATH;

  protected static final String QUERY_PARAM_A = "queryA";

  protected static final String QUERY_PARAM_B = "queryB";

  protected static final String QUERY_PARAM_C = "queryC";

  protected static final String PATH_PARAM_A = "pathA";

  protected static final String PATH_PARAM_B = "pathB";

  protected static final String RESOURCE_PATH_TEMPLATE = RESOURCE_PATH + "/{" + PATH_PARAM_A + "}/{" + PATH_PARAM_B + "}";

  protected static final String ABSOLUTE_RESOURCE_PATH_TEMPLATE = CONTEXT_PATH + RESOURCE_PATH_TEMPLATE;

  protected JaxRsLinkBuilderSupport support = new JaxRsLinkBuilderSupport();

  static class LinkableResourceAdapter implements LinkableResource {

    @Override
    public Link createLink() {
      throw new NotImplementedException("#createLink should never be called by the LinkBuilder");
    }

  }

  @Path(RESOURCE_PATH)
  static class TestResourceWithoutParameters extends LinkableResourceAdapter {
    // a class without any parameter properties
  }

  abstract LinkableResource createResourceWithTwoQueryParameters(String valueOfA, String valueOfB);

  abstract LinkableResource createResourceWithTwoPathParameters(String valueOfA, String valueOfB);

  @Test
  public void should_get_resource_path_from_annotation() {

    TestResourceWithoutParameters resource = new TestResourceWithoutParameters();

    String path = support.getResourcePathTemplate(resource);

    assertThat(path).isEqualTo(RESOURCE_PATH);
  }


  @Test
  public void should_get_query_parameters_with_non_null_value() throws Exception {

    String valueOfA = "testA";
    String valueOfB = "t:/est?B";

    LinkableResource resource = createResourceWithTwoQueryParameters(valueOfA, valueOfB);

    Map<String, Object> parameters = support.getQueryParameters(resource);

    assertThat(parameters).containsEntry(QUERY_PARAM_A, valueOfA);
    assertThat(parameters).containsEntry(QUERY_PARAM_B, valueOfB);
  }

  @Test
  public void should_get_query_parameters_with_null_value() throws Exception {

    String valueOfA = null;
    String valueOfB = null;

    LinkableResource resource = createResourceWithTwoQueryParameters(valueOfA, valueOfB);

    Map<String, Object> parameters = support.getQueryParameters(resource);

    assertThat(parameters).containsEntry(QUERY_PARAM_A, valueOfA);
    assertThat(parameters).containsEntry(QUERY_PARAM_B, valueOfB);
  }

  @Test
  public void should_get_path_parameters_with_non_null_value() throws Exception {

    String valueOfA = "testA";
    String valueOfB = "testB";

    LinkableResource resource = createResourceWithTwoPathParameters(valueOfA, valueOfB);

    Map<String, Object> parameters = support.getPathParameters(resource);

    assertThat(parameters).containsEntry(PATH_PARAM_A, valueOfA);
    assertThat(parameters).containsEntry(PATH_PARAM_B, valueOfB);
  }

  @Test
  public void should_get_path_parameters_with_null_value() throws Exception {

    String valueOfA = null;
    String valueOfB = null;

    LinkableResource resource = createResourceWithTwoPathParameters(valueOfA, valueOfB);

    Map<String, Object> parameters = support.getPathParameters(resource);

    assertThat(parameters).containsEntry(PATH_PARAM_A, valueOfA);
    assertThat(parameters).containsEntry(PATH_PARAM_B, valueOfB);
  }

}
