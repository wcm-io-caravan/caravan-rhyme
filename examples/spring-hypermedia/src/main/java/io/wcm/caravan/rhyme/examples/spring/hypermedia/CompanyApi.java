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
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import static io.wcm.caravan.rhyme.api.common.RequestMetricsCollector.EMBED_RHYME_METADATA;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * The single entry point for this hypermedia example HAL API. When it's rendered as a HAL resource, it contains link
 * templates for every other available resource, and additional resolved links to discover all employees and managers
 * without knowing their IDs.
 */
@HalApiInterface
public interface CompanyApi extends LinkableResource {

  static final String USE_FINGERPRINTING = "useFingerprinting";
  static final String USE_EMBEDDED_RESOURCES = "useEmbeddedResources";
  static final String ID = "id";

  /**
   * @return a collection of all employees in the company database
   */
  @Related("company:employees")
  EmployeeCollectionResource getEmployees();

  /**
   * @return a collection of all managers in the company database
   */
  @Related("company:managers")
  ManagerCollectionResource getManagers();

  /**
   * a link template to load an employee entity with a known ID.
   * @param id the generated primary key in the database
   * @return an {@link EmployeeResource}
   * @throws HalApiClientException with 404 status code if no employee was found with the given ID
   */
  @Related("company:employee")
  EmployeeResource getEmployeeById(@TemplateVariable(ID) Long id);

  /**
   * a link template to load a manager entity with a known ID.
   * @param id the generated primary key in the database
   * @return a {@link ManagerResource}
   * @throws HalApiClientException with 404 status code if no manager was found with the given ID
   */
  @Related("company:manager")
  ManagerResource getManagerById(@TemplateVariable(ID) Long id);

  /**
   * a link template to load a more detailed representation of an employee, which also embeds the entities of his
   * manager and all of his colleagues.
   * @param id the generated primary key in the database
   * @return a {@link DetailedEmployeeResource}
   * @throws HalApiClientException with 404 status code if no employee was found with the given ID
   */
  @Related("company:detailedEmployee")
  DetailedEmployeeResource getDetailedEmployeeById(@TemplateVariable(ID) Long id);

  /**
   * Reload the API entry point with different client preferences, which will lead to all links
   * being generated with additional parameters that enforce those preferences.
   * @param useEmbeddedResources if set to false, the server will never embed any resources, but only use links
   * @param useFingerprinting if set to false, no time stamp parameters will be added to the URLs
   * @param embedRhymeMetadata if set to true, extensive information for developers
   *          will be embedded into every resource
   * @return a new {@link CompanyApi} instance with the given settings
   */
  @Related("company:preferences")
  CompanyApi withClientPreferences(
      @TemplateVariable(USE_EMBEDDED_RESOURCES) Boolean useEmbeddedResources,
      @TemplateVariable(USE_FINGERPRINTING) Boolean useFingerprinting,
      @TemplateVariable(EMBED_RHYME_METADATA) Boolean embedRhymeMetadata);
}
