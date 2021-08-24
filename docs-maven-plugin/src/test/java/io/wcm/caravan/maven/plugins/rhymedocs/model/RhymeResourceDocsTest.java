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
package io.wcm.caravan.maven.plugins.rhymedocs.model;

import static io.wcm.caravan.maven.plugins.rhymedocs.model.RhymeApiDocsTest.getDocsFor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestEntryPoint;

public class RhymeResourceDocsTest {

  @Test
  public void getTitle_should_return_short_name() {

    RhymeResourceDocs entryPointDocs = getDocsFor(RhymeDocTestEntryPoint.class);

    assertThat(entryPointDocs.getTitle())
        .isEqualTo(RhymeDocTestEntryPoint.class.getSimpleName());
  }

  @Test
  public void getDescription_should_return_javadoc_comment() {

    RhymeResourceDocs entryPointDocs = getDocsFor(RhymeDocTestEntryPoint.class);

    assertThat(entryPointDocs.getDescription())
        .isEqualTo("Javadoc for entrypoint interface");
  }

  @Test
  public void getFullyQualifiedClassName_should_return_you_guess_what() {

    RhymeResourceDocs entryPointDocs = getDocsFor(RhymeDocTestEntryPoint.class);

    assertThat(entryPointDocs.getCanonicalClassName())
        .isEqualTo(RhymeDocTestEntryPoint.class.getName());
  }

}
