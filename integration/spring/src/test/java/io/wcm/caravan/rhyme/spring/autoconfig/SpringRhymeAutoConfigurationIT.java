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
package io.wcm.caravan.rhyme.spring.autoconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

@SpringBootTest
@ContextConfiguration
@EnableAutoConfiguration
public class SpringRhymeAutoConfigurationIT {

  @Autowired
  private HalResourceLoader resourceLoader;

  @Autowired
  private HalResourceLoaderBuilder resourceLoaderBuilder;

  @Test
  void default_resource_loader_is_available() {

    assertThat(resourceLoader)
        .isNotNull();
  }

  @Test
  void resource_loader_builder_can_build() {

    assertThat(resourceLoaderBuilder)
        .isNotNull();

    resourceLoaderBuilder.build();
  }

  @Configuration
  static class EmptyConfiguration {
    // we want to verify that SpringRhymeAutoConfiguration kicks and provides a HalResourceLoader bean without
    // any other configuration present in this class
  }

}
