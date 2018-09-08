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
package io.wcm.caravan.hal.api.server.jaxrs;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import io.wcm.caravan.hal.api.common.LinkableResource;

public class JaxRsLinkBuilderTestWithAnnotatedBeanParamTest extends JaxRsLinkBuilderTest {

  static class TwoQueryParametersBean {

    @QueryParam(QUERY_PARAM_A)
    private String queryA;

    @QueryParam(QUERY_PARAM_B)
    private String queryB;

    public TwoQueryParametersBean(String a, String b) {
      this.queryA = a;
      this.queryB = b;
    }
  }

  @Path(RESOURCE_PATH)
  private static class TestResourceWithTwoQueryParameters extends LinkableResourceAdapter {

    @BeanParam
    private TwoQueryParametersBean parameters;

    public TestResourceWithTwoQueryParameters(TwoQueryParametersBean parameters) {
      this.parameters = parameters;
    }
  }

  @Override
  LinkableResource createResourceWithTwoQueryParameters(String valueOfA, String valueOfB) {

    TwoQueryParametersBean parameters = new TwoQueryParametersBean(valueOfA, valueOfB);
    return (new TestResourceWithTwoQueryParameters(parameters));
  }

  static class TwoPathParametersBean {

    @PathParam(PATH_PARAM_A)
    private String pathA;

    @PathParam(PATH_PARAM_B)
    private String pathB;

    public TwoPathParametersBean(String a, String b) {
      this.pathA = a;
      this.pathB = b;
    }
  }

  @Path(RESOURCE_PATH_TEMPLATE)
  private static class TestResourceWithTwoPathParameters extends LinkableResourceAdapter {

    @BeanParam
    private TwoPathParametersBean parameters;

    public TestResourceWithTwoPathParameters(TwoPathParametersBean parameters) {
      this.parameters = parameters;
    }
  }

  @Override
  LinkableResource createResourceWithTwoPathParameters(String valueOfA, String valueOfB) {
    TwoPathParametersBean parameters = new TwoPathParametersBean(valueOfA, valueOfB);
    return new TestResourceWithTwoPathParameters(parameters);
  }

}
