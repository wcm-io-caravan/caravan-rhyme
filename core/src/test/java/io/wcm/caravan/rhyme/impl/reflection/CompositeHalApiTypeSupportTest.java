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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalApiClientBuilder;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.server.HalResponseRendererBuilder;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.HalApiClientImpl;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResponseRendererImpl;
import io.wcm.caravan.rhyme.testing.resources.TestResource;

@ExtendWith(MockitoExtension.class)
public class CompositeHalApiTypeSupportTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();

  @Mock
  private HalResourceLoader jsonLoader;

  @Mock
  private HalApiReturnTypeSupport mockReturnTypeSupport;

  @Mock
  private HalApiAnnotationSupport mockAnnotationSupport;

  private Method firstMethod = CompositeHalApiTypeSupportTest.class.getMethods()[0];

  @HalApiInterface
  interface TestInterface {

    String getString();
  }

  private void assertThatMockAnnotationSupportIsEffective(HalApiAnnotationSupport annotationSupport) throws Exception {

    Method method = TestInterface.class.getMethod("getString");

    when(mockAnnotationSupport.isRelatedResourceMethod(method))
        .thenReturn(true);

    assertThat(annotationSupport.isRelatedResourceMethod(method))
        .isTrue();

    verify(mockAnnotationSupport).isRelatedResourceMethod(method);
  }

  private void assertThatMockReturnTypeSupportIsEffective(HalApiReturnTypeSupport typeSupport) {

    Class type = Iterator.class;

    when(mockReturnTypeSupport.isProviderOfMultiplerValues(type))
        .thenReturn(true);

    assertThat(typeSupport.isProviderOfMultiplerValues(type))
        .isTrue();

    verify(mockReturnTypeSupport).isProviderOfMultiplerValues(type);
  }

  @Test
  void client_should_use_custom_return_types() throws Exception {

    HalApiClient client = HalApiClientBuilder.create().withReturnTypeSupport(mockReturnTypeSupport).build();

    assertThatMockReturnTypeSupportIsEffective(((HalApiClientImpl)client).getTypeSupport());
  }

  @Test
  void client_should_use_custom_annotations() throws Exception {

    HalApiClient client = HalApiClientBuilder.create().withAnnotationTypeSupport(mockAnnotationSupport).build();

    assertThatMockAnnotationSupportIsEffective(((HalApiClientImpl)client).getTypeSupport());
  }

  @Test
  void resource_renderer_should_use_custom_return_types() throws Exception {

    HalApiTypeSupport typeSupport = DefaultHalApiTypeSupport.extendWith(null, mockReturnTypeSupport);

    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl(metrics, typeSupport, OBJECT_MAPPER);

    assertThatMockReturnTypeSupportIsEffective(renderer.getTypeSupport());
  }

  @Test
  void resource_renderer_should_use_custom_annotations() throws Exception {

    HalApiTypeSupport typeSupport = DefaultHalApiTypeSupport.extendWith(mockAnnotationSupport, null);

    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl(metrics, typeSupport, OBJECT_MAPPER);

    assertThatMockAnnotationSupportIsEffective(renderer.getTypeSupport());
  }

  @Test
  void response_renderer_should_use_custom_annotations() throws Exception {

    AsyncHalResponseRenderer renderer = HalResponseRendererBuilder.create().withAnnotationTypeSupport(mockAnnotationSupport).build();

    assertThatMockAnnotationSupportIsEffective(((AsyncHalResponseRendererImpl)renderer).getAnnotationSupport());
  }

  private void assertThatCompositeReturnsFirstTrueValueOfMock(Function<HalApiAnnotationSupport, Boolean> fut) {

    HalApiTypeSupport composite = createCompositeTypeSupport();

    when(fut.apply(mockAnnotationSupport)).thenReturn(true);
    assertThat(fut.apply(composite)).isTrue();
    fut.apply(verify(mockAnnotationSupport));
  }

  private void assertThatCompositeReturnsFirstTrueValueOfReturnTypeMock(Function<HalApiReturnTypeSupport, Boolean> fut) {

    HalApiTypeSupport composite = createCompositeTypeSupport();

    when(fut.apply(mockReturnTypeSupport)).thenReturn(true);
    assertThat(fut.apply(composite)).isTrue();
    fut.apply(verify(mockReturnTypeSupport));
  }

  private void assertThatCompositeReturnsFirstNonNullValueOfMock(Function<HalApiAnnotationSupport, Object> fut, Object returnValue) {

    HalApiTypeSupport composite = createCompositeTypeSupport();

    when(fut.apply(mockAnnotationSupport)).thenReturn(returnValue);
    assertThat(fut.apply(composite)).isNotNull();
    fut.apply(verify(mockAnnotationSupport));
  }

  private <T, U> void assertThatCompositeReturnsFirstNonNullValueOfReturnTypeMock(Function<HalApiReturnTypeSupport, Function<T, U>> fut,
      Function<T, U> returnValue) {

    HalApiTypeSupport composite = createCompositeTypeSupport();

    when(fut.apply(mockReturnTypeSupport)).thenReturn(returnValue);
    assertThat(fut.apply(composite)).isNotNull();
    fut.apply(verify(mockReturnTypeSupport));
  }

  private HalApiTypeSupport createCompositeTypeSupport() {
    HalApiTypeSupport composite = new CompositeHalApiTypeSupport(ImmutableList.of(
        new DefaultHalApiTypeSupport(),
        new HalApiTypeSupportAdapter(null, null),
        new HalApiTypeSupportAdapter(mockAnnotationSupport),
        new HalApiTypeSupportAdapter(mockReturnTypeSupport)));
    return composite;
  }

  @Test
  void isHalApiInterface_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isHalApiInterface(TestResource.class));
  }

  @Test
  void getContentType_should_return_first_non_null_value() throws Exception {

    assertThatCompositeReturnsFirstNonNullValueOfMock(a -> a.getContentType(TestResource.class), "text/json");
  }

  @Test
  void isRelatedResourceMethod_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isRelatedResourceMethod(firstMethod));
  }

  @Test
  void getRelation_should_return_first_non_null_value() throws Exception {

    assertThatCompositeReturnsFirstNonNullValueOfMock(a -> a.getRelation(firstMethod), "rel");
  }

  @Test
  void isResourceLinkMethod_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isResourceLinkMethod(firstMethod));
  }

  @Test
  void isResourceReoresentationMethod_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isResourceRepresentationMethod(firstMethod));
  }

  @Test
  void isResourceStateMethod_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isResourceStateMethod(firstMethod));
  }

  @Test
  void isResourcePropertyMethod_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isResourcePropertyMethod(firstMethod));
  }

  @Test
  void getPropertyName_should_return_first_non_null_value() throws Exception {

    assertThatCompositeReturnsFirstNonNullValueOfMock(a -> a.getPropertyName(firstMethod), "prop");
  }


  @Test
  void convertFromObservable_should_return_first_non_null_value() throws Exception {

    Function<Observable, Iterator> fun = o -> ((List)o.toList().blockingGet()).iterator();
    assertThatCompositeReturnsFirstNonNullValueOfReturnTypeMock(a -> a.convertFromObservable(Iterator.class), fun);
  }

  @Test
  void convertToObservable_should_return_first_non_null_value() throws Exception {

    @SuppressWarnings("unchecked")
    Function<Object, Observable<?>> fun = o -> Observable.fromIterable(() -> ((List)o).iterator());
    assertThatCompositeReturnsFirstNonNullValueOfReturnTypeMock(a -> a.convertToObservable(Iterator.class), fun);
  }

  @Test
  void isProviderOfMultiplerValues_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfReturnTypeMock(a -> a.isProviderOfMultiplerValues(Set.class));
  }

  @Test
  void isProviderOptionalValues_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfReturnTypeMock(a -> a.isProviderOfOptionalValue(Set.class));
  }

}
