<img src="https://wcm.io/images/favicon-16@2x.png"/> Rhyme - OSGi / JAX-RS Integration
======
[![Maven Central](https://img.shields.io/maven-central/v/io.wcm.caravan/io.wcm.caravan.rhyme.osgi-jaxrs)](https://repo1.maven.org/maven2/io/wcm/caravan/io.wcm.caravan.rhyme.osgi-jaxrs/)

This module contains OSGi service interfaces and implementations to use Rhyme in an OSGI R7 container with the [JAX-RS Whiteboard](http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html)

It's meant to be used in combination with related [wcm.io Caravan](https://caravan.wcm.io/) projects:

- [Resilient HTTP](https://caravan.wcm.io/io/http/) - an asychronous resilient HTTP transport layer (required)
- [JSON Pipeline](https://caravan.wcm.io/pipeline/) - for aggregation, slicing and caching of JSON responses (optional)

# Key Differences and Extensions to Rhyme Core Module
- `CaravanHalApiClient` is an alternative to the core module's `HalApiClient` based on the Reslient HTTP project
  - if the JSON Pipeline bundles are available in the OSGI container, then they will be used for caching 
- `JaxRsAsyncHalResponseRenderer` - a wrapper around the core module's `AsyncHalResponseRender` that renders resources into JAX-RS `AsyncResponse` instances

# Examples

* [osgi-jaxrs-example-service](/examples/osgi-jaxrs-example-service) - an example for a fully asynchronous service implementation
* [osgi-jaxrs-example-launchpad](/examples/osgi-jaxrs-example-launchpad) - the launchpad to start that example service (and run some integration tests)

# Documentation
- See the main [README](/README.md) for a general introduction to the usage of the Rhyme framework
- See the **Javadocs** for the OSGi / JAX-RS integration API at https://caravan.wcm.io/rhyme/osgi-jaxrs/apidocs/ 
- The **Javadocs** for the core framework can be found at https://caravan.wcm.io/rhyme/core/apidocs/ 
