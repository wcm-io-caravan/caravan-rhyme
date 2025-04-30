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

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.wcm.caravan.rhyme.spring.api.UrlFingerprinting;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * Keeps track of any modifications to the repositories for {@link Employee} and {@link Manager} entities, so that
 * timestamped URLs can be created by the {@link CompanyApiLinkBuilder} using {@link UrlFingerprinting}.
 */
@Component
class RepositoryModificationListener {

  private static final Logger log = LoggerFactory.getLogger(RepositoryModificationListener.class);

  private volatile Instant lastModified = Instant.now();

  @PostPersist
  @PostUpdate
  @PostRemove
  private void afterAnyUpdate(Object entity) {

    log.debug("An update was detected for {}", entity);

    lastModified = Instant.now();
  }

  Instant getLastModified() {
    return lastModified;
  }
}
