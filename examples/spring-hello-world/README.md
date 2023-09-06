<img src="https://wcm.io/images/favicon-16@2x.png"/> Rhyme - Spring Hello World Example
======

# Summary

This is a minimal example project that shows how to use Caravan Rhyme in a [Spring Boot](https://spring.io/projects/spring-boot) application
to render and consume [HAL+JSON](https://stateless.group/hal_specification.html) resources.

# Build and Run

Using **JDK 17** and **Apache Maven 3.6.3** (or higher) you should be able to build and run the example like this:

```
git clone https://github.com/wcm-io-caravan/caravan-rhyme.git
cd caravan-rhyme
git checkout master
mvn -f examples/spring-hello-world/ clean verify spring-boot:run
```

If there are any failures during the build or integration tests, then please open an issue on github!

If not, you can then open the API entry point in your web browser to start exploring the API
- as raw HAL+JSON: http://localhost:8080/
- using the HAL Explorer: http://localhost:8080/explorer/index.html#uri=/

The **Doc** icons in the HAL Explorer will link to a HTML documentation (which is automatically generated from the source code).

# Source Code

- [HelloWorldResource](src/main/java/io/wcm/caravan/rhyme/examples/spring/helloworld/HelloWorldResource.java) - defines a simple API using Rhyme's annotations
- [HelloWorldController](src/main/java/io/wcm/caravan/rhyme/examples/spring/helloworld/HelloWorldController.java) - implements three different variations of this resource that are linked to each other
- [HelloWorldTest](src/test/java/io/wcm/caravan/rhyme/examples/spring/helloworld/HelloWorldTest.java) - is a full integration test that uses a `HalApiClient` to request the resources from a running instance of the application
