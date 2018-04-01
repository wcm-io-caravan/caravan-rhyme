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

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import io.wcm.caravan.hal.api.common.LinkableResource;
import io.wcm.caravan.hal.resource.Link;


public class JaxRsLinkBuilderTest {

  private static final String CONTEXT_PATH = "/context";

  private static final String RESOURCE_PATH = "/test";

  private static final String QUERY_PARAM_A = "queryA";

  private static final String QUERY_PARAM_B = "queryB";

  private static Link buildLinkTo(LinkableResource targetResource) {

    JaxRsLinkBuilder linkBuilder = new JaxRsLinkBuilder(CONTEXT_PATH, targetResource);

    return linkBuilder.build();
  }

  class LinkableResourceAdapter implements LinkableResource {

    @Override
    public Link createLink() {
      throw new NotImplementedException("#createLink should never be called by the LinkBuilder");
    }

  }

  @Path(RESOURCE_PATH)
  class TestResourceWithoutParameters extends LinkableResourceAdapter {
    // a class without any parameter properties
  }

  @Test
  public void link_should_concatenate_context_path_and_resource_path() {

    Link link = buildLinkTo(new TestResourceWithoutParameters());

    assertThat(link.getHref().startsWith(CONTEXT_PATH + RESOURCE_PATH));
  }

  @Path(RESOURCE_PATH)
  public class TestResourceWithTwoQueryParameters extends LinkableResourceAdapter {

    @QueryParam(QUERY_PARAM_A)
    private final String queryA;

    @QueryParam(QUERY_PARAM_B)
    private final String queryB;

    public TestResourceWithTwoQueryParameters(String a, String b) {
      this.queryA = a;
      this.queryB = b;
    }
  }

  @Test
  public void link_should_resolve_query_parameters_with_non_null_value() throws Exception {

    String valueOfA = "testA";
    String valueOfB = "testB";

    Link link = buildLinkTo(new TestResourceWithTwoQueryParameters(valueOfA, valueOfB));

    assertThat(link.isTemplated()).isFalse();
    assertThat(link.getHref()).endsWith("?" + QUERY_PARAM_A + "=" + valueOfA + "&" + QUERY_PARAM_B + "=" + valueOfB);
  }

  @Test
  public void link_should_insert_variables_for_query_parameters_with_null_value() throws Exception {

    String valueOfA = null;
    String valueOfB = null;

    Link link = buildLinkTo(new TestResourceWithTwoQueryParameters(valueOfA, valueOfB));

    assertThat(link.isTemplated());
    assertThat(link.getHref()).endsWith("{?" + QUERY_PARAM_A + "," + QUERY_PARAM_B + "}");
  }

}
