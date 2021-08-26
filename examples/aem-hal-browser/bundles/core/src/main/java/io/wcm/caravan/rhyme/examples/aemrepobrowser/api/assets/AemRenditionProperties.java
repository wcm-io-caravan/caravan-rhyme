package io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets;

public interface AemRenditionProperties {

  boolean isValid();

  String getInvalidReason();

  String getMimeType();

  Integer getHeight();

  Integer getWidth();
}
