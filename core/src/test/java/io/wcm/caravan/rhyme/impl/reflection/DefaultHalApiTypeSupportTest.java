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
package io.wcm.caravan.rhyme.impl.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;


public class DefaultHalApiTypeSupportTest {

  private DefaultHalApiTypeSupport typeSupport = new DefaultHalApiTypeSupport();

  @Test
  void isProviderOfOptionalValue_should_return_true_for_Optional() throws Exception {

    assertThat(typeSupport.isProviderOfOptionalValue(Optional.class))
        .isTrue();
  }

  @Test
  void isProviderOfOptionalValue_should_return_true_for_Maybe() throws Exception {

    assertThat(typeSupport.isProviderOfOptionalValue(Maybe.class))
        .isTrue();
  }

  @Test
  void isProviderOfOptionalValue_should_return_false_for_Single() throws Exception {

    assertThat(typeSupport.isProviderOfOptionalValue(Single.class))
        .isFalse();
  }

}
