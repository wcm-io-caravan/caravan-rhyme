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
package io.wcm.caravan.rhyme.aem.impl.linkbuilder;

import static io.wcm.caravan.rhyme.aem.impl.linkbuilder.UrlFingerprintingImpl.TIMESTAMP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.models.annotations.Model;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.Page;
import com.google.common.collect.Ordering;
import com.mercateo.test.clock.TestClock;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.FingerprintBuilder;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.api.resources.ImmutableResource;
import io.wcm.caravan.rhyme.aem.impl.queries.AemPageQueries;
import io.wcm.caravan.rhyme.aem.testing.context.AppAemContext;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import wiremock.com.google.common.collect.ImmutableMap;


@ExtendWith(AemContextExtension.class)
public class UrlFingerprintingImplTest {

  private TestClock clock = TestClock.fixed(Instant.EPOCH, ZoneId.systemDefault());

  private static final Comparator<Page> MOST_RECENT_PAGE_FIRST = Ordering.natural().onResultOf(Page::getLastModified).reversed();

  private AemContext context = AppAemContext.newAemContextWithJcrMock();

  private final Set<Page> createdPages = new TreeSet<Page>(MOST_RECENT_PAGE_FIRST);

  private SlingRhyme createRhyme(String resourcePath) {

    return AppAemContext.createRhymeInstance(context, resourcePath);
  }

  private Map<String, Object> getTimestampParamsForPage(Page page, Class<? extends AbstractLinkableResource> slingModelClass) {

    SlingRhyme rhyme = createRhyme(page.getPath());

    UrlFingerprintingImpl fingerprinting = rhyme.adaptTo(UrlFingerprintingImpl.class);

    AbstractLinkableResource resource = rhyme
        .adaptTo(slingModelClass);

    return fingerprinting.getQueryParams(resource);
  }

  private static String appendSlashToPath(Page page) {
    return page.getPath() + "/";
  }

  private Page createPageWithLastModified(String path, Instant lastModified) {

    Page page = context.create().page(path);

    Calendar calender = Calendar.getInstance();
    calender.setTimeInMillis(lastModified.toEpochMilli());
    context.create().resource(page.getPath() + "/jcr:content", ImmutableMap.of("cq:lastModified", calender));

    assertThat(page.getLastModified().getTimeInMillis())
        .isEqualTo(lastModified.toEpochMilli());

    createdPages.add(page);

    return page;

  }

  private void mockQueryResultForLastModifiedBelow(Page belowPage) {

    mockQueryResultForLastModifiedBelow(belowPage.getPath());
  }

  private void mockQueryResultForLastModifiedBelow(String path) {

    Session session = context.resourceResolver().adaptTo(Session.class);

    String pathWithSlash = path + "/";

    List<Node> resultNodes = createdPages.stream()
        .filter(page -> (page.getPath() + "/").startsWith(pathWithSlash))
        .map(page -> page.getContentResource().adaptTo(Node.class))
        .collect(Collectors.toList());

    String query = AemPageQueries.getLastModifiedPageContentQuery(path);

    MockJcr.setQueryResult(session, query, "xpath", resultNodes);
  }


  @Test
  public void createLinkToCurrentResource_uses_last_modified_of_content_resource() throws Exception {

    Instant lastModified = clock.instant();

    Page page = createPageWithLastModified("/content", lastModified);

    mockQueryResultForLastModifiedBelow(page);

    Map<String, Object> queryParams = getTimestampParamsForPage(page, ResourceWithLastModifiedBelowContentTimestamp.class);

    assertThat(queryParams.get(TIMESTAMP))
        .isEqualTo(lastModified.toString());
  }

  @Test
  public void createLinkToCurrentResource_uses_last_modified_of_child_resource() throws Exception {

    Instant lastModified = clock.instant();
    Page page = createPageWithLastModified("/content", lastModified);

    Instant childModified = lastModified.plusSeconds(5);
    createPageWithLastModified("/content/foo", childModified);

    mockQueryResultForLastModifiedBelow(page);

    Map<String, Object> queryParams = getTimestampParamsForPage(page, ResourceWithLastModifiedBelowContentTimestamp.class);

    assertThat(queryParams.get(TIMESTAMP))
        .isEqualTo(childModified.toString());
  }

