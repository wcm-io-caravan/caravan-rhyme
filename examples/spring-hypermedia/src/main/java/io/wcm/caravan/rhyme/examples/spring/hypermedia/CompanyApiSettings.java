package io.wcm.caravan.rhyme.examples.spring.hypermedia;


/**
 * Defines some common settings that can be configured by consumers via
 * {@link CompanyApi#withSettings(CompanyApiSettings)}, and will be applied to all
 * resources of the API.
 */
public interface CompanyApiSettings {

  /**
   * @return true if the server should be allowed to include embedded resources in the response.
   */
  default Boolean getUseEmbeddedResources() {
    return true;
  }

  /**
   * @return true if URL fingerprinting should be used for all links
   */
  default Boolean getUseFingerprinting() {
    return true;
  }
}
