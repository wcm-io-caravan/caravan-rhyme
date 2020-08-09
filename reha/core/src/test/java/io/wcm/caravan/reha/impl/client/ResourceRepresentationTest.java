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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.ResourceRepresentation;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.api.relations.StandardRelations;
import io.wcm.caravan.reha.impl.client.ClientTestSupport.ResourceTreeClientTestSupport;
import io.wcm.caravan.reha.testing.resources.TestResource;

public class ResourceRepresentationTest {

  private final ResourceTreeClientTestSupport client = ClientTestSupport.withResourceTree();
  private final TestResource entryPoint = client.getEntryPoint();

  @BeforeEach
  public void setUp() {
    entryPoint.setText("test");
    entryPoint.setFlag(true);
    entryPoint.createLinked(ITEM);
    entryPoint.createEmbedded(StandardRelations.COLLECTION).createEmbedded(ITEM);
  }

  @HalApiInterface
  interface ResourceWithRepresentations {

    @ResourceRepresentation
    Single<HalResource> asHalResource();

    @ResourceRepresentation
    Single<ObjectNode> asObjectNode();

    @ResourceRepresentation
    Single<JsonNode> asJsonNode();

    @ResourceRepresentation
    Single<String> asString();
  }

  @Test
  public void representation_should_be_available_as_hal_resource() {

    HalResource hal = client.createProxy(ResourceWithRepresentations.class)
        .asHalResource()
        .blockingGet();

    assertThat(hal.getModel()).isEqualTo(entryPoint.getJson());
  }

  @Test
  public void representation_should_be_available_as_object_node() {

    ObjectNode json = client.createProxy(ResourceWithRepresentations.class)
        .asObjectNode()
        .blockingGet();

    assertThat(json).isEqualTo(entryPoint.getJson());
  }

  @Test
  public void representation_should_be_available_as_json_node() {

    JsonNode json = client.createProxy(ResourceWithRepresentations.class)
        .asJsonNode()
        .blockingGet();

    assertThat(json).isEqualTo(entryPoint.getJson());
  }

  @Test
  public void representation_should_be_available_as_string() {

    String string = client.createProxy(ResourceWithRepresentations.class)
        .asString()
        .blockingGet();

    assertThat(string).isEqualTo(entryPoint.getJson().toString());
  }

  @HalApiInterface
  interface ResourceWithUnsupportedRepresentations {

    @ResourceRepresentation
    Single<Document> asXmlDocument();
  }

  @Test
  public void should_throw_developer_exception_if_emission_type_is_not_supported() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithUnsupportedRepresentations.class).asXmlDocument().blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageEndingWith("annotated with @ResourceRepresentation must return a reactive type emitting either HalResource, JsonNode, String");
  }
}
