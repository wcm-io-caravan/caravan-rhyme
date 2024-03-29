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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.ResourceTreeClientTestSupport;
import io.wcm.caravan.rhyme.impl.client.RelatedResourceTest.ResourceWithSingleRelated;
import io.wcm.caravan.rhyme.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.rhyme.testing.resources.TestResource;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
class ToStringTest {

  private final ResourceTreeClientTestSupport client = ClientTestSupport.withResourceTree();

  @Test
  void toString_can_be_called_on_resource_with_self_link() {

    ResourceWithSingleRelated resource = client.createProxy(ResourceWithSingleRelated.class);

    assertThat(resource).hasToString("dynamic client proxy for ResourceWithSingleRelated at /");
  }

  @Test
  void toString_can_be_called_on_linked_resource() {

    TestResource linkedTest = client.getEntryPoint().createLinked(StandardRelations.ITEM);

    ResourceWithSingleRelated entryPoint = client.createProxy(ResourceWithSingleRelated.class);
    ResourceWithSingleState linkedResource = entryPoint.getItem().blockingGet();

    assertThat(linkedResource).hasToString("dynamic client proxy for ResourceWithSingleState at " + linkedTest.getUrl());
  }

  @Test
  void toString_can_be_called_on_embedded_resource() {

    client.getEntryPoint().createEmbedded(StandardRelations.ITEM);

    ResourceWithSingleRelated entryPoint = client.createProxy(ResourceWithSingleRelated.class);
    ResourceWithSingleState embeddedResource = entryPoint.getItem().blockingGet();

    assertThat(embeddedResource).hasToString("dynamic client proxy for ResourceWithSingleState (embedded without self link)");
  }
}
