package io.wcm.caravan.rhyme.aem.api.resources;

import io.wcm.caravan.rhyme.aem.api.linkbuilder.FingerprintBuilder;

public interface ImmutableResource {

  void buildFingerprint(FingerprintBuilder fingerprint);
}
