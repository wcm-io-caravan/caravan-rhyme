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

import static io.wcm.caravan.rhyme.testing.TestRelations.LINKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * contains only some tests for edge case exception handling that is hard to reproduce with realistic client / renderer
 * tests
 */
class RxJavaReflectionUtilsTest {

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();
  private final HalApiTypeSupport typeSupport = new DefaultHalApiTypeSupport();

  @HalApiInterface
  public interface TestResourceWithCheckedException {

    @Related(LINKED)
    LinkableResource getExternal() throws IOException;
  }

  @Test
  void throwing_checked_exceptions_should_be_handled() {

    TestResourceWithCheckedExceptionImpl resourceImpl = new TestResourceWithCheckedExceptionImpl();

    Throwable ex = catchThrowable(
        () -> RxJavaReflectionUtils.invokeMethodAndReturnObservable(resourceImpl, resourceImpl.getRelatedResourceMethod(), metrics, typeSupport));

    assertThat(ex).isInstanceOf(HalApiServerException.class)
        .hasMessageStartingWith("A checked exception was thrown when calling #getExternal of TestResourceWithCheckedExceptionImpl")
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void calling_private_methods_should_throw_exception() {

    TestResourceWithCheckedExceptionImpl resourceImpl = new TestResourceWithCheckedExceptionImpl();
    assertThat(resourceImpl.test()).isEqualTo("foo");

    Throwable ex = catchThrowable(
        () -> RxJavaReflectionUtils.invokeMethodAndReturnObservable(resourceImpl, resourceImpl.getPrivateMethod(), metrics, typeSupport));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to invoke method #test of TestResourceWithCheckedExceptionImpl")
        .hasCauseInstanceOf(IllegalAccessException.class);
  }

  @Test
  void convertObservableTo_should_fail_for_unsupported_types() {

    Observable<String> obs = Observable.just("foo");

    Throwable ex = catchThrowable(
        () -> RxJavaReflectionUtils.convertObservableTo(Iterator.class, obs, typeSupport));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessage("The given target type of java.util.Iterator is not a supported return type");
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
