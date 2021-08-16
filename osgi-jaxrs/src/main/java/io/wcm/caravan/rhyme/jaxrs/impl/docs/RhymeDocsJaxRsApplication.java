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
package io.wcm.caravan.rhyme.jaxrs.impl.docs;

import javax.ws.rs.core.Application;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationBase;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;

@Component(service = Application.class, scope = ServiceScope.BUNDLE)
@JaxrsApplicationBase(RhymeDocsJaxRsApplication.BASE_PATH)
@JaxrsName(RhymeDocsJaxRsApplication.JAXRS_NAME)
public class RhymeDocsJaxRsApplication extends Application {

  public static final String BASE_PATH = "/docs/rhyme/api";

  public static final String JAXRS_NAME = "caravan.rhyme.rhymedocs";

  public static final String SELECTOR = "(osgi.jaxrs.name=" + JAXRS_NAME + ")";

}
