<img src="https://wcm.io/images/favicon-16@2x.png"/> Rhyme - Spring Boot Integration
======
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm.caravan/io.wcm.caravan.rhyme.spring/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm.caravan/io.wcm.caravan.rhyme.spring)

This module contains API interfaces and Spring beans to simplify using Rhyme to build a HAL microservice as a Spring Boot application.

# Usage

Simply add this module as a dependency to your Spring Boot project, and the extensions explained below should become effective through auto-configuration.

It currently doesn't give you much control to configure or replace the default behaviour that will become effective through auto-configuration by simply adding this module as a dependency.

If you want to use Rhyme in an existing application, there are some areas where it can break your application (e.g. exception handling) 

In that case, you can always just use the core module instead, and use the Spring integration [implementation classes](/src/main/java/io/wcm/caravan/rhyme/spring/impl) as inspiration for the required glue code.

# Key Differences and Extensions to Rhyme Core Module

* You usually don't have to interact with `RhymeBuilder` to configure and create a `Rhyme` instance yourself
* Instead, you can use `@Autowire` to inject a request-scoped `SpringRhyme` bean into your application
  * this SpringRhyme instance will allow you to fetch remote resources (using a  Spring `WebClient`), or modify your response's max-age
* To **render** a resource, simply return a `LinkableResource` instance (that also implements a `@HalApiInterface`) from your `@RestController`'s request handling method
* You can use the `UrlFingerprinting` and `RhymeLinkBuilder` interfaces to create fingerprinted links to your controller methods (with the help of Spring HATEOAS's `WebMvcLinkBuilder`)
* Any exceptions thrown by your resources (or by the Spring framework) will be caught by Rhyme and rendered as a vnd.error resource

# Examples

* [spring-hypermedia](/examples/spring-hypermedia) - a well documented example of all the key concepts
* [spring-hello-world](/examples/spring-hello-world) - a simpler starting point with minimal dependencies

# Documentation
- See the main [README](/README.md) for a general introduction to the usage of the Rhyme framework
- See the **Javadocs** for the Spring integration API at https://caravan.wcm.io/rhyme/spring/apidocs/ 
- The **Javadocs** for the core framework can be found at https://caravan.wcm.io/rhyme/core/apidocs/ 
