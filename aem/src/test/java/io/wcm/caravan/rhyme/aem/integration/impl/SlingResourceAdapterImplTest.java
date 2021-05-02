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
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.integration.ResourceSelectorProvider;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.SelectorSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceSelectorProvider;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class SlingResourceAdapterImplTest {

  private AemContext context = AppAemContext.newAemContext();

  @BeforeEach
  void setUp() {
    context.registerService(ResourceSelectorProvider.class, new TestResourceSelectorProvider());
  }

  private SlingResourceAdapterImpl createAdapterInstanceForResource(String resourcePath) {

    SlingRhyme rhyme = createRhymeInstance(resourcePath);

    return rhyme.adaptTo(SlingResourceAdapterImpl.class);
  }

  private SlingRhyme createRhymeInstance(String resourcePath) {

    return AppAemContext.createRhymeInstance(context, resourcePath);
  }

  @Test
  public void can_be_adapted_from_SlingRhyme() throws Exception {

    SlingRhyme rhyme = createRhymeInstance("/content/foo");

    SlingResourceAdapter adapter = rhyme.adaptTo(SlingResourceAdapter.class);

    assertThat(adapter).isNotNull();
  }

  @Test
  public void should_adapt_single_instance_that_exists() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    SlingTestResource resource = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class)
        .getInstance();

    assertThatResourceIsSelectorSlingTestResourceAt("/content/foo", resource);
  }

  @Test
  public void should_fail_to_adapt_single_instance_that_does_not_exist() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Throwable ex = catchThrowable(() -> adapter.selectResourceAt("/content/foo/bar")
        .adaptTo(SlingTestResource.class)
        .getInstance());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("No resources were found after selecting ");
  }

  @Test
  public void should_adapt_optional_instance_that_exists() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Optional<SlingTestResource> resource = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(resource).isPresent();
    assertThatResourceIsSelectorSlingTestResourceAt("/content/foo", resource.get());
  }

  @Test
  public void should_return_empty_optional_instance_for_resource_that_does_not_exist() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Optional<SlingTestResource> resource = adapter.selectResourceAt("/content/foo/bar")
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(resource).isEmpty();
  }

  @Test
  public void should_fail_to_adapt_if_no_resources_were_selected() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Throwable ex = catchThrowable(() -> adapter.adaptTo(SlingTestResource.class).getInstance());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("No resources have been selected with this adapter");
  }

  private void setUpPages(String... resourcePaths) {

    context.create().page(resourcePaths[0]);

    for (int i = 1; i < resourcePaths.length; i++) {
      context.create().page(resourcePaths[i]);
    }
  }

  private List<SlingTestResource> applySelection(SlingResourceAdapter adapter) {

    return adapter
        .adaptTo(SlingTestResource.class)
        .getStream()
        .collect(Collectors.toList());
  }

  private void assertThatResourcesMatch(List<SlingTestResource> resources, String... resourcePaths) {

    assertThat(resources).hasSize(resourcePaths.length);
    for (int i = 0; i < resourcePaths.length; i++) {
      assertThatResourceIsSelectorSlingTestResourceAt(resourcePaths[0], resources.get(0));
    }
  }

  @Test
  public void should_allow_multiple_selections() throws Exception {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter
        .selectResourceAt("/content/foo/1")
        .selectResourceAt("/content/foo/2"));

    assertThatResourcesMatch(resources, "/content/foo/1", "/content/foo/2");
  }


  @Test
  public void select_should_allow_using_custom_Stream() {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Stream<Resource> stream = Stream.of(
        context.create().resource("/content/foo/1"),
        context.create().resource("/content/foo/2"));

    adapter.select(stream)
        .adaptTo(SlingTestResource.class)
        .getStream();
  }

  @Test
  public void selectParentResource_should_find_parent() {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectParentResource());

    assertThatResourcesMatch(resources, "/content");
  }

  @Test
  public void selectParentResource_should_handle_missing_parent() {

    setUpPages("/content");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/");

    List<SlingTestResource> resources = applySelection(adapter.selectParentResource());

    assertThat(resources).isEmpty();
  }

  @Test
  public void selectChildResources_should_find_children() throws Exception {

    setUpPages("/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectChildResources());

    assertThatResourcesMatch(resources, "/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");
  }

  @Test
  public void selectChildResources_should_handle_missing_children() throws Exception {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectChildResources());

    assertThat(resources).isEmpty();
  }

  @Test
  public void selectChildResource_should_find_child_by_name() throws Exception {

    setUpPages("/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectChildResource("3"));

    assertThatResourcesMatch(resources, "/content/foo/3");
  }

  @Test
  public void selectChildResource_should_handle_non_existent_name() throws Exception {

    setUpPages("/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectChildResource("6"));

    assertThat(resources).isEmpty();
  }

  @Test
  public void selectContentOfCurrentPage_should_work_from_page_resource() throws Exception {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfCurrentPage());

    assertThatResourcesMatch(resources, "/content/foo/_jcr_content");
  }

  @Test
  public void selectContentOfCurrentPage_should_work_from_page_content_resource() throws Exception {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfCurrentPage());

    assertThatResourcesMatch(resources, "/content/foo/_jcr_content");
  }

  @Test
  public void selectContentOfCurrentPage_should_work_from_below_content_resource() throws Exception {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content/foo/bar");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfCurrentPage());

    assertThatResourcesMatch(resources, "/content/foo/_jcr_content");
  }

  @Test
  public void selectContentOfCurrentPage_should_return_empty_stream_if_there_is_no_page() throws Exception {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content/foo/bar");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfCurrentPage());

    assertThat(resources).isEmpty();
  }

  @Test
  public void selectContentOfParentPage_should_work_from_page_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfParentPage());

    assertThatResourcesMatch(resources, "/content/foo/_jcr_content");
  }

  @Test
  public void selectContentOfParentPage_should_work_from_page_content_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfParentPage());

    assertThatResourcesMatch(resources, "/content/foo/_jcr_content");
  }

  @Test
  public void selectContentOfParentPage_should_return_empty_stream_if_there_is_no_parent_page() throws Exception {

    setUpPages("/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandParentPage());

    assertThat(resources).isEmpty();
  }

  @Test
  public void selectContentOfGrandParentPage_should_work_from_page_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/bar", "/content/foo/bar/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandParentPage());

    assertThatResourcesMatch(resources, "/content/foo/_jcr_content");
  }

  @Test
  public void selectContentOfGrandParentPage_should_work_from_page_content_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/bar", "/content/foo/bar/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandParentPage());

    assertThatResourcesMatch(resources, "/content/foo/_jcr_content");
  }

  @Test
  public void selectContentOfGrandParentPage_should_return_empty_stream_if_there_is_no_grand_parent_page() throws Exception {

    setUpPages("/content/foo", "/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandParentPage());

    assertThat(resources).isEmpty();
  }


  @Test
  public void selectContentOfChildPages_should_work_from_page_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPages());

    assertThatResourcesMatch(resources, "/content/foo/1/_jcr_content", "/content/foo/2/_jcr_content");
  }

  @Test
  public void selectContentOfChildPages_should_work_from_page_content_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPages());

    assertThatResourcesMatch(resources, "/content/foo/1/_jcr_content", "/content/foo/2/_jcr_content");
  }

  @Test
  public void selectContentOfChildPages_should_return_empty_stream_if_there_are_no_child_pages() throws Exception {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPages());

    assertThat(resources).isEmpty();
  }


  @Test
  public void selectContentOfChildPage_should_work_from_page_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPage("2"));

    assertThatResourcesMatch(resources, "/content/foo/2/_jcr_content");
  }

  @Test
  public void selectContentOfChildPage_should_work_from_page_content_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPage("2"));

    assertThatResourcesMatch(resources, "/content/foo/2/_jcr_content");
  }

  @Test
  public void selectContentOfChildPage_should_return_empty_stream_if_child_page_doesnt_exist() throws Exception {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPage("3"));

    assertThat(resources).isEmpty();
  }

  @Test
  public void selectContentOfGrandChildPages_should_work_from_page_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/bar", "/content/foo/bar/1", "/content/foo/bar/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandChildPages());

    assertThatResourcesMatch(resources, "/content/foo/bar/1/_jcr_content", "/content/foo/bar/2/_jcr_content");
  }

  @Test
  public void selectContentOfGrandChildPages_should_work_from_page_content_resource() throws Exception {

    setUpPages("/content/foo", "/content/foo/bar", "/content/foo/bar/1", "/content/foo/bar/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandChildPages());

    assertThatResourcesMatch(resources, "/content/foo/bar/1/_jcr_content", "/content/foo/bar/2/_jcr_content");
  }

  @Test
  public void selectContentOfGrandChildPages_should_return_empty_stream_if_there_are_no_child_pages() throws Exception {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandChildPages());

    assertThat(resources).isEmpty();
  }

  @Test
  public void selectContentOfGrandChildPages_should_return_empty_stream_if_there_are_no_grand_child_pages() throws Exception {

    setUpPages("/content/foo", "/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandChildPages());

    assertThat(resources).isEmpty();
  }

  @Test
  public void filter_should_remove_non_matches() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Predicate<Resource> predicate = (res) -> false;

    Optional<SlingTestResource> barResource = adapter.selectCurrentResource()
        .filter(predicate)
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(barResource).isEmpty();
  }

  @Test
  public void filter_should_keep_matches() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Predicate<Resource> predicate = (res) -> true;

    Optional<SlingTestResource> barResource = adapter.selectCurrentResource()
        .filter(predicate)
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(barResource).isPresent();
  }

  @Test
  public void filter_should_allow_multiple_filters() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Predicate<Resource> predicate1 = (res) -> true;
    Predicate<Resource> predicate2 = (res) -> false;

    Optional<SlingTestResource> resource = adapter.selectCurrentResource()
        .filter(predicate1)
        .filter(predicate2)
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(resource).isEmpty();
  }


  @Test
  public void should_filter_by_name() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Optional<SlingTestResource> resource = adapter.selectCurrentResource()
        .filterWithName("bar")
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(resource).isEmpty();
  }

  @Test
  public void should_filter_by_adaptability() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    context.create().page("/content/foo/page");
    context.create().resource("/content/foo/resource");

    Stream<SlingTestResource> filtered = adapter.selectChildResources()
        .filterAdaptableTo(Page.class)
        .adaptTo(SlingTestResource.class)
        .getStream();

    List<SlingTestResource> resources = filtered.collect(Collectors.toList());

    assertThat(resources).hasSize(1);
    assertThatResourceIsSelectorSlingTestResourceAt("/content/foo/page", resources.get(0));
  }

  @Test
  public void withLinkTitle_allows_to_overide_title_for_SlingLinkableResource_instances() {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    String customTitle = "New Link Title";

    SlingTestResource resource = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class)
        .withLinkTitle(customTitle)
        .getInstance();

    assertThat(resource.createLink().getTitle()).isEqualTo(customTitle);
  }

  @Test
  public void withLinkTitle_fails_overide_title_if_model_class_does_not_implement_SlingLinkableResource() {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    String customTitle = "New Link Title";

    Throwable ex = catchThrowable(() -> adapter.selectCurrentResource()
        .adaptTo(ClassThatDoesNotImplementSlingLinkableResource.class)
        .withLinkTitle(customTitle)
        .getInstance());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Your model class " + ClassThatDoesNotImplementSlingLinkableResource.class.getSimpleName() + " does not implement "
            + SlingLinkableResource.class.getName());

  }

  @Model(adaptables = Resource.class, adapters = ClassThatDoesNotImplementSlingLinkableResource.class)
  public static class ClassThatDoesNotImplementSlingLinkableResource implements EmbeddableResource {

  }

  private void assertThatResourceIsSelectorSlingTestResourceAt(String resourcePath, SlingTestResource resource) {

    assertThat(resource).isNotNull();
    assertThat(resource).isInstanceOf(SelectorSlingTestResource.class);

    String expectedHref = resourcePath + "." + SelectorSlingTestResource.SELECTOR + "." + HalApiServlet.EXTENSION;
    assertThat(resource.createLink().getHref()).isEqualTo(expectedHref);
  }

}
