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

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.parameters.QueryParam;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class QueryParamInjectorTest {

  private AemContext context = AppAemContext.newAemContext();

  private SlingRhyme createRhymeInstance(String queryString) {

    Resource content = context.create().resource("/content");
    context.request().setQueryString(queryString);

    return AppAemContext.createRhymeInstance(context, content.getPath());
  }

  private <T> T createModelForRequestWithQuery(String queryString, Class<T> modelClass) {

    SlingRhyme rhyme = createRhymeInstance(queryString);

    return rhyme.adaptResource(context.currentResource(), modelClass);
  }

  @Test
  public void should_handle_empty_query_string() {

    String queryString = "";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.intParam).isNull();
  }

  @Test
  public void should_handle_valid_integer() {

    String queryString = "intParam=1";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.intParam).isEqualTo(1);
  }

  @Test
  public void should_handle_valid_String() {

    String queryString = "stringParam=foo";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.stringParam).isEqualTo("foo");
  }

  @Test
  public void should_handle_valid_enum() {

    String queryString = "enumParam=FOO";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.enumParam).isEqualTo(FooBarEnum.FOO);
  }

  @Test
  public void should_handle_missing_enum() {

    String queryString = "";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.enumParam).isNull();
  }

  @Test
  public void should_handle_invalid_enum() {

    String queryString = "enumParam=WHATEVER";

    Throwable ex = Assertions.catchThrowable(() -> createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class));

    assertThat(ex)
        .isInstanceOf(HalApiServerException.class)
        .hasMessageStartingWith("Invalid value 'WHATEVER' for parameter 'enumParam'");
  }

  @Test
  public void should_handle_invalid_value() {

    String queryString = "intParam=bar";

    Throwable ex = Assertions.catchThrowable(() -> createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class));

    assertThat(ex)
        .isInstanceOf(HalApiServerException.class)
        .hasMessageStartingWith("Invalid value 'bar' for parameter 'intParam'");
  }

  @Test
  public void should_handle_missing_integer_value() {

    String queryString = "intParam";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.intParam).isNull();
  }

  @Test
  public void should_handle_missing_string_value() {

    String queryString = "stringParam";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.stringParam).isNull();
  }

  @Test
  public void should_handle_empty_string_value() {

    String queryString = "stringParam=";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.stringParam).isNull();
  }

  @Test
  public void should_respect_name_attribute() {

    String queryString = "foo=123";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.renamedParam).isEqualTo(123.0);
  }

  @Test
  public void should_use_first_of_multiple_values() {

    String queryString = "intParam=1&intParam=2";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.intParam).isEqualTo(1);
  }

  @Test
  public void should_handle_lists_with_one_entry() {

    String queryString = "intList=1";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.intList)
        .hasSize(1)
        .first().isEqualTo(1);
  }

  @Test
  public void should_handle_lists_with_multiple_entries() {

    String queryString = "intList=1&intList=2&intList=3";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.intList)
        .hasSize(3)
        .containsExactly(1, 2, 3);
  }

  @Test
  public void should_handle_lists_with_no_values() {

    String queryString = "";

    ModelWithOptionalParams model = createModelForRequestWithQuery(queryString, ModelWithOptionalParams.class);

    assertThat(model.intList)
        .isEmpty();
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithOptionalParams {

    @QueryParam()
    private Integer intParam;

    @QueryParam
    private String stringParam;

    @QueryParam
    private Boolean boolParam;

    @QueryParam
    private List<Integer> intList;

    @QueryParam(name = "foo")
    private Double renamedParam;

    @QueryParam
    private FooBarEnum enumParam;
  }

  public enum FooBarEnum {
    FOO, BAR
  }

}
