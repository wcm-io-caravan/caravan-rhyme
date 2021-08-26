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

import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.aem.impl.adaptation.RhymeObjects;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;


public class RhymeObjectsTest {

  private static final String FIELD_NAME = "intProperty";

  @Test
  public void testWriteFieldUnchecked_can_set_value_of_private_field() throws Exception {

    ClassWithIntProperty instance = new ClassWithIntProperty();

    RhymeObjects.writeFieldUnchecked(instance, ClassWithIntProperty.class.getDeclaredField(FIELD_NAME), 123);

    Assertions.assertThat(instance.intProperty).isEqualTo(123);
  }

  @Test
  public void testWriteFieldUnchecked_fails_for_incompatible_type() throws Exception {

    ClassWithIntProperty instance = new ClassWithIntProperty();

    Throwable ex = catchThrowable(() -> RhymeObjects.writeFieldUnchecked(instance, ClassWithIntProperty.class.getDeclaredField(FIELD_NAME), "foo"));

    Assertions.assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to inject instance of java.lang.String")
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  static class ClassWithIntProperty {

    private int intProperty;
  }

}
