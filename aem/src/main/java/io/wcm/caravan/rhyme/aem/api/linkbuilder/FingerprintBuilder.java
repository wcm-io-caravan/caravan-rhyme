package io.wcm.caravan.rhyme.aem.api.linkbuilder;

public interface FingerprintBuilder {

  void useFingerprintFromIncomingRequest();

  void addLastModifiedOfContentBelow(String path);

}
