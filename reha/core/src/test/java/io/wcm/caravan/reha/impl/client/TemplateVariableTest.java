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
package io.wcm.caravan.reha.impl.client;

import static io.wcm.caravan.reha.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.reha.impl.client.ClientTestSupport.ENTRY_POINT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.damnhandy.uri.template.UriTemplate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.Related;
import io.wcm.caravan.reha.api.annotations.TemplateVariable;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.reha.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.reha.testing.resources.TestResourceState;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
public class TemplateVariableTest {

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
  public void link_template_should_be_expanded_if_only_parameter_is_given() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    mockHalResponseWithNumber("/item/1", 1);

    TestResourceState state = client.createProxy(ResourceWithSimpleLinkTemplate.class)
        .getLinked(1)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(1);
  }

  @Test
  public void link_template_should_not_be_expanded_if_only_parameter_is_missing() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}"));

    mockHalResponseWithNumber("/item/", 0);

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithSimpleLinkTemplate.class)
            .getLinked(null)
            .flatMap(ResourceWithSingleState::getProperties)
            .blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Cannot follow the link template to /item/{number} because it has not been expanded");

  }

  @HalApiInterface
  interface ResourceWithComplexLinkTemplate {

    @Related(ITEM)
    Single<ResourceWithSingleState> getLinked(
        @TemplateVariable("number") Integer number,
        @TemplateVariable("optionalFlag") Boolean optionalFlag);
  }

  @Test
  public void link_template_should_be_expanded_if_one_of_multiple_parameters_is_missing() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}{?optionalFlag}"));

    mockHalResponseWithNumber("/item/1", 1);

    TestResourceState state = client.createProxy(ResourceWithComplexLinkTemplate.class)
        .getLinked(1, null)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(1);
  }

  @Test
  public void link_template_should_be_expanded_if_all_of_multiple_parameters_are_present() {

    entryPoint.addLinks(ITEM, new Link("/item/{number}{?optionalFlag}"));

    mockHalResponseWithNumber("/item/1?optionalFlag=true", 1);

    TestResourceState state = client.createProxy(ResourceWithComplexLinkTemplate.class)
        .getLinked(1, true)
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(state.number).isEqualTo(1);
  }

  @HalApiInterface
  interface ResourceWithTemplateAndResolvedLinks {

    @Related(ITEM)
    Single<ResourceWithSingleState> getLinked(@TemplateVariable("number") Integer number);

    @Related(ITEM)
    Observable<ResourceWithSingleState> getAllLinked();
  }

  @Test
  public void ignore_link_template_if_method_without_template_variable_is_called_and_there_are_resolved_links() {

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
  public void expand_link_template_if_method_without_template_variable_is_called_but_there_are_no_resolved_links() {

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
  public void resolved_links_should_be_ignored_if_method_with_template_variable_is_called() {

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
  public void resolved_links_should_not_be_followed_if_method_with_template_variable_is_called_but_there_is_no_matching_template() {

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
  interface ResourceWithMissingAnnotations {

    @Related(ITEM)
    Single<ResourceWithSingleState> getItem(String parameter);
  }

  @Test
  public void should_throw_developer_exception_if_annotation_for_parameter_is_missing() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithMissingAnnotations.class).getItem("foo"));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("all parameters ").hasMessageEndingWith("need to be either annotated with @TemplateVariable or @TemplateVariables");
  }
}
