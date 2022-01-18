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
package io.wcm.caravan.rhyme.aem.impl.parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.parameters.QueryParam;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

public class QueryParamCollectorTest {

  private final QueryParamCollector collector = new QueryParamCollector();

  @Test
  void should_find_all_fields_and_handle_null_values()  {

    ModelWithQueryParams model = new ModelWithQueryParams();

    Map<String, Object> params = collector.getQueryParameters(model);

    assertThat(params.keySet())
        .containsExactlyInAnyOrder("foo", "bar");

    assertThat(params.values())
        .containsOnly((Object)null);
  }

  @Test
  void should_find_all_fields_and_return_values()  {

    ModelWithQueryParams model = new ModelWithQueryParams();
    model.foo = "foo";
    model.bar = ImmutableList.of(123, 456);

    Map<String, Object> params = collector.getQueryParameters(model);

    assertThat(params.keySet())
        .containsExactlyInAnyOrder("foo", "bar");

    assertThat(params.get("foo"))
        .isEqualTo("foo");

    assertThat(params.get("bar"))
        .isInstanceOf(List.class).isEqualTo(model.bar);
  }

  @Model(adaptables = SlingRhyme.class)
  private static class ModelWithQueryParams {

    @QueryParam
    private String foo;

    @QueryParam
    private List<Integer> bar;
  }

  @Test
  void should_find_all_params_in_nested_models()  {

    ModelWithNestedParamModel model = new ModelWithNestedParamModel();
    model.params = new ModelWithQueryParams();
    model.params.foo = "foo";
    model.params.bar = ImmutableList.of(123, 456);

    Map<String, Object> params = collector.getQueryParameters(model);

    assertThat(params.keySet()).containsExactlyInAnyOrder("foo", "bar");

    assertThat(params.get("foo"))
        .isEqualTo("foo");

    assertThat(params.get("bar"))
        .isInstanceOf(List.class)
        .isEqualTo(model.params.bar);
  }

  @Model(adaptables = SlingRhyme.class)
  static class ModelWithNestedParamModel {

    @Self
    private ModelWithQueryParams params;

  }

  @Test
  void readField_should_handle_exceptions()  {

    Field field = FieldUtils.getDeclaredField(ModelWithQueryParams.class, "foo", true);

    Throwable ex = catchThrowable(() -> QueryParamCollector.readField(this, field));

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to access field ")
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }
}
