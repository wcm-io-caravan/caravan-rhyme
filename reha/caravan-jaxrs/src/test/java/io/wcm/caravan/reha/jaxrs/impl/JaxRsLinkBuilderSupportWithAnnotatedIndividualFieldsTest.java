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

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import io.wcm.caravan.reha.api.resources.LinkableResource;

public class JaxRsLinkBuilderSupportWithAnnotatedIndividualFieldsTest extends AbstractJaxRsLinkBuilderSupportTest {

  @Path(RESOURCE_PATH)
  private static class TestResourceWithTwoQueryParameters extends LinkableResourceAdapter {

    @QueryParam(QUERY_PARAM_A)
    private String queryA;

    @QueryParam(QUERY_PARAM_B)
    private String queryB;

    TestResourceWithTwoQueryParameters(String a, String b) {
      this.queryA = a;
      this.queryB = b;
    }
  }

  @Override
  LinkableResource createResourceWithTwoQueryParameters(String valueOfA, String valueOfB) {
    return new TestResourceWithTwoQueryParameters(valueOfA, valueOfB);
  }

  @Path(RESOURCE_PATH_TEMPLATE)
  private static class TestResourceWithTwoPathParameters extends LinkableResourceAdapter {

    @PathParam(PATH_PARAM_A)
    private String pathA;

    @PathParam(PATH_PARAM_B)
    private String pathB;

    TestResourceWithTwoPathParameters(String a, String b) {
      this.pathA = a;
      this.pathB = b;
    }
  }

  @Override
  LinkableResource createResourceWithTwoPathParameters(String valueOfA, String valueOfB) {
    return new TestResourceWithTwoPathParameters(valueOfA, valueOfB);
  }
}
