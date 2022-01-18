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
package io.wcm.caravan.rhyme.aem.impl.adaptation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.api.RhymeResourceRegistration;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.aem.impl.adaptation.SlingModelPostAdaptationStageTest.ClassThatDoesNotImplementSlingLinkableResource;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.context.AppAemContext;
import io.wcm.caravan.rhyme.aem.testing.models.SelectorSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceRegistration;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class SlingResourceAdapterImplTest {

  private AemContext context = AppAemContext.newAemContext();

  @BeforeEach
  void setUp() {
    context.registerService(RhymeResourceRegistration.class, new TestResourceRegistration());
  }

  private SlingResourceAdapterImpl createAdapterInstanceForResource(String resourcePath) {

    SlingRhyme rhyme = createRhymeInstance(resourcePath);

    return rhyme.adaptTo(SlingResourceAdapterImpl.class);
  }

  private SlingRhyme createRhymeInstance(String resourcePath) {

    return AppAemContext.createRhymeInstance(context, resourcePath);
  }

  @Test
  void can_be_adapted_from_SlingRhyme()  {

    SlingRhyme rhyme = createRhymeInstance("/content/foo");

    SlingResourceAdapter adapter = rhyme.adaptTo(SlingResourceAdapter.class);

    assertThat(adapter).isNotNull();
  }

  @Test
  void should_adapt_single_instance_that_exists()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    SlingTestResource resource = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class)
        .getInstance();

    assertThatResourceIsSelectorSlingTestResourceAt("/content/foo", resource);
  }

  @Test
  void should_allow_to_specify_implementation_sling_model_class()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    SelectorSlingTestResource resource = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class, SelectorSlingTestResource.class)
        .getInstance();

    assertThatResourceIsSelectorSlingTestResourceAt("/content/foo", resource);
  }


  @Test
  void should_adapt_models_not_implementing_SlingLinkableResource_if_no_decorators_are_used() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    ClassThatDoesNotImplementSlingLinkableResource resource = adapter
        .selectCurrentResource()
        .adaptTo(ClassThatDoesNotImplementSlingLinkableResource.class)
        .getInstance();

    assertThat(resource).isNotNull();
  }

  @Test
  void should_fail_to_adapt_single_instance_that_does_not_exist()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    Throwable ex = catchThrowable(() -> adapter.selectResourceAt("/content/foo/bar")
        .adaptTo(SlingTestResource.class)
        .getInstance());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("No resources were found after selecting ");
  }

  @Test
  void should_adapt_optional_instance_that_exists()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    Optional<SlingTestResource> resource = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(resource).isPresent();
    assertThatResourceIsSelectorSlingTestResourceAt("/content/foo", resource.get());
  }

  @Test
  void should_return_empty_optional_instance_for_resource_that_does_not_exist()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    Optional<SlingTestResource> resource = adapter.selectResourceAt("/content/foo/bar")
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(resource).isEmpty();
  }

  @Test
  void should_fail_to_adapt_if_no_resources_were_selected()  {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/content/foo");

    Throwable ex = catchThrowable(() -> adapter.adaptTo(SlingTestResource.class).getInstance());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("No resources have been selected with this adapter");
  }


  @Test
  void should_fail_to_adapt_if_class_is_not_adaptable() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    Throwable ex = catchThrowable(() -> adapter.selectCurrentResource()
        .adaptTo(NotAnAdaptableClass.class)
        .getInstance());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to adapt");
  }

  public static class NotAnAdaptableClass {

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
  void fromResourceAt_should_work_for_existing_resource()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.fromResourceAt("/content").selectCurrentResource());

    assertThatResourcesMatch(resources, "/content");
  }

  @Test
  void fromResourceAt_should_fail_for_non_existing_resource()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    Throwable ex = catchThrowable(() -> applySelection(adapter.fromResourceAt("/content/foo").selectCurrentResource()));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("There does not exist a resource at");
  }

  @Test
  void fromResourceAt_should_fail_on_multiple_calls()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    Throwable ex = catchThrowable(() -> applySelection(adapter.fromResourceAt("/content").fromResourceAt("/").selectCurrentResource()));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("You cannot call the from* methods multiple times");
  }

  @Test
  void fromCurrentPage_should_work_from_page_resource()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.fromCurrentPage().selectCurrentResource());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void fromCurrentPage_should_work_from_page_content_resource()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.fromCurrentPage().selectCurrentResource());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void fromCurrentPage_should_work_from_below_page_content_resource()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content/bar");

    List<SlingTestResource> resources = applySelection(adapter.fromCurrentPage().selectCurrentResource());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void fromCurrentPage_should_fail_if_there_is_no_page()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar");

    Throwable ex = catchThrowable(() -> applySelection(adapter.fromCurrentPage().selectCurrentResource()));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageEndingWith("is not a page, and not located within a page");
  }

  @Test
  void fromParentPage_should_work_from_page_resource()  {

    setUpPages("/content/foo", "/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar");

    List<SlingTestResource> resources = applySelection(adapter.fromParentPage().selectCurrentResource());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void fromParentPage_should_work_from_page_content_resource()  {

    setUpPages("/content/foo", "/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.fromParentPage().selectCurrentResource());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void fromParentPage_should_fail_if_there_is_no_parent_page()  {

    setUpPages("/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar");

    Throwable ex = catchThrowable(() -> applySelection(adapter.fromParentPage().selectCurrentResource()));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("The parent resource")
        .hasMessageEndingWith("is not a page");
  }

  @Test
  void fromGrandParentPage_should_work_from_page_resource()  {

    setUpPages("/content/foo", "/content/foo/bar", "/content/foo/bar/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar/foo");

    List<SlingTestResource> resources = applySelection(adapter.fromGrandParentPage().selectCurrentResource());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void fromGrandParentPage_should_work_from_page_content_resource()  {

    setUpPages("/content/foo", "/content/foo/bar", "/content/foo/bar/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.fromGrandParentPage().selectCurrentResource());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void fromGrandParentPage_should_fail_if_there_is_no_parent_page()  {

    setUpPages("/content/foo", "/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar");

    Throwable ex = catchThrowable(() -> applySelection(adapter.fromGrandParentPage().selectCurrentResource()));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("The parent resource")
        .hasMessageEndingWith("is not a page");
  }

  @Test
  void fromParentPage_should_fail_if_called_after_select()  {

    setUpPages("/content/foo", "/content/foo/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar");

    Throwable ex = catchThrowable(() -> applySelection(adapter.selectCurrentResource().fromCurrentPage()));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("The SlingResourceAdapterImpl#fromXyz methods must be called *before* any of the selectXyz methods");
  }


  @Test
  void should_allow_multiple_selections()  {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter
        .selectResourceAt("/content/foo/1")
        .selectResourceAt("/content/foo/2"));

    assertThatResourcesMatch(resources, "/content/foo/1", "/content/foo/2");
  }


  @Test
  void select_should_allow_using_custom_Stream() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    Stream<Resource> stream = Stream.of(
        context.create().resource("/content/foo/1"),
        context.create().resource("/content/foo/2"));

    List<SlingTestResource> resources = adapter.select(stream)
        .adaptTo(SlingTestResource.class)
        .getStream()
        .collect(Collectors.toList());

    assertThatResourcesMatch(resources, "/content/foo/1", "/confent/foo2");
  }

  @Test
  void selectParentResource_should_find_parent() {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectParentResource());

    assertThatResourcesMatch(resources, "/content");
  }

  @Test
  void selectParentResource_should_handle_missing_parent() {

    setUpPages("/content");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/");

    List<SlingTestResource> resources = applySelection(adapter.selectParentResource());

    assertThat(resources).isEmpty();
  }

  @Test
  void selectContentResource_should_find_content() {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentResource());

    assertThatResourcesMatch(resources, "/content/foo/jcr:content");
  }

  @Test
  void selectContentResource_should_handle_missing_content() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentResource());

    assertThat(resources).isEmpty();
  }

  @Test
  void selectChildResources_should_find_children()  {

    setUpPages("/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectChildResources());

    assertThatResourcesMatch(resources, "/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");
  }

  @Test
  void selectChildResources_should_handle_missing_children()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectChildResources());

    assertThat(resources).isEmpty();
  }

  @Test
  void selectChildPages_should_find_child_pages_and_ignore_content()  {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectChildPages());

    assertThatResourcesMatch(resources, "/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");
  }

  @Test
  void selectGrandChildResources_should_find_grand_children()  {

    setUpPages("/content",
        "/content/foo", "/content/foo/1", "/content/foo/2",
        "/content/bar", "/content/bar/1", "/content/bar/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    List<SlingTestResource> resources = applySelection(adapter.selectGrandChildResources());

    assertThatResourcesMatch(resources,
        "/content/foo/jcr:content", "/content/foo/1", "/content/foo/2",
        "/content/bar/jcr:content", "/content/bar/1", "/content/bar/2");
  }

  @Test
  void selectGrandChildResources_should_handle_leaf_page()  {

    setUpPages("/content");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    List<SlingTestResource> resources = applySelection(adapter.selectGrandChildResources());

    assertThat(resources).isEmpty();
  }


  @Test
  void selectGrandChildPages_should_find_grand_children_and_ignore_content()  {

    setUpPages("/content",
        "/content/foo", "/content/foo/1", "/content/foo/2",
        "/content/bar", "/content/bar/1", "/content/bar/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    List<SlingTestResource> resources = applySelection(adapter.selectGrandChildPages());

    assertThatResourcesMatch(resources, "/content/foo/1", "/content/foo/2", "/content/bar/1", "/content/bar/2");
  }

  @Test
  void selectGrandChildPages_should_handle_leaf_page()  {

    setUpPages("/content");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    List<SlingTestResource> resources = applySelection(adapter.selectGrandChildPages());

    assertThat(resources).isEmpty();
  }

  @Test
  void selectGrandChildPages_should_handle_empty_grand_child_page()  {

    setUpPages("/content", "/content/foo", "/content/bar");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    List<SlingTestResource> resources = applySelection(adapter.selectGrandChildPages());

    assertThat(resources).isEmpty();
  }

  @Test
  void selectChildPages_should_handle_empty_children()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    assertThat(context.resourceResolver().getResource("/content/foo/jcr:content")).isNotNull();

    List<SlingTestResource> resources = applySelection(adapter.selectChildPages());

    assertThat(resources).isEmpty();
  }

  @Test
  void selectChildResource_should_find_child_by_name()  {

    setUpPages("/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectChildResource("3"));

    assertThatResourcesMatch(resources, "/content/foo/3");
  }

  @Test
  void selectChildResource_should_handle_non_existent_name()  {

    setUpPages("/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectChildResource("6"));

    assertThat(resources).isEmpty();
  }

  @Test
  void selectSiblingResource_should_find_child_by_name()  {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/1");

    List<SlingTestResource> resources = applySelection(adapter.selectSiblingResource("3"));

    assertThatResourcesMatch(resources, "/content/foo/3");
  }

  @Test
  void selectSiblingResource_should_handle_non_existent_name()  {

    setUpPages("/content/foo/1", "/content/foo/2", "/content/foo/3", "/content/foo/4");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/6");

    List<SlingTestResource> resources = applySelection(adapter.selectChildResource("6"));

    assertThat(resources).isEmpty();
  }

  /*

  @Override
  public SlingResourceAdapter selectGrandChildResources() {

    return resourceSelector.add(ResourceStreams::getGrandChildren, "grand children of {}");
  }


  @Override
  public SlingResourceAdapter selectGrandChildPages() {

    return resourceSelector.add(ResourceStreams::getGrandChildPages, "grand child pages of {}");
  }
  */

  @Test
  void selectContainingPage_should_select_current_resource_from_page_resource()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContainingPage());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void selectContainingPage_should_select_parent_resource_page_content_resource()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContainingPage());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void selectContainingPage_should_work_below_content_resource()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content/foo/bar");

    List<SlingTestResource> resources = applySelection(adapter.selectContainingPage());

    assertThatResourcesMatch(resources, "/content/foo");
  }

  @Test
  void selectContentOfCurrentPage_should_work_from_page_resource()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfCurrentPage());

    assertThatResourcesMatch(resources, "/content/foo/jcr:content");
  }

  @Test
  void selectContentOfCurrentPage_should_work_from_page_content_resource()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfCurrentPage());

    assertThatResourcesMatch(resources, "/content/foo/jcr:content");
  }

  @Test
  void selectContentOfCurrentPage_should_work_from_below_content_resource()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content/foo/bar");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfCurrentPage());

    assertThatResourcesMatch(resources, "/content/foo/jcr:content");
  }

  @Test
  void selectContentOfCurrentPage_should_fail_if_there_is_no_page()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/bar");

    Throwable ex = catchThrowable(() -> applySelection(adapter.selectContentOfCurrentPage()));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageEndingWith("is not a page, and not located within a page");
  }

  @Test
  void selectContentOfChildPages_should_work_from_page_resource()  {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPages());

    assertThatResourcesMatch(resources, "/content/foo/1/jcr:content", "/content/foo/2/jcr:content");
  }

  @Test
  void selectContentOfChildPages_should_work_from_page_content_resource()  {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPages());

    assertThatResourcesMatch(resources, "/content/foo/1/jcr:content", "/content/foo/2/jcr:content");
  }

  @Test
  void selectContentOfChildPages_should_return_empty_stream_if_there_are_no_child_pages()  {

    setUpPages("/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPages());

    assertThat(resources).isEmpty();
  }

  @Test
  void selectContentOfGrandChildPages_should_work_from_page_resource()  {

    setUpPages("/content", "/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandChildPages());

    assertThatResourcesMatch(resources, "/content/foo/1/jcr:content", "/content/foo/2/jcr:content");
  }

  @Test
  void selectContentOfGrandChildPages_should_work_from_page_content_resource()  {

    setUpPages("/content", "/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandChildPages());

    assertThatResourcesMatch(resources, "/content/foo/1/jcr:content", "/content/foo/2/jcr:content");
  }

  @Test
  void selectContentOfGrandChildPages_should_return_empty_stream_if_there_are_no_child_pages()  {

    setUpPages("/content", "/content/foo");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfGrandChildPages());

    assertThat(resources).isEmpty();
  }

  @Test
  void selectContentOfChildPage_should_work_from_page_resource()  {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPage("2"));

    assertThatResourcesMatch(resources, "/content/foo/2/jcr:content");
  }

  @Test
  void selectContentOfChildPage_should_work_from_page_content_resource()  {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo/jcr:content");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPage("2"));

    assertThatResourcesMatch(resources, "/content/foo/2/jcr:content");
  }

  @Test
  void selectContentOfChildPage_should_return_empty_stream_if_child_page_doesnt_exist()  {

    setUpPages("/content/foo", "/content/foo/1", "/content/foo/2");

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    List<SlingTestResource> resources = applySelection(adapter.selectContentOfChildPage("3"));

    assertThat(resources).isEmpty();
  }

  @Test
  void filter_should_remove_non_matches()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    Predicate<Resource> predicate = (res) -> false;

    Optional<SlingTestResource> barResource = adapter.selectCurrentResource()
        .filter(predicate)
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(barResource).isEmpty();
  }

  @Test
  void filter_should_keep_matches()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    Predicate<Resource> predicate = (res) -> true;

    Optional<SlingTestResource> barResource = adapter.selectCurrentResource()
        .filter(predicate)
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(barResource).isPresent();
  }

  @Test
  void filter_should_allow_multiple_filters()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

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
  void should_filter_by_name()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    Optional<SlingTestResource> resource = adapter.selectCurrentResource()
        .filterWithName("bar")
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(resource).isEmpty();
  }

  @Test
  void should_filter_by_adaptability()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

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
  void should_filter_by_adaptability_and_predicate()  {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    context.create().page("/content/foo/page1");
    context.create().resource("/content/foo/resource");
    context.create().page("/content/foo/page2");

    Stream<SlingTestResource> filtered = adapter.selectChildResources()
        .filterAdaptableTo(Page.class, page -> page.getName().equals("page2"))
        .adaptTo(SlingTestResource.class)
        .getStream();

    List<SlingTestResource> resources = filtered.collect(Collectors.toList());

    assertThat(resources).hasSize(1);
    assertThatResourceIsSelectorSlingTestResourceAt("/content/foo/page2", resources.get(0));
  }

  private void assertThatResourceIsSelectorSlingTestResourceAt(String resourcePath, SlingTestResource resource) {

    assertThat(resource).isNotNull();
    assertThat(resource).isInstanceOf(SelectorSlingTestResource.class);

    String expectedHref = resourcePath + "." + SelectorSlingTestResource.SELECTOR + "." + HalApiServlet.EXTENSION;
    assertThat(resource.createLink().getHref()).isEqualTo(expectedHref);
  }

}
