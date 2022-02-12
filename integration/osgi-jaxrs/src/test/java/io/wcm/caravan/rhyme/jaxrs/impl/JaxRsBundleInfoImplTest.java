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
package io.wcm.caravan.rhyme.jaxrs.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.core.Application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

@ExtendWith(MockitoExtension.class)
class JaxRsBundleInfoImplTest {

  private static final String BUNDLE_NAME = "mocked-bundle";
  private static final String BASE_PATH = "/base/path";
  @Mock
  private ComponentContext componentCtx;
  @Mock
  private Bundle bundle;
  @Mock
  private BundleContext bundleCtx;
  @Mock
  private ServiceReference<Application> serviceRef;

  @BeforeEach
  void setUp() {
    when(componentCtx.getUsingBundle()).thenReturn(bundle);
    when(bundle.getBundleContext()).thenReturn(bundleCtx);
  }


  private JaxRsBundleInfoImpl createAndActivateBundle() {
    JaxRsBundleInfoImpl bundleInfo = new JaxRsBundleInfoImpl();
    bundleInfo.activate(componentCtx);
    return bundleInfo;
  }

  private void mockPresenceOfApplicationService(String basePath) throws InvalidSyntaxException {
    when(bundleCtx.getServiceReferences(eq(Application.class), anyString()))
        .thenReturn(ImmutableList.of(serviceRef));
    lenient().when(bundleCtx.getService(serviceRef)).thenReturn(mock(Application.class));
    when(serviceRef.getProperty(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE)).thenReturn(basePath);
  }

  private void mockVersion(Version version) {
    when(bundle.getVersion()).thenReturn(version);
  }

  private void mockBundleHeadersWithLastModified(String timestamp) {
    Dictionary<String, String> bundleHeaders = new Hashtable<>();
    bundleHeaders.put("Bnd-LastModified", timestamp);
    when(bundle.getHeaders()).thenReturn(bundleHeaders);
  }


  @Test
  void getApplicationPath_should_fail_if_no_application_is_registered() {

    when(bundle.getSymbolicName()).thenReturn(BUNDLE_NAME);

    Throwable ex = catchThrowable(this::createAndActivateBundle);

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("No component extending JAX-RS Application was found in the bundle " + BUNDLE_NAME);
  }

  @Test
  void getApplicationPath_should_fail_if_no_base_path_annotation_present() throws InvalidSyntaxException {

    mockPresenceOfApplicationService(null);

    Throwable ex = catchThrowable(this::createAndActivateBundle);

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("No @JaxrsApplicationBase annotation present");
  }

  @Test
  void getApplicationPath_should_return_path_from_service_ref() throws InvalidSyntaxException {

    mockPresenceOfApplicationService(BASE_PATH);
    mockVersion(new Version(1, 2, 3));

    JaxRsBundleInfoImpl bundleInfo = createAndActivateBundle();

    assertThat(bundleInfo.getApplicationPath()).isEqualTo(BASE_PATH);
  }

  @Test
  void getBundleVersion_should_return_release_version() throws InvalidSyntaxException {

    mockPresenceOfApplicationService(BASE_PATH);
    mockVersion(new Version(1, 2, 3));

    JaxRsBundleInfoImpl bundleInfo = createAndActivateBundle();

    assertThat(bundleInfo.getBundleVersion()).isEqualTo("1.2.3");
  }

  @Test
  void getBundleVersion_should_return_snapshot_version() throws InvalidSyntaxException {

    mockPresenceOfApplicationService(BASE_PATH);
    mockVersion(new Version(1, 2, 3, "SNAPSHOT"));
    mockBundleHeadersWithLastModified("123456");

    JaxRsBundleInfoImpl bundleInfo = createAndActivateBundle();

    assertThat(bundleInfo.getBundleVersion()).isEqualTo("1.2.3.SNAPSHOT-123456");
  }


  @Test
  void findJaxRsApplicationServiceInBundle_should_handle_exceptions() throws InvalidSyntaxException {

    when(bundleCtx.getServiceReferences(any(Class.class), anyString()))
        .thenThrow(new RuntimeException());

    Throwable ex = catchThrowable(() -> createAndActivateBundle());

    assertThat(ex)
        .isInstanceOf(RuntimeException.class)
        .hasMessageStartingWith("Failed to find JAX-RS Application service for bundle");
  }
}
