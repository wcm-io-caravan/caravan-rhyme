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
package io.wcm.caravan.rhyme.aem.impl.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.RhymeResourceRegistration;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.impl.resources.AemApiDiscoveryResourceImpl;
import io.wcm.caravan.rhyme.aem.testing.models.SelectorSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceRegistration;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
@ExtendWith(MockitoExtension.class)
public class AemApiDiscoveryResourceImplTest {

  private AemContext context = AppAemContext.newAemContext();

  @Mock
  private RhymeResourceRegistration mockRegistration;

  @Mock
  private LinkableResource mockResource;

  private AemApiDiscoveryResourceImpl resource;

  @BeforeEach
  void setUp() {

    context.registerService(RhymeResourceRegistration.class, new TestResourceRegistration());
    context.registerService(RhymeResourceRegistration.class, mockRegistration);

    SlingRhyme rhyme = AppAemContext.createRhymeInstance(context, "/content");

    resource = rhyme.adaptResource(context.request().getResource(), AemApiDiscoveryResourceImpl.class);
  }

  @Test
  void createLink_should_not_fail() {

    Link link = resource.createLink();

    assertThat(link)
        .isNotNull()
        .returns("/content.rhyme", Link::getHref);
  }

  @Test
  void getAllEntryPoints_should_return_single_entrypoint_from_testResourceSelectorProvider() {

    List<LinkableResource> entryPoints = resource.getApiEntryPoints();

    assertThat(entryPoints)
        .hasSize(1)
        .first()
        .isInstanceOf(SelectorSlingTestResource.class);
  }

  @Test
  void getAllEntryPoints_should_include_second_entrypoint_from_Mock() {

    Mockito.when(mockRegistration.getApiEntryPoint(ArgumentMatchers.any()))
        .thenAnswer(invocation -> Optional.of(new TestEntryPointResource()));

    List<LinkableResource> entryPoints = resource.getApiEntryPoints();

    assertThat(entryPoints)
        .hasSize(2)
        .extracting(LinkableResource::createLink)
        .extracting(Link::getHref)
        .containsExactlyInAnyOrder("/foo", "/.selectortest.rhyme");
  }

  class TestEntryPointResource implements LinkableResource {

    @Override
    public Link createLink() {
      return new Link("/foo");
    }

  }
}
