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
package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpHeaders;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.integration.ResourceSelectorProvider;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.ResourceTypeSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.SelectorSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceSelectorProvider;
import io.wcm.caravan.rhyme.api.relations.VndErrorRelations;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class HalApiServletTest {

  private static final String TEST_RESOURCE_PATH = "/content/test";

  private AemContext context = AppAemContext.newAemContext();

  private HalApiServlet servlet;

  @BeforeEach
  void setUp() {

    context.registerService(ResourceSelectorProvider.class, new TestResourceSelectorProvider());

    servlet = context.registerInjectActivateService(new HalApiServlet());
  }

  private Resource createResourceWithResourceType(String path, String resourceType) {
    return context.create().resource(path, SlingConstants.PROPERTY_RESOURCE_TYPE, resourceType);
  }

  private MockSlingHttpServletResponse requestResource(Resource resource, String selector) throws ServletException, IOException {

    context.requestPathInfo().setResourcePath(resource.getPath());
    context.requestPathInfo().setExtension(HalApiServlet.EXTENSION);
    context.requestPathInfo().setSelectorString(selector);

    MockSlingHttpServletRequest request = context.request();

    request.setResource(resource);

    MockSlingHttpServletResponse response = context.response();
    servlet.doGet(request, response);

    return response;
  }


  @Test
  public void doGet_without_selector_should_return_test_resource_registered_to_resource_type() throws Exception {

    Resource resource = createResourceWithResourceType(TEST_RESOURCE_PATH, ResourceTypeSlingTestResource.RESOURCE_TYPE);

    MockSlingHttpServletResponse response = requestResource(resource, null);

    HalResource halResource = assertStatusIsOkAndParse(response);

    assertThatSelfLinkHasHrefAndTitle(halResource, context.request().getPathInfo(), ResourceTypeSlingTestResource.DEFAULT_TITLE);

    assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isNull();
  }

  @Test
  public void doGet_with_selector_should_return_test_resource_registered_to_selector() throws Exception {

    Resource resource = context.create().resource(TEST_RESOURCE_PATH);

    MockSlingHttpServletResponse response = requestResource(resource, SelectorSlingTestResource.SELECTOR);

    HalResource halResource = assertStatusIsOkAndParse(response);

    assertThatSelfLinkHasHrefAndTitle(halResource, context.request().getPathInfo(), SelectorSlingTestResource.DEFAULT_TITLE);

    assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("max-age=" + SelectorSlingTestResource.MAX_AGE_SECONDS);
  }


  @Test
  public void doGet_with_path_to_non_existing_resource_should_return_404() throws Exception {

    Resource resource = new NonExistingResource(context.resourceResolver(), "/does/not/exist");

    MockSlingHttpServletResponse response = requestResource(resource, null);

    HalResource vndErrorResource = assertStatusIsNotOkAndParseVndError(response, HttpStatus.SC_NOT_FOUND);

    Link aboutLink = vndErrorResource.getLink(VndErrorRelations.ABOUT);
    assertThat(aboutLink).isNotNull();
    assertThat(aboutLink.getHref()).isEqualTo("http://localhost/does/not/exist.rhyme");
  }

  @Test
  public void caravan_metadata_should_be_removed_by_default() throws Exception {

    Resource resource = createResourceWithResourceType(TEST_RESOURCE_PATH, ResourceTypeSlingTestResource.RESOURCE_TYPE);

    MockSlingHttpServletResponse response = requestResource(resource, null);

    HalResource halResource = assertStatusIsOkAndParse(response);

    assertThat(halResource.getEmbedded("caravan:metadata")).isEmpty();
  }

  @Test
  public void caravan_metadata_should_be_maintained_if_query_parameter_is_present() throws Exception {

    Resource resource = createResourceWithResourceType(TEST_RESOURCE_PATH, ResourceTypeSlingTestResource.RESOURCE_TYPE);

    context.request().setQueryString(HalApiServlet.QUERY_PARAM_EMBED_METADATA);

    MockSlingHttpServletResponse response = requestResource(resource, null);

    HalResource halResource = assertStatusIsOkAndParse(response);

    assertThat(halResource.getEmbedded("caravan:metadata")).hasSize(1);
  }

  @Test
  public void curies_to_custom_link_relations_should_be_present() throws Exception {

    Resource resource = context.create().resource(TEST_RESOURCE_PATH);

    MockSlingHttpServletResponse response = requestResource(resource, SelectorSlingTestResource.SELECTOR);

    HalResource halResource = assertStatusIsOkAndParse(response);

    Link curies = halResource.getLink("curies");

    assertThat(curies).isNotNull();

    assertThat(curies.getName())
        .isEqualTo("test");

    assertThat(curies.getHref())
        .isEqualTo("/content.rhymedocs.html/" + SlingTestResource.class.getName() + ".html");
  }


  private void assertThatSelfLinkHasHrefAndTitle(HalResource halResource, String href, String title) {
    assertThat(halResource.getLink()).isNotNull();

    assertThat(halResource.getLink().getTitle()).isEqualTo(title);
    assertThat(halResource.getLink().getHref()).isEqualTo(href);
  }

  private HalResource assertStatusIsNotOkAndParseVndError(MockSlingHttpServletResponse response, int expectedStatus) throws IOException, JsonParseException {

    assertThat(response.getStatus()).isEqualTo(expectedStatus);
    assertThat(response.getContentType()).isEqualTo("application/vnd.error+json;charset=UTF-8");

    return parseAsHalResource(response);
  }

  private HalResource assertStatusIsOkAndParse(MockSlingHttpServletResponse response) throws IOException, JsonParseException {

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    assertThat(response.getContentType()).isEqualTo("application/hal+json;charset=UTF-8");

    return parseAsHalResource(response);
  }

  private HalResource parseAsHalResource(MockSlingHttpServletResponse response) throws IOException, JsonParseException {
    JsonFactory factory = new JsonFactory(new ObjectMapper());
    ObjectNode json = factory.createParser(response.getOutputAsString()).readValueAs(ObjectNode.class);

    return new HalResource(json);
  }
}
