package io.wcm.caravan.rhyme.aem.api.linkbuilder;

import org.apache.sling.api.resource.Resource;

public interface FingerprintBuilder {

  void useFingerprintFromIncomingRequest();

  void addLastModifiedOfPagesBelow(Resource resource);

}
