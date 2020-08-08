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

import javax.ws.rs.BeanParam;
import javax.ws.rs.Path;

import org.junit.Test;

import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.jaxrs.impl.JaxRsLinkBuilderSupportWithAnnotatedBeanParamTest.TwoPathParametersBean;
import io.wcm.caravan.reha.jaxrs.impl.JaxRsLinkBuilderSupportWithAnnotatedBeanParamTest.TwoQueryParametersBean;

public class JaxRsLinkBuilderSupportWithBeanConstructorParamTest extends AbstractJaxRsLinkBuilderSupportTest {


  @Path(RESOURCE_PATH)
  private static class TestResourceWithTwoQueryParameters extends LinkableResourceAdapter {

    private final TwoQueryParametersBean parameters;

    public TestResourceWithTwoQueryParameters(@BeanParam TwoQueryParametersBean parameters) {
      this.parameters = parameters;
    }
  }

  @Override
  LinkableResource createResourceWithTwoQueryParameters(String valueOfA, String valueOfB) {

    TwoQueryParametersBean parameters = new TwoQueryParametersBean(valueOfA, valueOfB);
    return (new TestResourceWithTwoQueryParameters(parameters));
  }

  @Path(RESOURCE_PATH_TEMPLATE)
  private static class TestResourceWithTwoPathParameters extends LinkableResourceAdapter {

    private final TwoPathParametersBean parameters;

    public TestResourceWithTwoPathParameters(@BeanParam TwoPathParametersBean parameters) {
      this.parameters = parameters;
    }
  }

  @Override
  LinkableResource createResourceWithTwoPathParameters(String valueOfA, String valueOfB) {
    TwoPathParametersBean parameters = new TwoPathParametersBean(valueOfA, valueOfB);
    return new TestResourceWithTwoPathParameters(parameters);
  }


  @Test
  public void should_allow_query_param_bean_to_be_null() throws Exception {

    TestResourceWithTwoQueryParameters resource = new TestResourceWithTwoQueryParameters(null);

    Map<String, Object> parameters = support.getQueryParameters(resource);

    assertThat(parameters).containsEntry(QUERY_PARAM_A, null);
    assertThat(parameters).containsEntry(QUERY_PARAM_B, null);
  }

  @Test
  public void should_allow_path_param_bean_to_be_null() throws Exception {

    TestResourceWithTwoPathParameters resource = new TestResourceWithTwoPathParameters(null);

    Map<String, Object> parameters = support.getPathParameters(resource);

    assertThat(parameters).containsEntry(PATH_PARAM_A, null);
    assertThat(parameters).containsEntry(PATH_PARAM_B, null);
  }
}
