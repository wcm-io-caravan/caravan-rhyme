<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Rhyme - Spring Hypermedia Example
======

# Introduction

This is an example project that shows how to use Caravan Rhyme in a [Spring Boot](https://spring.io/projects/spring-boot) application
to render and consume [HAL+JSON](https://stateless.group/hal_specification.html) resources.

It's based on the [Spring HATEOAS - Hypermedia Example](https://github.com/spring-projects/spring-hateoas-examples/tree/main/hypermedia)
project by [Greg L. Turnquist](https://github.com/gregturn) and shows the similarities and differences to a plain [Spring HATEOAS](https://spring.io/projects/spring-hateoas) application.

One area where it differs is that the meaning of the`company:detailedEmployee` relation was changed to become an example for how a service built with **Rhyme** 
can easily create a resource based on other HAL+JSON resources retrieved by HTTP from an upstream service. Make sure not to miss loading this resource when you run the example, and have a look at the embedded `rhyme:metadata` resource to see how the core Rhyme framework keeps track of those requests to the upstream service (as explained [here](https://github.com/wcm-io-caravan/caravan-rhyme#data-debugging-and-performance-analysis) in the root README)

# Build and Run

Until the `feature/spring-hypermedia-example` branch is merged to develop, you need to build this example from source to run it.

Using **JDK 8 or 11** and **Apache Maven 3.6.0** (or higher) you should be able to build and run the example like this:

```
git clone https://github.com/wcm-io-caravan/caravan-rhyme.git
cd caravan-rhyme
git checkout feature/spring-hypermedia-example
mvn clean install
mvn -f examples/spring-hypermedia/ spring-boot:run
```
You can then open http://localhost:8081/browser/index.html in your web browser to start exploring the API. 

The **docs** icons in the HAL browser will link to a context-sensitive HTML documentation for each resource and relation.

# Source Code Structure

## Example Service Code
The package [io.wcm.caravan.rhyme.examples.spring.hypermedia](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia) 
contains all code to define and implement the example API. You can compare it to the [corresponding package](https://github.com/spring-projects/spring-hateoas-examples/tree/main/hypermedia/src/main/java/org/springframework/hateoas/examples)
in the Spring HATEOAS example project (on which it is based) to understand the similarities and differences.

You can easily browse the source code on github by starting with the [CompanyApi](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/CompanyApi.java)
entrypoint interface, and then use github's cross-referencing links that pop up when you click on a resource class name. 'Definition' links will always take you to the corresponding
`@HalApiInterface` definition, and 'References' will find the server-side implementation class.

It's one of the key advantages of **Rhyme** that the available relations of the API's resources are very clearly defined in each `@HalApiInterface`,
and it's as easy to navigate through the code as it is to navigate to your resources in the HAL browser. The annotations and Javadoc comments from the API interfaces are
also also the single source for the generated HTML documentation that is automatically linked from your resources with the `curies` relation.

## Spring Integration Code
The package [io.wcm.caravan.rhyme.spring.impl](src/main/java/io/wcm/caravan/rhyme/spring/impl) contains the code to integrate the core **Rhyme**
framework with Spring Boot. This will go into a seperate integration module in the near future, so it can be re-used outside of this example, 
using only the interfaces from the [io.wcm.caravan.rhyme.spring.api](src/main/java/io/wcm/caravan/rhyme/spring/api) package.

## Integration Tests
The test package [io.wcm.caravan.rhyme.examples.spring.hypermedia](src/test/java/io/wcm/caravan/rhyme/examples/spring/hypermedia) contains simple yet extensive tests for
for the example service. One key thing to note here is that all test cases defined in `AbstractCompanyApiIntegrationTest` are using only the `CompanyApi` entry point interface
to access the resources under test. This allows the same set of tests to be run twice (by the two subclasses): directly testing the server-side implementations of those interfaces, and executing actual HTTP requests to your controllers, parsing the response and following links as required using a 'HalApiClient' instance.