  @Test
  public void createLinkToCurrentResource_fails_if_resource_doesnt_exist() throws Exception {

    Instant lastModified = clock.instant();

    Page page = createPageWithLastModified("/foo", lastModified);

    mockQueryResultForLastModifiedBelow("/content");

    Throwable ex = catchThrowable(() -> getTimestampParamsForPage(page, ResourceWithLastModifiedBelowContentTimestamp.class));

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessage("Failed to get most recent cq:lastModified below /content");

    assertThat(ex)
        .hasRootCauseMessage("Not a single cq:PageContent node was found below /content");
  }


  @Model(adaptables = SlingRhyme.class)
  public static class ResourceWithLastModifiedBelowContentTimestamp extends AbstractLinkableResource implements ImmutableResource {

    @Override
    public void buildFingerprint(FingerprintBuilder fingerprint) {

      fingerprint.addLastModifiedOfContentBelow("/content");
    }

    @Override
    protected String getDefaultLinkTitle() {
      return "default link title";
    }
  }

  @Test
  public void createLinkToCurrentResource_uses_last_modified_of_two_roots() throws Exception {

    Instant lastModified = clock.instant();
    Page page = createPageWithLastModified("/content", lastModified);

    Instant fooModified = lastModified.plusSeconds(5);
    Page fooPage = createPageWithLastModified("/content/foo", fooModified);

    Instant barModified = lastModified.plusSeconds(15);
    Page barPage = createPageWithLastModified("/content/bar", barModified);

    mockQueryResultForLastModifiedBelow(fooPage);
    mockQueryResultForLastModifiedBelow(barPage);

    Map<String, Object> queryParams = getTimestampParamsForPage(page, ResourceWithLastModifiedBelowFooAndBarTimestamp.class);

    assertThat(queryParams.get(TIMESTAMP))
        .isEqualTo(barModified.toString());
  }

  @Test
  public void createLinkToCurrentResource_uses_last_modified_of_two_roots_reverse_order() throws Exception {

    Instant lastModified = clock.instant();
    Page page = createPageWithLastModified("/content", lastModified);

    Instant fooModified = lastModified.plusSeconds(15);
    Page fooPage = createPageWithLastModified("/content/foo", fooModified);

    Instant barModified = lastModified.plusSeconds(5);
    Page barPage = createPageWithLastModified("/content/bar", barModified);

    mockQueryResultForLastModifiedBelow(fooPage);
    mockQueryResultForLastModifiedBelow(barPage);

    Map<String, Object> queryParams = getTimestampParamsForPage(page, ResourceWithLastModifiedBelowFooAndBarTimestamp.class);

    assertThat(queryParams.get(TIMESTAMP))
        .isEqualTo(fooModified.toString());
  }


  @Model(adaptables = SlingRhyme.class)
  public static class ResourceWithLastModifiedBelowFooAndBarTimestamp extends AbstractLinkableResource implements ImmutableResource {

    @Override
    public void buildFingerprint(FingerprintBuilder fingerprint) {

      fingerprint.addLastModifiedOfContentBelow("/content/foo");
      fingerprint.addLastModifiedOfContentBelow("/content/bar");
    }

    @Override
    protected String getDefaultLinkTitle() {
      return "default link title";
    }
  }

  @Test
  public void appendIncomingFingerprintTo_should_not_append_anything_without_query_param_in_request() throws Exception {

    String path = "/content";

    SlingRhyme rhyme = createRhyme(path);

    UrlFingerprintingImpl fingerprinting = rhyme.adaptTo(UrlFingerprintingImpl.class);

    String url = fingerprinting.appendIncomingFingerprintTo(path);

    assertThat(url)
        .isEqualTo(path);
  }

  @Test
  public void appendIncomingFingerprintTo_should_append_timestamp_from_request() throws Exception {

    String query = TIMESTAMP + "=foo";
    context.request().setQueryString(query);

    String path = "/content";

    SlingRhyme rhyme = createRhyme(path);

    UrlFingerprintingImpl fingerprinting = rhyme.adaptTo(UrlFingerprintingImpl.class);

    String url = fingerprinting.appendIncomingFingerprintTo(path);

    assertThat(url)
        .isEqualTo(path + "?" + query);
  }

  @Test
  public void appendIncomingFingerprintTo_should_append_timestamp_to_url_with_query_param() throws Exception {

    String query = TIMESTAMP + "=foo";
    context.request().setQueryString(query);

    String path = "/content";

    SlingRhyme rhyme = createRhyme(path);

    UrlFingerprintingImpl fingerprinting = rhyme.adaptTo(UrlFingerprintingImpl.class);

    String url = fingerprinting.appendIncomingFingerprintTo(path + "?bar=123");

    assertThat(url)
        .isEqualTo(path + "?bar=123&" + query);
  }

}
