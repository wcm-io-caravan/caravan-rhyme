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
package io.wcm.caravan.rhyme.impl.client;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.COLLECTION;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.FIRST;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.NEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.damnhandy.uri.template.UriTemplate;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceRepresentation;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.MockClientTestSupport;

class LinkRewritingTest {

  private static final String INVALID_URI = "/<>";
  private static final String ENTRY_POINT_PATH = "/";
  private static final String PAGE_PATH_TEMPLATE = "/pages/{index}";
  private static final String ROOT_PATH_TEMPLATE = "{+path}.rhyme";

  private static final String BASE_URL = "http://example.org:12345";
  private static final String ENTRY_POINT_URL = BASE_URL + ENTRY_POINT_PATH;
  private static final String PAGE_URL_TEMPLATE = BASE_URL + PAGE_PATH_TEMPLATE;

  private final MockClientTestSupport client = ClientTestSupport.withMocking();
  private HalResourceLoader mockLoader = client.getMockJsonLoader();

  @Test
  void resolved_links_should_be_rewritten() {

    LinkRewriting rewriting = new LinkRewriting(BASE_URL);

    Link link = new Link(ENTRY_POINT_PATH);
    rewriting.rewriteLink(link);

    assertThat(link.getHref()).isEqualTo(BASE_URL + ENTRY_POINT_PATH);
  }

  @Test
  void path_from_context_uri_should_be_ignored() {

    LinkRewriting rewriting = new LinkRewriting(BASE_URL + "/other/path");

    Link link = new Link(ENTRY_POINT_PATH);
    rewriting.rewriteLink(link);

    assertThat(link.getHref()).isEqualTo(BASE_URL + ENTRY_POINT_PATH);
  }

  @Test
  void templates_with_path_variables_should_be_rewritten() {

    LinkRewriting rewriting = new LinkRewriting(BASE_URL);

    Link link = new Link(PAGE_PATH_TEMPLATE);
    rewriting.rewriteLink(link);

    assertThat(link.getHref()).isEqualTo(BASE_URL + PAGE_PATH_TEMPLATE);
  }

  @Test
  void templates_beginning_with_path_variables_should_be_rewritten() {

    LinkRewriting rewriting = new LinkRewriting(BASE_URL);

    Link link = new Link(ROOT_PATH_TEMPLATE);
    rewriting.rewriteLink(link);

    assertThat(link.getHref()).isEqualTo(BASE_URL + ROOT_PATH_TEMPLATE);
  }

  @Test
  void absolute_links_should_not_be_rewritten() {

    LinkRewriting rewriting = new LinkRewriting(BASE_URL);

    String absoluteUrl = "https://foo.bar/123";

    Link link = new Link(absoluteUrl);
    rewriting.rewriteLink(link);

    assertThat(link.getHref()).isEqualTo(absoluteUrl);
  }

  @Test
  void invalid_links_should_be_ignored() {

    LinkRewriting rewriting = new LinkRewriting(BASE_URL);

    Link link = new Link(INVALID_URI);
    rewriting.rewriteLink(link);

    assertThat(link.getHref()).isEqualTo(INVALID_URI);
  }

  @HalApiInterface
  interface TestEntryPointResource extends LinkableResource {

    @Related(COLLECTION)
    TestPageResource getPage(@TemplateVariable("index") Integer index);

    @Related(FIRST)
    TestPageResource getFirstPage();

    @ResourceRepresentation
    HalResource asHalResource();
  }

  @HalApiInterface
  interface TestPageResource extends LinkableResource {

    @Related(NEXT)
    TestPageResource getNext();

    @ResourceRepresentation
    HalResource asHalResource();
  }

  private String createPagePath(int index) {
    UriTemplate template = UriTemplate.buildFromTemplate(PAGE_PATH_TEMPLATE).build();
    String uri = template.set("index", index).expand();
    return uri;
  }

  private HalResponse createEntryPointResponse() {

    HalResource hal = new HalResource(ENTRY_POINT_PATH);
    hal.setLink(COLLECTION, new Link(PAGE_PATH_TEMPLATE));
    hal.setLink(FIRST, new Link(createPagePath(0)));

    return new HalResponse()
        .withStatus(200)
        .withBody(hal);
  }

