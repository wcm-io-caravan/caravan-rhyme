<img src="https://wcm.io/images/favicon-16@2x.png"/> Rhyme Core Framework
======
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm.caravan/io.wcm.caravan.rhyme.core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm.caravan/io.wcm.caravan.rhyme.core)

This module contains all the core interfaces and implementation classes for the Rhyme framework.

# Usage

This is the only direct dependency you need to add to any Java project to use Rhyme for consuming or implementing HAL API interfaces as explained in the main [README](/README.md).

There are also some additional integration modules available that you can use instead:
- [Spring (Boot)](/integration/spring)
- [OSGi/JAX-RS](/integration/osgi-jaxrs)
- [AEM](/integration/aem)

These integration modules will also include the core framework as a transient dependency. They do make some assumptions that may not fit your use case, so nothing prevents you from just using the core framework instead (and copy and adjust code from the integration modules as required).

# Documentation
- See the main [README](/README.md) for a general introduction to the usage of the Rhyme framework
- See the **Javadocs** at https://caravan.wcm.io/rhyme/core/apidocs/ for more detailed documentation
