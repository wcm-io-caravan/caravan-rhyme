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

## Spring Integration Code
The package [io.wcm.caravan.rhyme.spring.impl](src/main/java/io/wcm/caravan/rhyme/spring/impl) contains the code to integrate the core **Rhyme**
framework with Spring Boot. This will go into a seperate integration module in the near future, so it can be re-used outside of this example, 
using only the interfaces from the [io.wcm.caravan.rhyme.spring.api](src/main/java/io/wcm/caravan/rhyme/spring/api) package.

## Integration Tests
The test package [io.wcm.caravan.rhyme.examples.spring.hypermedia](src/test/java/io/wcm/caravan/rhyme/examples/spring/hypermedia) contains simple yet extensive tests for for the example service.

One key thing to note here is that all test cases defined in `AbstractCompanyApiIT` are using only the `CompanyApi` entry point interface
to access the resources under test. This allows the same set of tests to be run three times (by the subclasses):
- directly testing the server-side implementations of those interfaces
- using Spring's `MockMvc` to create mock requests to the contollers (to also test the path mappings, and JSON (de)serialization)
- actually starting up the complete application and using a regular HTTP client to retrieve the resources from http://localhost:8081

# Examples for Key Concepts

This is an example with very limited functionality, but the point is that is demonstrates most of the key concepts of **Rhyme**:

## Using annotated interfaces to define your API

You can easily browse the source code on github by starting with the [CompanyApi](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/CompanyApi.java#34)
entrypoint interface, and then use github's cross-referencing links that pop up when you click on a resource class name. 'Definition' links will always take you to the corresponding
`@HalApiInterface` definition, and 'References' will find the server-side implementation class.

It's one of the key advantages of **Rhyme** that the available relations of the API's resources are very clearly defined in each `@HalApiInterface`,
and it's as easy to navigate through the code as it is to navigate to your resources in the HAL browser. 

## Generation and Integration of HTML API documentation

The annotations and Javadoc comments from the API interfaces are
also the single source for the generated HTML documentation that is automatically linked from your resources with the `curies` relation.

Tools such as the HAL Browser our HAL explorer will automatically link to this documentation directly for any custom relation that is being used. The exact same documentation is also used by developer's implementing the service, and developers will also see the same javadocs when using the Rhyme client proxies to access your API (if you decide to publish your interfaces to your consumers as a seperate module). This ensures that documentation is always in sync with the code with very little effort.

## Rendering HAL resources

In a Rhyme web service built with Spring Boot, to render a resource you only have to return an implementation of the corresponding interface in your controller method. In this example, the implementations have very little logic so they are all defined directly in the controllers (often as anonymous inner classes). The nice thing about having these resources as well-structured classes is that you can easily refactor and move the code around as required while your project grows.

Linking to other resources is as easy as calling the methods of other controllers to create the resources you want to link to. See the [CompanyApiController](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/CompanyApiController.java) as an example how it easily defines links for the HAL representation of the entry point. Note that the way this works also allows internal consumers to call those methods directly, with the same semantics defined in the interface.

## Embedded Resources

The [EmployeeResourceImpl](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/EmployeeController.java#L134) class is a good example how you can control whether a resource should be embedded. Simply implementing the 'EmbeddedableResource' interface would embed this resource wherever it is linked. By overriding '#isEmbedded()' you can control in which context this should happen. In this example this depends on which constructor you use, but you can use any logic you want to make that decision. For example you can also expose additional links or template variables in your API to allow your clients to activate or disable the use of embedded resources.

## Consuming HAL resources with Rhyme client proxies

See the [DetailedEmployeeController](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/DetailedEmployeeController.java) as an example for a controller that fetches other resources from an upstream service to build its response. In this case we are just retrieving other resources from the same API on localhost, but it would work the same way for external services.

## Caching and URL Fingerprinting

Any link from the entry point contains an additional 'timestamp' parameter. It's based on the last modification date of the repositories. This URL fingerprinting allows any of the linked resource to set a long value (100 days) for the 'max-age' cache-control directive. Because the entry point is only cached for a short amount of time (10 seconds), any clients will still receive up-to-date resource for repeated requests (as long as they always start their requests at the entry point).

There is no code required within the resource implementations (and no parameters exposed in the API) to achieve this. All this is handled by the central [CompanyApiLinkBuilder](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/CompanyApiLinkBuilder.java)

