<img src="https://wcm.io/images/favicon-16@2x.png"/> Rhyme API Interfaces and Annotations
======
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm.caravan/io.wcm.caravan.rhyme.api-interfaces/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm.caravan/io.wcm.caravan.rhyme.api-interfaces)

This module contains only the Java annotations, interfaces and some constants required to define your Rhyme HAL API interfaces.

# Usage

You'll only need this as a direct dependency if you want to create a module that only contains your `@HalApiInterface` definitions (and classes or interfaces representing their JSON resource state).
You could share such a module with consumers of your API (without actually sharing any implementation code!), so they can use `HalApiClient` or `Rhyme` from the core framework to easily create dynamic client proxies for your HAL API interfaces.

In a project where you actually want to write code using these interfaces, you should instead add the [core](/core) module (or any of the [integration](/integration) modules) as dependency.

# Documentation
- See the main [README](/README.md) for a general introduction to the usage of these annotations and interfaces
- See the **Javadocs** at https://caravan.wcm.io/rhyme/api-interfaces/apidocs/ for more detailed documentation
- See the [Spring Hypermedia](/examples/spring-hypermedia) example for real usage

