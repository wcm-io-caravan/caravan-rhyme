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
package io.wcm.caravan.rhyme.impl.client;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceLink;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.ResourceTreeClientTestSupport;
import io.wcm.caravan.ryhme.testing.TestState;
import io.wcm.caravan.ryhme.testing.resources.TestResource;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
public class ResourceLinkTest {

  private final ResourceTreeClientTestSupport client = ClientTestSupport.withResourceTree();
  private final TestResource entryPoint = client.getEntryPoint();

  @HalApiInterface
  interface LinkTargetResource {

    @ResourceState
    Single<TestState> getState();

    @ResourceLink
    Link createLink();
  }

  @Test
  public void link_should_be_extracted_from_entry_point() {

    Link link = client.createProxy(LinkTargetResource.class)
        .createLink();

    assertThat(link.getHref()).isEqualTo(entryPoint.getUrl());
  }


  @HalApiInterface
  interface ResourceWithSingleLinked {

    @Related(ITEM)
    Single<LinkTargetResource> getLinked();
  }

  @Test
  public void link_should_be_extracted_from_single_linked_resource() {

    TestResource itemResource = entryPoint.createLinked(ITEM);

    Link link = client.createProxy(ResourceWithSingleLinked.class)
        .getLinked()
        .map(LinkTargetResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo(itemResource.getUrl());
  }

  @Test
  public void original_referencing_link_name_should_be_used() {

    String linkName = "linkName";

    entryPoint.createLinked(ITEM, linkName);

    Link link = client.createProxy(ResourceWithSingleLinked.class)
        .getLinked()
        .map(LinkTargetResource::createLink)
        .blockingGet();

    assertThat(link.getName()).isEqualTo(linkName);
  }


  @HalApiInterface
  interface ResourceWithMultipleLinked {

    @Related(ITEM)
    Observable<LinkTargetResource> getLinked();
  }

  @Test
  public void filtering_linked_resources_by_name_should_be_possible() {

    Observable.range(0, 10).forEach(i -> entryPoint.createLinked(ITEM, Integer.toString(i)).setNumber(i));

    TestState filteredState = client.createProxy(ResourceWithMultipleLinked.class)
        .getLinked()
        .filter(resource -> StringUtils.equals(resource.createLink().getName(), "5"))
        .singleElement()
        .flatMapSingle(LinkTargetResource::getState)
        .blockingGet();

    assertThat(filteredState.number).isEqualTo(5);
  }

  @Test
  public void filtering_linked_resources_by_name_should_still_use_embedded_resources() {

    Observable.range(0, 10).forEach(i -> entryPoint.createLinked(ITEM, Integer.toString(i)));

    // get one of the link that was created
    Link link = entryPoint.asHalResource().getLinks(ITEM).get(5);

    // and create an embedded resource with a self link of the same URL
    HalResource embedded = new HalResource().setLink(link);
    embedded.getModel().put("string", "foo");
    entryPoint.asHalResource().addEmbedded(ITEM, embedded);

    // then check that filtering the resource with link name....
    TestState filteredState = client.createProxy(ResourceWithMultipleLinked.class)
        .getLinked()
        .filter(resource -> link.getName().equals(resource.createLink().getName()))
        .singleElement()
        .flatMapSingle(r -> r.getState())
        .blockingGet();

    // will actually return the embedded resource (because the string value wasn't added to the linked resource)
    assertThat(filteredState.string).isEqualTo("foo");
  }

  @Test
  public void links_with_different_names_to_same_resource_should_create_different_proxies() {

    TestResource foo = entryPoint.createLinked(ITEM, "foo");

    entryPoint.addLinkTo(ITEM, foo).setName("bar");

    List<LinkTargetResource> resources = client.createProxy(ResourceWithMultipleLinked.class)
        .getLinked()
        .toList()
        .blockingGet();

    assertThat(resources).hasSize(2);

    LinkTargetResource fooResource = resources.get(0);
    LinkTargetResource barResource = resources.get(1);
    assertThat(fooResource).isNotSameAs(barResource);

    assertThat(fooResource.createLink().getName()).isEqualTo("foo");
    assertThat(barResource.createLink().getName()).isEqualTo("bar");
  }

  @HalApiInterface
  interface ResourceWithSingleEmbedded {

    @Related(ITEM)
    Single<LinkTargetResource> getEmbedded();
  }

  @Test
  public void self_link_should_be_extracted_from_embedded_resource_if_its_not_explicitly_linked() {

    TestResource itemResource = entryPoint.createEmbedded(ITEM);
    itemResource.asHalResource().setLink(new Link("/embedded/self-link"));

    Link link = client.createProxy(ResourceWithSingleEmbedded.class)
        .getEmbedded()
        .map(LinkTargetResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo(itemResource.getUrl());
  }

  @Test
  public void named_link_should_be_extracted_for_embedded_resource_if_it_is_explicitly_linked() {

    String linkName = "linkName";
    TestResource itemResource = entryPoint.createLinked(ITEM, linkName);
    entryPoint.asHalResource().addEmbedded(ITEM, new HalResource(itemResource.getUrl()));

    Link link = client.createProxy(ResourceWithSingleEmbedded.class)
        .getEmbedded()
        .map(LinkTargetResource::createLink)
        .blockingGet();

    assertThat(link.getName()).isEqualTo(linkName);
  }

  @Test
  public void should_use_the_first_link_name_if_multiple_links_are_pointing_to_the_same_embedded() {

    TestResource embedded = entryPoint.createEmbedded(ITEM);
    embedded.asHalResource().setLink(new Link("/embedded"));

    Observable.range(0, 3).forEach(i -> {
      String linkName = Integer.toString(i);
      entryPoint.asHalResource().addLinks(ITEM, new Link(embedded.getUrl()).setName(linkName));
    });

    Link link = client.createProxy(ResourceWithSingleEmbedded.class)
        .getEmbedded()
        .map(LinkTargetResource::createLink)
        .blockingGet();

    assertThat(link.getName()).isEqualTo("0");
  }

  @Test
  public void link_with_empty_href_should_be_extracted_from_embedded_resource_without_self_link() {

    entryPoint.createEmbedded(ITEM);

    Link link = client.createProxy(ResourceWithSingleEmbedded.class)
        .getEmbedded()
        .map(LinkTargetResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEmpty();
  }


  @HalApiInterface
  interface ResourceWithLinkTemplate {

    @Related(ITEM)
    Single<LinkTargetResource> getLinked(
        @TemplateVariable("intParam") Integer intParam,
        @TemplateVariable("stringParam") String stringParam,
        @TemplateVariable("listParam") List<String> listParam);
  }

  @Test
  public void link_template_should_be_fully_expanded_if_at_least_one_parameter_is_null() {

    String uriTemplate = "/test{?intParam,stringParam,listParam*}";
    entryPoint.asHalResource().addLinks(ITEM, new Link(uriTemplate));

    Link link = client.createProxy(ResourceWithLinkTemplate.class)
        .getLinked(5, null, null)
        .map(LinkTargetResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo("/test?intParam=5");
  }

  @Test
  public void link_template_should_not_be_expanded_if_all_parameters_are_null() {

    String uriTemplate = "/test{?intParam,stringParam,listParam*}";
    entryPoint.asHalResource().addLinks(ITEM, new Link(uriTemplate));

    Link link = client.createProxy(ResourceWithLinkTemplate.class)
        .getLinked(null, null, null)
        .map(LinkTargetResource::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo(uriTemplate);
  }


  @HalApiInterface
  interface ResourceWithUri {

    @ResourceLink
    String getUri();
  }

  @Test
  public void link_uri_should_be_accessible_as_string() {

    String uri = client.createProxy(ResourceWithUri.class)
        .getUri();

    assertThat(uri).isEqualTo(entryPoint.getUrl());
  }

  @HalApiInterface
  interface EntyPointWithEmbedded {

    @Related(ITEM)
    Single<ResourceWithUri> getEmbedded();
  }

  @Test
  public void link_uri_should_be_empty_if_resource_is_embedded() {

    entryPoint.createEmbedded(ITEM);

    String uri = client.createProxy(EntyPointWithEmbedded.class)
        .getEmbedded()
        .map(ResourceWithUri::getUri)
        .blockingGet();

    assertThat(uri).isEmpty();
  }

  @HalApiInterface
  interface ResourceWithUnsupportedType {

    @ResourceLink
    URI getLink();
  }

  @Test
  public void unsupported_return_types_should_throw_developer_exception() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithUnsupportedType.class).getLink());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageEndingWith("annotated with @ResourceLink must return either a String or io.wcm.caravan.hal.resource.Link");
  }
}
