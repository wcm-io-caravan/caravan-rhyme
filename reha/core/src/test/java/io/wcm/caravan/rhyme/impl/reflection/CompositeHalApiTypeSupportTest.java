/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.JsonResourceLoader;
import io.wcm.caravan.rhyme.impl.client.HalApiClientImpl;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResponseRendererImpl;
import io.wcm.caravan.ryhme.testing.resources.TestResource;

@ExtendWith(MockitoExtension.class)
public class CompositeHalApiTypeSupportTest {

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();
  private final ExceptionStatusAndLoggingStrategy exceptionStrategy = null;

  @Mock
  private JsonResourceLoader jsonLoader;

  @Mock
  private HalApiReturnTypeSupport mockReturnTypeSupport;

  @Mock
  private HalApiAnnotationSupport mockAnnotationSupport;

  private Method firstMethod = CompositeHalApiTypeSupportTest.class.getMethods()[0];

  private HalApiTypeSupportAdapter assertThatCompositeHasDefaultAndAdapter(CompositeHalApiTypeSupport composite) {

    assertThat(composite.getDelegates()).hasSize(2);
    assertThat(composite.getDelegates().get(0)).isInstanceOf(DefaultHalApiTypeSupport.class);
    assertThat(composite.getDelegates().get(1)).isInstanceOf(HalApiTypeSupportAdapter.class);

    return (HalApiTypeSupportAdapter)composite.getDelegates().get(1);
  }

  private void assertThatMockAnnotationSupportIsEffective(HalApiAnnotationSupport annotationSupport) {

    assertThat(annotationSupport).isInstanceOf(CompositeHalApiTypeSupport.class);
    CompositeHalApiTypeSupport composite = (CompositeHalApiTypeSupport)annotationSupport;

    HalApiTypeSupportAdapter adapter = assertThatCompositeHasDefaultAndAdapter(composite);
    assertThat(adapter.getAnnotationSupport()).isSameAs(mockAnnotationSupport);
  }

  private void assertThatMockReturnTypeSupportIsEffective(HalApiTypeSupport typeSupport) {

    assertThat(typeSupport).isInstanceOf(CompositeHalApiTypeSupport.class);
    CompositeHalApiTypeSupport composite = (CompositeHalApiTypeSupport)typeSupport;

    HalApiTypeSupportAdapter adapter = assertThatCompositeHasDefaultAndAdapter(composite);
    assertThat(adapter.getReturnTypeSupport()).isSameAs(mockReturnTypeSupport);
  }

  @Test
  public void client_should_use_custom_return_types() throws Exception {

    HalApiClient client = HalApiClient.create(jsonLoader, metrics, null, mockReturnTypeSupport);

    assertThatMockReturnTypeSupportIsEffective(((HalApiClientImpl)client).getTypeSupport());
  }

  @Test
  public void client_should_use_custom_annotations() throws Exception {

    HalApiClient client = HalApiClient.create(jsonLoader, metrics, mockAnnotationSupport, null);

    assertThatMockAnnotationSupportIsEffective(((HalApiClientImpl)client).getTypeSupport());
  }

  @Test
  public void resource_renderer_should_use_custom_return_types() throws Exception {

    HalApiTypeSupport typeSupport = DefaultHalApiTypeSupport.extendWith(null, mockReturnTypeSupport);

    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl(metrics, typeSupport);

    assertThatMockReturnTypeSupportIsEffective(renderer.getTypeSupport());
  }

  @Test
  public void resource_renderer_should_use_custom_annotations() throws Exception {

    HalApiTypeSupport typeSupport = DefaultHalApiTypeSupport.extendWith(mockAnnotationSupport, null);

    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl(metrics, typeSupport);

    assertThatMockAnnotationSupportIsEffective(renderer.getTypeSupport());
  }

  @Test
  public void response_renderer_should_use_custom_annotations() throws Exception {

    AsyncHalResponseRenderer renderer = AsyncHalResponseRenderer.create(metrics, exceptionStrategy, mockAnnotationSupport, null);

    assertThatMockAnnotationSupportIsEffective(((AsyncHalResponseRendererImpl)renderer).getAnnotationSupport());
  }

  private void assertThatCompositeReturnsFirstTrueValueOfMock(Function<HalApiAnnotationSupport, Boolean> fut) {

    HalApiTypeSupport composite = createCompositeTypeSupport();

    when(fut.apply(mockAnnotationSupport)).thenReturn(true);
    assertThat(fut.apply(composite)).isTrue();
    fut.apply(verify(mockAnnotationSupport));
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
  public void isHalApiInterface_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isHalApiInterface(TestResource.class));
  }

  @Test
  public void getContentType_should_return_first_non_null_value() throws Exception {

    assertThatCompositeReturnsFirstNonNullValueOfMock(a -> a.getContentType(TestResource.class), "text/json");
  }

  @Test
  public void isRelatedResourceMethod_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isRelatedResourceMethod(firstMethod));
  }

  @Test
  public void getRelation_should_return_first_non_null_value() throws Exception {

    assertThatCompositeReturnsFirstNonNullValueOfMock(a -> a.getRelation(firstMethod), "rel");
  }

  @Test
  public void isResourceLinkMethod_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isResourceLinkMethod(firstMethod));
  }

  @Test
  public void isResourceReoresentationMethod_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isResourceRepresentationMethod(firstMethod));
  }

  @Test
  public void isResourceStateMethod_should_return_first_true_value() throws Exception {

    assertThatCompositeReturnsFirstTrueValueOfMock(a -> a.isResourceStateMethod(firstMethod));
  }

  @Test
  public void convertFromObservable_should_return_first_non_null_value() throws Exception {

    Function<Observable, Stream> fun = o -> ((List)o.toList().blockingGet()).stream();
    assertThatCompositeReturnsFirstNonNullValueOfReturnTypeMock(a -> a.convertFromObservable(Stream.class), fun);
  }

  @Test
  public void convertToObservable_should_return_first_non_null_value() throws Exception {

    @SuppressWarnings("unchecked")
    Function<Object, Observable<?>> fun = s -> Observable.fromStream((Stream)s);
    assertThatCompositeReturnsFirstNonNullValueOfReturnTypeMock(a -> a.convertToObservable(Stream.class), fun);
  }
}
