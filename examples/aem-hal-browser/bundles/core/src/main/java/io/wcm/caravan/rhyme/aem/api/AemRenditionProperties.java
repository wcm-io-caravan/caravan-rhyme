package io.wcm.caravan.rhyme.aem.api;

public interface AemRenditionProperties {

  boolean isValid();

  String getInvalidReason();

  String getMimeType();

  Integer getHeight();

  Integer getWidth();
}
