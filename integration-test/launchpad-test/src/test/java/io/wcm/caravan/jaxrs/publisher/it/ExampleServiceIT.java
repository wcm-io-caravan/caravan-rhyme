/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
package io.wcm.caravan.jaxrs.publisher.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.wcm.caravan.hal.api.annotations.StandardRelations;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class ExampleServiceIT {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

  private static final String SERVER_URL = "http://localhost:8080"; //System.getProperty("launchpad.http.server.url");
  private static final String SERVICE_ID = "/caravan/hal/sample-service";
  private static final String ITEM_COLLECTION_PATH = SERVICE_ID + "/collections/items";
  private static final String COLLECTION_EXAMPLES_PATH = SERVICE_ID + "/collections";

  @Test
  public void testInvalidPathNotFound() throws IOException {
    assertNotFound(SERVICE_ID + "/invalidPath");
  }

  @Test
  public void loadEntryPointResource() throws IOException {
    HalResource hal = assertHalResourceFoundAt(SERVICE_ID);

    Link link = hal.getLink(StandardRelations.COLLECTION);
    assertThat(link).as("collection link").isNotNull();
    assertThat(link.getHref()).as("collection href").startsWith(COLLECTION_EXAMPLES_PATH);
  }

  @Test
  public void loadCollectionExamplesResource() throws IOException {
    HalResource hal = assertHalResourceFoundAt(COLLECTION_EXAMPLES_PATH);

    Link link = hal.getLink(StandardRelations.COLLECTION);
    assertThat(link).as("collection link").isNotNull();
    assertThat(link.getHref()).as("collection href").startsWith(COLLECTION_EXAMPLES_PATH);

    assertThat(link.isTemplated()).as("collection link templated?").isTrue();

    UriTemplate template = UriTemplate.fromTemplate(link.getHref());
    assertThat(template.getVariables()).as("template variables")
        .contains("numItems")
        .contains("embedItems");

    Link itemTemplate = hal.getLink(StandardRelations.ITEM);
    assertThat(itemTemplate.isTemplated()).as("item templated").isTrue();
    assertThat(itemTemplate.getHref()).as("item template").startsWith(ITEM_COLLECTION_PATH + "/{index}");
  }

  @Test
  public void loadItemCollectionResource() throws IOException {
    HalResource hal = assertHalResourceFoundAt(ITEM_COLLECTION_PATH + "?numItems=5");

    List<Link> links = hal.getLinks(StandardRelations.ITEM);
    assertThat(links).as("number of item links").hasSize(5);
    assertThat(links.get(0).getHref()).as("first item href")
        .startsWith(ITEM_COLLECTION_PATH + "/0");
  }

  @Test
  public void loadItemCollectionResourceWithEmbeddedItems() throws IOException {
    HalResource hal = assertHalResourceFoundAt(ITEM_COLLECTION_PATH + "?numItems=5&embedItems=true");

    List<HalResource> embedded = hal.getEmbedded(StandardRelations.ITEM);
    assertThat(embedded).as("number of item links").hasSize(5);
    assertThat(embedded.get(0).getLink().getHref()).as("self ref of embedded")
        .startsWith(ITEM_COLLECTION_PATH + "/0");
  }

  @Test
  public void loadItemResource() throws IOException {
    HalResource hal = assertHalResourceFoundAt(ITEM_COLLECTION_PATH + "/3");

    assertThat(hal.getModel().get("index").asInt()).as("index property from state")
        .isEqualTo(3);
  }

  private HalResource assertHalResourceFoundAt(String url) throws IOException {

    String fullUrl = SERVER_URL + url;
    HttpResponse response = getResponse(fullUrl);

    assertThat(response.getStatusLine().getStatusCode()).as("Response code for " + fullUrl)
        .isEqualTo(HttpServletResponse.SC_OK);

    assertThat(response.getFirstHeader("Content-Type").getValue()).as("Content type for " + fullUrl)
        .isEqualTo(HalResource.CONTENT_TYPE);

    String jsonString = EntityUtils.toString(response.getEntity());
    assertThat(jsonString).as("JSON response").isNotBlank();

    JsonNode json = JSON_FACTORY.createParser(jsonString).readValueAsTree();
    HalResource halResource = new HalResource(json);

    assertThat(halResource.getLink()).as("self link").isNotNull();
    assertThat(halResource.getLink().getHref()).as("self URL").startsWith(url);

    return halResource;
  }

  private void assertNotFound(String url) throws IOException {
    String fullUrl = SERVER_URL + url;
    HttpResponse response = getResponse(fullUrl);

    assertThat(response.getStatusLine().getStatusCode()).as("Response code for " + fullUrl)
        .isEqualTo(HttpServletResponse.SC_NOT_FOUND);
  }

  private HttpResponse getResponse(String fullUrl) throws IOException, ClientProtocolException {
    HttpGet get = new HttpGet(fullUrl);

    CloseableHttpClient client = HttpClientBuilder.create().build();
    return client.execute(get);
  }
}
