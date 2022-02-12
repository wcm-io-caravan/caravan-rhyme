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

import javax.ws.rs.core.Application;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsBundleInfo;

/**
 * OSGi DS component that implements {@link JaxRsBundleInfo}
 */
@Component(service = JaxRsBundleInfo.class, scope = ServiceScope.BUNDLE)
public class JaxRsBundleInfoImpl implements JaxRsBundleInfo {

  private static final Logger log = LoggerFactory.getLogger(JaxRsBundleInfoImpl.class);

  private String applicationPath;
  private String bundleVersion;

  @Activate
  void activate(ComponentContext componentCtx) {

    Bundle usingBundle = componentCtx.getUsingBundle();

    log.debug("Activating {} service for using bundle {} with id {}", getClass().getSimpleName(), usingBundle.getSymbolicName(), usingBundle.getBundleId());

    applicationPath = findApplicationBasePath(usingBundle);

    bundleVersion = findBundleVersion(usingBundle);
  }

  private static String findApplicationBasePath(Bundle bundle) {

    ServiceReference<Application> serviceRef = findJaxRsApplicationServiceInBundle(bundle);

    Object applicationBaseProperty = serviceRef.getProperty(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE);
    if (applicationBaseProperty == null) {
      Application app = bundle.getBundleContext().getService(serviceRef);
      throw new HalApiDeveloperException("No @JaxrsApplicationBase annotation present on the " + app.getClass().getName() + " class");
    }

    log.info("Found application base path {} in bundle {}", applicationBaseProperty, bundle.getSymbolicName());

    return applicationBaseProperty.toString();
  }

  @SuppressWarnings("PMD.AvoidRethrowingException")
  static ServiceReference<Application> findJaxRsApplicationServiceInBundle(Bundle bundle) {

    try {
      // there can be multiple Application service registered, so we filter the one that is defined
      // in the bundle that is referencing this service
      String filter = "(&(" + Constants.SERVICE_BUNDLEID + "=" + bundle.getBundleId() + "))";

      return bundle.getBundleContext().getServiceReferences(Application.class, filter).stream()
          .findFirst()
          .orElseThrow(() -> new HalApiDeveloperException("No component extending JAX-RS Application was found in the bundle " + bundle.getSymbolicName()));
    }
    catch (HalApiDeveloperException ex) {
      throw ex;
    }
    catch (InvalidSyntaxException | RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to find JAX-RS Application service for bundle " + bundle.getSymbolicName(), ex);
    }
  }

  private static String findBundleVersion(Bundle bundle) {

    String version = bundle.getVersion().toString();

    if (!version.endsWith("SNAPSHOT")) {
      return version;
    }

    return version + "-" + bundle.getHeaders().get("Bnd-LastModified");
  }

  @Override
  public String getApplicationPath() {
    return applicationPath;
  }

  @Override
  public String getBundleVersion() {
    return bundleVersion;
  }
}
