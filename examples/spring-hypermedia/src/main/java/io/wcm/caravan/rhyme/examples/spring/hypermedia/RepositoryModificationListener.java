package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import java.time.Instant;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Keeps track of any modifications to the repository, so that timestamped URLs can be created by the
 * {@link TimestampedLinkBuilder}
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
