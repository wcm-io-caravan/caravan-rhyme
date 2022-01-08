package io.wcm.caravan.rhyme.examples.spring.hypermedia;


/**
 * Defines some common settings that can be configured by consumers via
 * {@link CompanyApi#withSettings(CompanyApiSettings)}, and will be applied to all
 * resources of the API.
 */
public class CompanyApiSettings {

  public Boolean useEmbeddedResources;

  public Boolean useFingerprinting;

  /**
   * @return true if the server should be allowed to include embedded resources in the response.
   */
  public Boolean getUseEmbeddedResources() {
    return useEmbeddedResources;
  }

  /**
   * @return true if URL fingerprinting should be used for all links
   */
  public Boolean getUseFingerprinting() {
    return useFingerprinting;
  }

  public CompanyApiSettings setUseEmbeddedResources(boolean value) {
    useEmbeddedResources = value;
    return this;
  }

  public CompanyApiSettings setUseFingerprinting(boolean value) {
    useFingerprinting = value;
    return this;
  }
}
