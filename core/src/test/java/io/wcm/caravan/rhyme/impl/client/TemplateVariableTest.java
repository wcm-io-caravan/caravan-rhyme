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

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.rhyme.impl.client.ClientTestSupport.ENTRY_POINT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.damnhandy.uri.template.UriTemplate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.rhyme.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;
import io.wcm.caravan.rhyme.testing.resources.TestResourceState;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
class TemplateVariableTest {

  private final MockClientTestSupport client = ClientTestSupport.withMocking();
  private final HalResource entryPoint = new HalResource();

  @BeforeEach
  void setUp() {
    client.mockHalResponse(ENTRY_POINT_URI, entryPoint);
  }

  private void mockHalResponseWithNumber(String url, int number) {
    TestResourceState state = new TestResourceState();
    state.number = number;

    HalResource resource = new HalResource(state, url);
    client.mockHalResponse(url, resource);
  }

  @HalApiInterface
  interface ResourceWithSimpleLinkTemplate {

    @Related(ITEM)
    Single<ResourceWithSingleState> getLinked(@TemplateVariable("number") Integer number);
  }

  @Test
  void link_template_can_be_followed_if_only_parameter_is_given() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    mockHalResponseWithNumber("/item/1", 1);

    TestResourceState state = client.createProxy(ResourceWithSimpleLinkTemplate.class)
        .getLinked(1)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(1);
  }

  @Test
  void link_template_should_not_be_expanded_if_only_parameter_is_missing() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    mockHalResponseWithNumber("/item/", 0);

    Link link = client.createProxy(ResourceWithSimpleLinkTemplate.class)
        .getLinked(null)
        .map(ResourceWithSingleState::createLink)
        .blockingGet();

    assertThat(link.getHref())
        .isEqualTo("/item/{number}");
  }

  @Test
  void link_template_should_be_followed_with_incomplete_url_if_only_parameter_is_missing() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    mockHalResponseWithNumber("/item/", 0);

    TestResourceState state = client.createProxy(ResourceWithSimpleLinkTemplate.class)
        .getLinked(null)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state)
        .isNotNull();
  }

  @HalApiInterface
  interface ResourceWithComplexLinkTemplate {

    String TEMPLATE = "/item/{number}{?optionalFlag}";

    @Related(ITEM)
    Single<ResourceWithSingleState> getLinked(
        @TemplateVariable("number") Integer number,
        @TemplateVariable("optionalFlag") Boolean optionalFlag);
  }

  @Test
  void link_template_should_be_expanded_and_followed_if_one_of_multiple_parameters_is_missing() {

    entryPoint.addLinks(ITEM, new Link(ResourceWithComplexLinkTemplate.TEMPLATE));

    mockHalResponseWithNumber("/item/1", 1);

    TestResourceState state = client.createProxy(ResourceWithComplexLinkTemplate.class)
        .getLinked(1, null)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(1);
  }

  @Test
  void link_template_should_be_expanded_and_followed_if_all_of_multiple_parameters_are_present() {

    entryPoint.addLinks(ITEM, new Link(ResourceWithComplexLinkTemplate.TEMPLATE));

    mockHalResponseWithNumber("/item/1?optionalFlag=true", 1);

    TestResourceState state = client.createProxy(ResourceWithComplexLinkTemplate.class)
        .getLinked(1, true)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(1);
  }

  @Test
  void link_template_should_be_preserved_if_all_parameters_are_null() {

    entryPoint.addLinks(ITEM, new Link(ResourceWithComplexLinkTemplate.TEMPLATE));

    Link link = client.createProxy(ResourceWithComplexLinkTemplate.class)
        .getLinked(null, null)
        .map(ResourceWithSingleState::createLink)
        .blockingGet();

    assertThat(link.getHref())
        .isEqualTo(ResourceWithComplexLinkTemplate.TEMPLATE);
  }

  @HalApiInterface
  interface ResourceWithOptionalLinkedResource {

    @Related(ITEM)
    Optional<ResourceWithSingleState> getLinked(@TemplateVariable("number") Integer number);
  }

  @Test
  void linked_resource_should_not_be_present_if_there_is_no_link_in_entryPoint() {
    assertThat(client.createProxy(ResourceWithOptionalLinkedResource.class)
        .getLinked(null))
        .isNotPresent();
  }

  @Test
  void link_template_should_be_partially_expanded_if_some_parameters_are_null() {

    entryPoint.addLinks(ITEM, new Link(ResourceWithComplexLinkTemplate.TEMPLATE));

    Link link = client.createProxy(ResourceWithComplexLinkTemplate.class)
        .getLinked(1, null)
        .map(ResourceWithSingleState::createLink)
        .blockingGet();

    assertThat(link.getHref())
        .isEqualTo("/item/1{?optionalFlag}");
  }

  @Test
  void link_template_should_be_expanded_if_no_parameters_are_null() {

    entryPoint.addLinks(ITEM, new Link(ResourceWithComplexLinkTemplate.TEMPLATE));

    Link link = client.createProxy(ResourceWithComplexLinkTemplate.class)
        .getLinked(1, false)
        .map(ResourceWithSingleState::createLink)
        .blockingGet();

    assertThat(link.getHref())
        .isEqualTo("/item/1?optionalFlag=false");
  }

  @HalApiInterface
  interface ResourceWithTemplateAndResolvedLinks {

    @Related(ITEM)
    Single<ResourceWithSingleState> getLinked(@TemplateVariable("number") Integer number);

    @Related(ITEM)
    Observable<ResourceWithSingleState> getAllLinked();
  }

  @Test
  void ignore_link_template_if_method_without_template_variable_is_called_and_there_are_resolved_links() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    int numResolvedLinks = 5;
    Observable.range(0, numResolvedLinks).forEach(i -> {
      String url = "/item/" + i;
      entryPoint.addLinks(ITEM, new Link(url));

      mockHalResponseWithNumber(url, i);
    });

    List<TestResourceState> states = client.createProxy(ResourceWithTemplateAndResolvedLinks.class)
        .getAllLinked()
        .flatMapSingle(ResourceWithSingleState::getProperties)
        .toList()
        .blockingGet();

    assertThat(states).hasSize(numResolvedLinks);
  }

  @Test
  void expand_link_template_if_method_without_template_variable_is_called_but_there_are_no_resolved_links() {

    int numTemplates = 5;
    Observable.range(0, numTemplates).forEach(i -> {
      String template = "/item/" + i + "{?optionalFlag}";
      entryPoint.addLinks(ITEM, new Link(template));

      String uri = UriTemplate.fromTemplate(template).expand();
      mockHalResponseWithNumber(uri, i);
    });

    List<TestResourceState> states = client.createProxy(ResourceWithTemplateAndResolvedLinks.class)
        .getAllLinked()
        .flatMapSingle(ResourceWithSingleState::getProperties)
        .toList()
        .blockingGet();

    assertThat(states).hasSize(numTemplates);
  }

  @Test
  void resolved_links_should_be_ignored_if_method_with_template_variable_is_called() {

    int numResolvedLinks = 5;
    Observable.range(0, numResolvedLinks).forEach(i -> {
      String url = "/item/" + i;
      entryPoint.addLinks(ITEM, new Link(url));

      mockHalResponseWithNumber(url, i);
    });

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    TestResourceState state = client.createProxy(ResourceWithTemplateAndResolvedLinks.class)
        .getLinked(3)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(3);
  }

  @Test
  void resolved_links_should_not_be_followed_if_method_with_template_variable_is_called_but_there_is_no_matching_template() {

    String url = "/item/3";
    entryPoint.addLinks(ITEM, new Link(url));

    mockHalResponseWithNumber(url, 3);

    Throwable ex = catchThrowable(() -> client.createProxy(ResourceWithTemplateAndResolvedLinks.class)
        .getLinked(3)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("No matching link template found with relation item");
  }


  @HalApiInterface
  public interface ResourceWithMissingAnnotations {

    @Related(ITEM)
    LinkableTestResource getItem(String parameter);
  }

  @Test
  void should_throw_developer_exception_if_annotation_for_parameter_is_missing() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithMissingAnnotations.class).getItem("foo"));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("method parameter names have been stripped for ")
        .hasMessageEndingWith("need to be annotated with either @TemplateVariable or @TemplateVariables");
  }

  @Test
  void should_not_throw_developer_exception_if_parameter_names_are_available_instead_of_annotation() {

    entryPoint.addLinks(ITEM, new Link("/items/{id}"));

    // by default parameter names are stripped from class files. This can be avoided with compiler settings, and
    // this would be required if you want to use template methods without using annotations.

    // to unit-test the support for this, we'll need to create a dynamic class that extends ResourceWithMissingAnnotations.
    // but ensures the name of the parameters are available
    Class<? extends ResourceWithMissingAnnotations> resourceClass = createHalApiInterfaceWithPreservedParameterNames();

    ResourceWithMissingAnnotations proxy = client.createProxy(resourceClass);
    LinkableTestResource linkedResource = proxy.getItem("foo");

    Link link = linkedResource.createLink();
    assertThat(link.getHref()).isEqualTo("/items/foo");
  }

  private Class<? extends ResourceWithMissingAnnotations> createHalApiInterfaceWithPreservedParameterNames() {

    AnnotationDescription halApiAnnotation = AnnotationDescription.Builder.ofType(HalApiInterface.class).build();
    AnnotationDescription relatedAnnotation = AnnotationDescription.Builder.ofType(Related.class).define("value", StandardRelations.ITEM).build();

    ByteBuddy bb = new ByteBuddy();

    Class<? extends ResourceWithMissingAnnotations> resourceClass = bb.makeInterface(ResourceWithMissingAnnotations.class)
        .annotateType(halApiAnnotation)
        // override the ResourceWithMissingAnnotations#getItem but this dynamic version will have parameter names present
        .defineMethod("getItem", LinkableTestResource.class, Modifier.PUBLIC)
        .withParameter(String.class, "id")
        .withoutCode()
        .annotateMethod(relatedAnnotation)
        .make()
        .load(getClass().getClassLoader())
        .getLoaded();

    return resourceClass;
  }
}