  private HalResponse createPageResource(int index) {

    HalResource hal = new HalResource(createPagePath(index));
    hal.setLink(NEXT, new Link(createPagePath(index + 1)));

    return new HalResponse()
        .withStatus(200)
        .withBody(hal);
  }

  private HalResponse createEntryPointResponseWithEmbeddedFirstPage() {

    HalResponse entryPoint = createEntryPointResponse();

    HalResponse firstPage = createPageResource(0);

    entryPoint.getBody().addEmbedded(FIRST, firstPage.getBody());

    return entryPoint;
  }

  private void mockEntryPointResponse(String url, HalResponse response) {

    when(mockLoader.getHalResource(ArgumentMatchers.eq(url)))
        .thenReturn(Single.just(response));
  }

  private void mockPageResponseWithAbsoluteUrl(int index) {

    String url = BASE_URL + createPagePath(index);
    HalResponse response = createPageResource(index);

    when(mockLoader.getHalResource(ArgumentMatchers.eq(url)))
        .thenReturn(Single.just(response));
  }

  private TestEntryPointResource getEntryPoint(String entryPointUrl) {
    Rhyme rhyme = RhymeBuilder.withResourceLoader(client.getMockJsonLoader()).buildForRequestTo("/foo");
    TestEntryPointResource entryPoint = rhyme.getRemoteResource(entryPointUrl, TestEntryPointResource.class);
    return entryPoint;
  }

  @Test
  void links_shouldnt_be_rewritten_if_entrypoint_was_fetched_with_path_url() {

    mockEntryPointResponse(ENTRY_POINT_PATH, createEntryPointResponse());

    HalResource hal = getEntryPoint(ENTRY_POINT_PATH).asHalResource();

    assertThat(hal.getLink().getHref()).isEqualTo(ENTRY_POINT_PATH);
    assertThat(hal.getLink(COLLECTION).getHref()).isEqualTo(PAGE_PATH_TEMPLATE);
    assertThat(hal.getLink(FIRST).getHref()).isEqualTo(createPagePath(0));
  }

  @Test
  void links_shouldnt_be_rewritten_if_entrypoint_was_fetched_with_invalid_uri() {

    mockEntryPointResponse(INVALID_URI, createEntryPointResponse());

    HalResource hal = getEntryPoint(INVALID_URI).asHalResource();

    assertThat(hal.getLink().getHref()).isEqualTo(ENTRY_POINT_PATH);
    assertThat(hal.getLink(COLLECTION).getHref()).isEqualTo(PAGE_PATH_TEMPLATE);
    assertThat(hal.getLink(FIRST).getHref()).isEqualTo(createPagePath(0));
  }

  @Test
  void links_should_be_made_absolute_if_entrypoint_was_fetched_with_absolute_url() {

    mockEntryPointResponse(ENTRY_POINT_URL, createEntryPointResponse());

    HalResource hal = getEntryPoint(ENTRY_POINT_URL).asHalResource();

    assertThat(hal.getLink().getHref()).isEqualTo(ENTRY_POINT_URL);
    assertThat(hal.getLink(COLLECTION).getHref()).isEqualTo(PAGE_URL_TEMPLATE);
    assertThat(hal.getLink(FIRST).getHref()).isEqualTo(BASE_URL + createPagePath(0));
  }

  @Test
  void rewritten_absolute_links_should_be_followed_correctly() {

    mockEntryPointResponse(ENTRY_POINT_URL, createEntryPointResponse());
    mockPageResponseWithAbsoluteUrl(0);
    mockPageResponseWithAbsoluteUrl(1);

    HalResource page1 = getEntryPoint(ENTRY_POINT_URL).getFirstPage().getNext().asHalResource();

    assertThat(page1.getLink().getHref()).isEqualTo(BASE_URL + createPagePath(1));

    verify(mockLoader, times(3)).getHalResource(anyString());
  }

  @Test
  void links_should_be_made_absolute_in_embedded_resources() {

    mockEntryPointResponse(ENTRY_POINT_URL, createEntryPointResponseWithEmbeddedFirstPage());

    HalResource embedded = getEntryPoint(ENTRY_POINT_URL).asHalResource()
        .getEmbeddedResource(FIRST);

    assertThat(embedded.getLink().getHref()).isEqualTo(BASE_URL + createPagePath(0));
    assertThat(embedded.getLink(NEXT).getHref()).isEqualTo(BASE_URL + createPagePath(1));
  }

}
