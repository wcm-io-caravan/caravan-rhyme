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
import static io.wcm.caravan.rhyme.impl.client.ClientTestSupport.ENTRY_POINT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.damnhandy.uri.template.UriTemplate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceLink;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariables;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.ryhme.testing.resources.TestResourceState;


public class TemplateVariablesTest {

  private final MockClientTestSupport client = ClientTestSupport.withMocking();
  private final HalResource entryPoint = new HalResource();

  @BeforeEach
  void setUp() {
    client.mockHalResponse(ENTRY_POINT_URI, entryPoint);
  }

  private void mockHalResponseWithNumberAndText(String url, Integer number, String text) {

    TestResourceState state = new TestResourceState();
    state.number = number;
    state.text = text;

    HalResource resource = new HalResource(state, url);
    client.mockHalResponse(url, resource);
  }

  private void mockHalResponseForTemplateExpandedWithDto(String template, VariablesDto dto) {

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", dto.id);
    map.put("text", dto.text);

    String uri = UriTemplate.expand(template, map);
    mockHalResponseWithNumberAndText(uri, dto.id, dto.text);
  }

  private void mockHalResponseForTemplateExpandedWithInterface(String template, VariablesInterface variables) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", variables.getId());
    map.put("text", variables.getText());

    String uri = UriTemplate.expand(template, map);
    mockHalResponseWithNumberAndText(uri, variables.getId(), variables.getText());
  }

  @HalApiInterface
  interface LinkedResourceWithSingleState {

    @ResourceState
    Single<TestResourceState> getProperties();

    @ResourceLink
    Link createLink();
  }


  public static class VariablesDto {

    public Integer id;
    public String text;
  }

  @HalApiInterface
  interface ResourceWithTemplateVariablesDto {

    @Related(ITEM)
    Single<LinkedResourceWithSingleState> getItem(@TemplateVariables VariablesDto dto);
  }

  @Test
  public void should_expand_template_with_variables_from_dto() throws Exception {

    String template = "/item/{id}{?text*}";
    entryPoint.addLinks(ITEM, new Link(template));

    VariablesDto dto = new VariablesDto();
    dto.id = 123;
    dto.text = "text";

    mockHalResponseForTemplateExpandedWithDto(template, dto);

    TestResourceState linkedState = client.createProxy(ResourceWithTemplateVariablesDto.class)
        .getItem(dto)
        .flatMap(LinkedResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
  }

  @Test
  public void should_expand_template_with_null_field_in_dto() throws Exception {

    String template = "/item/{id}{?text}";
    entryPoint.addLinks(ITEM, new Link(template));

    VariablesDto dto = new VariablesDto();
    dto.id = 123;
    dto.text = null;

    mockHalResponseForTemplateExpandedWithDto(template, dto);

    TestResourceState linkedState = client.createProxy(ResourceWithTemplateVariablesDto.class)
        .getItem(dto)
        .flatMap(LinkedResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
  }

  @Test
  public void should_expand_template_with_only_null_fields_in_dto() throws Exception {

    String template = "/item/{id}{?text}";
    entryPoint.addLinks(ITEM, new Link(template));

    VariablesDto dto = new VariablesDto();
    dto.id = null;
    dto.text = null;

    mockHalResponseForTemplateExpandedWithDto(template, dto);

    TestResourceState linkedState = client.createProxy(ResourceWithTemplateVariablesDto.class)
        .getItem(dto)
        .flatMap(LinkedResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
  }

  @Test
  public void should_not_expand_template_if_null_dto_is_used() throws Exception {

    String template = "/item/{id}{?text}";
    entryPoint.addLinks(ITEM, new Link(template));

    Link link = client.createProxy(ResourceWithTemplateVariablesDto.class)
        .getItem(null)
        .map(LinkedResourceWithSingleState::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo(template);
  }

  @SuppressWarnings("unused")
  @SuppressFBWarnings("UUF_UNUSED_FIELD")
  public static class VariablesDtoWithPrivateFields {

    private Integer id;
    private String text;
  }

  @HalApiInterface
  interface ResourceWithTemplateVariablesDtoWithPrivateFields {

    @Related(ITEM)
    Single<LinkedResourceWithSingleState> getItem(@TemplateVariables VariablesDtoWithPrivateFields dto);
  }

  @Test
  public void should_fail_if_dto_fields_are_private() throws Exception {

    VariablesDtoWithPrivateFields dto = new VariablesDtoWithPrivateFields();

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithTemplateVariablesDtoWithPrivateFields.class).getItem(dto));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageContaining("Make sure that all fields in your classes used as parameters annotated with @TemplateVariables are public")
        .hasCauseInstanceOf(IllegalAccessException.class);
  }

  public interface VariablesInterface {

    Integer getId();

    String getText();
  }

  @HalApiInterface
  interface ResourceWithTemplateVariablesInterface {

    @Related(ITEM)
    Single<LinkedResourceWithSingleState> getItem(@TemplateVariables VariablesInterface dto);
  }

  @Test
  public void should_expand_template_with_variables_interface() throws Exception {

    String template = "/item/{id}{?text*}";
    entryPoint.addLinks(ITEM, new Link(template));

    VariablesInterface variables = new VariablesInterface() {

      @Override
      public Integer getId() {
        return 123;
      }

      @Override
      public String getText() {
        return "text";
      }

    };

    mockHalResponseForTemplateExpandedWithInterface(template, variables);

    TestResourceState linkedState = client.createProxy(ResourceWithTemplateVariablesInterface.class)
        .getItem(variables)
        .flatMap(LinkedResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
  }

  @Test
  public void should_not_expand_template_if_null_interface_is_used() throws Exception {

    String template = "/item/{id}{?text}";
    entryPoint.addLinks(ITEM, new Link(template));

    Link link = client.createProxy(ResourceWithTemplateVariablesInterface.class)
        .getItem(null)
        .map(LinkedResourceWithSingleState::createLink)
        .blockingGet();

    assertThat(link.getHref()).isEqualTo(template);
  }

  @Test
  public void should_fail_if_calling_interface_method_throws_exception() throws Exception {

    VariablesInterface variables = Mockito.mock(VariablesInterface.class);
    Mockito.when(variables.getId()).thenThrow(new IllegalArgumentException());

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithTemplateVariablesInterface.class).getItem(variables));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to extract template variables")
        .hasCauseInstanceOf(InvocationTargetException.class);
  }

  @Test
  public void should_fail_if_non_null_variable_is_not_present_in_template() throws Exception {

    String template = "/item/{id}";
    entryPoint.addLinks(ITEM, new Link(template));

    VariablesDto dto = new VariablesDto();
    dto.id = 123;
    dto.text = "text";

    mockHalResponseForTemplateExpandedWithDto(template, dto);

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithTemplateVariablesDto.class)
            .getItem(dto)
            .flatMap(LinkedResourceWithSingleState::getProperties)
            .blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("No matching link template found with relation item which contains the variables [id, text]");
  }

  @Test
  public void should_not_fail_if_null_variable_is_not_present_in_template() throws Exception {

    String template = "/item/{id}";
    entryPoint.addLinks(ITEM, new Link(template));

    VariablesDto dto = new VariablesDto();
    dto.id = 123;
    dto.text = null;

    mockHalResponseForTemplateExpandedWithDto(template, dto);

    TestResourceState linkedState = client.createProxy(ResourceWithTemplateVariablesDto.class)
        .getItem(dto)
        .flatMap(LinkedResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
  }
}
