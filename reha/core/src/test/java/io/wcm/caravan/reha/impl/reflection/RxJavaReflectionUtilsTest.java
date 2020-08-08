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
package io.wcm.caravan.reha.impl.reflection;

import static io.wcm.caravan.reha.testing.TestRelations.LINKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.client.HalApiDeveloperException;
import io.wcm.caravan.reha.api.common.HalApiReturnTypeSupport;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.api.server.HalApiServerException;

/**
 * contains only some tests for edge case exception handling that is hard to reproduce with realistic client / renderer
 * tests
 */
public class RxJavaReflectionUtilsTest {

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();
  private final HalApiReturnTypeSupport typeSupport = new DefaultHalApiTypeSupport();

  @HalApiInterface
  public interface TestResourceWithCheckedException {

    @RelatedResource(relation = LINKED)
    LinkableResource getExternal() throws IOException;
  }

  @Test
  public void throwing_checked_exceptions_should_be_handled() {

    TestResourceWithCheckedExceptionImpl resourceImpl = new TestResourceWithCheckedExceptionImpl();

    Throwable ex = catchThrowable(
        () -> RxJavaReflectionUtils.invokeMethodAndReturnObservable(resourceImpl, resourceImpl.getRelatedResourceMethod(), metrics, typeSupport));

    assertThat(ex).isInstanceOf(HalApiServerException.class)
        .hasMessageStartingWith("A checked exception was thrown when calling TestResourceWithCheckedExceptionImpl#getExternal")
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  public void calling_private_methods_should_throw_exception() {

    TestResourceWithCheckedExceptionImpl resourceImpl = new TestResourceWithCheckedExceptionImpl();
    assertThat(resourceImpl.test()).isEqualTo("foo");

    Throwable ex = catchThrowable(
        () -> RxJavaReflectionUtils.invokeMethodAndReturnObservable(resourceImpl, resourceImpl.getPrivateMethod(), metrics, typeSupport));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to invoke method TestResourceWithCheckedExceptionImpl#test")
        .hasCauseInstanceOf(IllegalAccessException.class);
  }

  private static final class TestResourceWithCheckedExceptionImpl implements TestResourceWithCheckedException {

    @Override
    public LinkableResource getExternal() throws IOException {
      throw new IOException("File not found");
    }

    private String test() {
      return "foo";
    }

    private Method getRelatedResourceMethod() throws NoSuchMethodException, SecurityException {
      return this.getClass().getMethod("getExternal");
    }

    private Method getPrivateMethod() throws SecurityException {
      return ReflectionUtils.findMethod(this.getClass(), "test").get();
    }
  }

}
