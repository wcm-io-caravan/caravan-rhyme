/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

import io.wcm.caravan.rhyme.jaxrs.api.JaxRsBundleInfo;

/**
 * OSGi DS component that implements {@link JaxRsBundleInfo}
 */
@Component(service = JaxRsBundleInfo.class, scope = ServiceScope.BUNDLE)
public class JaxRsBundleInfoImpl implements JaxRsBundleInfo {

  private String applicationPath;
  private String bundleVersion;

  @Activate
  void activate(ComponentContext componentCtx) {

    Bundle usingBundle = componentCtx.getUsingBundle();

    applicationPath = findApplicationBasePath(usingBundle);

    bundleVersion = findBundleVersion(usingBundle);
  }

  private String findApplicationBasePath(Bundle bundle) {

    ServiceReference<Application> serviceRef = bundle.getBundleContext().getServiceReference(Application.class);
    if (serviceRef == null) {
      throw new RuntimeException("No component extending JAX-RS Application was found in the bundle " + bundle.getSymbolicName());
    }

    Object applicationBaseProperty = serviceRef.getProperty(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE);
    if (applicationBaseProperty == null) {
      Application app = bundle.getBundleContext().getService(serviceRef);
      throw new RuntimeException("No @JaxrsApplicationBase annotation present on the " + app.getClass().getName() + " class");
    }

    return applicationBaseProperty.toString();
  }

  private String findBundleVersion(Bundle bundle) {

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
