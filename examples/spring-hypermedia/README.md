<img src="https://wcm.io/images/favicon-16@2x.png"/> Rhyme - Spring Hypermedia Example
======

# Introduction

This is an example project that shows how to use Caravan Rhyme in a [Spring Boot](https://spring.io/projects/spring-boot) application
to render and consume [HAL+JSON](https://stateless.group/hal_specification.html) resources.

It's based on the [Spring HATEOAS - Hypermedia Example](https://github.com/spring-projects/spring-hateoas-examples/tree/main/hypermedia)
project by [Greg L. Turnquist](https://github.com/gregturn) and shows the similarities and differences to a plain [Spring HATEOAS](https://spring.io/projects/spring-hateoas) application.

There are a few differences to the original example:
* additional link templates to fetch individual entities have been added to the entry point
* all links are using custom relations, and a `curies` link is pointing to the generated documentation
* manager resources directly link or embed the related employees (since there is no benefit from having an additional collection resource)
* clients have full control on whether embedded resource are being used
* the supervisor examples and collection of detailed employee resources have been omitted
* the detailed employee resources contain additional linked or embedded resources, including an external HTML link (as an example how to to link to non-HAL resources)

Make sure not to miss loading a `company:detailedEmployee` link when you run the example, as this was specifically included to show how a resource implementation will load other HAL resources via **Rhyme**'s client proxies. Add an `embedRhymeMetadata` query parameter to see the embedded `rhyme:metadata` resource, which shows how the core Rhyme framework keeps track of those HTTP requests (as explained [here](https://github.com/wcm-io-caravan/caravan-rhyme#data-debugging-and-performance-analysis) in the root README)

# Build and Run


Using **JDK 8, 11 or 17** and **Apache Maven 3.6.3** (or higher) you should be able to build and run the example like this:

```
git clone https://github.com/wcm-io-caravan/caravan-rhyme.git
cd caravan-rhyme
git checkout master
mvn -f examples/spring-hypermedia/ clean verify spring-boot:run
```

If there are any failures during the build or integration tests, then please open an issue on github!

If not, you can then open the API entry point in your web browser to start exploring the API:
- as raw HAL+JSON: http://localhost:8081/
- using the HAL Browser: http://localhost:8081/browser/index.html
- using the HAL Explorer: http://localhost:8081/explorer/index.html#uri=/

The **docs** icons in the HAL Browser will link to an HTML documentation for each resource and relation (which is automatically generated from the source code).

# Source Code Structure

## Example Service Code
The package [io.wcm.caravan.rhyme.examples.spring.hypermedia](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia) 
contains all code to define and implement the example API. You can compare it to the [corresponding package](https://github.com/spring-projects/spring-hateoas-examples/tree/main/hypermedia/src/main/java/org/springframework/hateoas/examples)
in the Spring HATEOAS example project (on which it is based) to understand the similarities and differences.

## Integration Tests
The test package [io.wcm.caravan.rhyme.examples.spring.hypermedia](src/test/java/io/wcm/caravan/rhyme/examples/spring/hypermedia) contains simple yet extensive tests for for the example service.

One key thing to note here is that all test cases defined in `AbstractCompanyApiIT` are using only the `CompanyApi` entry point interface
to navigate to the resources under test. This allows the same set of tests to be run multiple times (by the subclasses):
- directly using the server-side implementations (like a consumer of the API running in the same application context would)
- using Spring's `MockMvc` to create mock requests to the contollers (to also test the path mappings, link generation and JSON (de)serialization)
- actually starting up the server on a random port and using a regular HTTP client to retrieve the resources, exactly as an external consumer would do

# Examples for Key Concepts

This is an example with very limited functionality, but its intention is to demonstrate most of the key concepts of **Rhyme**:

## Using annotated interfaces to define your API

You can easily browse the source code on github by starting with the [CompanyApi](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/CompanyApi.java#34)
entrypoint interface, and then use github's cross-referencing links that pop up when you click on a resource class name. 'Definition' links will always take you to the corresponding
`@HalApiInterface` definition, and 'References' will find the server-side implementation class.

It's one of the key advantages of **Rhyme** that the available relations of the API's resources are very clearly defined in each `@HalApiInterface`,
and it's as easy to navigate through the code as it is to navigate to your resources in the HAL browser. 

## Generation and Integration of HTML API documentation

The annotations and Javadoc comments from the API interfaces are
also the single source for the generated HTML documentation that is automatically linked from your resources with the `curies` relation.

Tools such as the HAL Browser or HAL Explorer will automatically link to this documentation directly for any custom relation that is being used.

The exact same documentation is also used by developers implementing the service (as its based on Javadocs), and consumers will also see the same Javadocs when using the Rhyme client proxies to access your API (if you decide to publish your interfaces to your consumers as a seperate module). This ensures that documentation is easy to maintain at a single location and immediately available to everyone using the API.

## Rendering HAL resources

To **render** a HAL resource in a web service built with Spring Boot & Rhyme, you only have to return an **implementation of the corresponding interface** in your controller method. In this example, the implementations clasess don't have much logic so they are all defined directly in the controllers (often as anonymous inner classes). The nice thing about having these resources represented as classes is that you can easily refactor (and move the code around) as required while your project grows, and use either composition or inheritance where it makes sense.

**Linking** to other resources works by **returning resource implementations created by other controllers**. See the [CompanyApiController](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/CompanyApiController.java) as an example how it easily defines links for the HAL representation of the entry point. Note that the way this works also allows internal consumers to call those methods directly, with the same semantics defined in the interface.

## Embedded Resources

The [EmployeeResourceImpl](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/EmployeeController.java#L134) class is a good example how you can control whether a resource should be embedded. Simply implementing the `EmbeddedableResource` interface would embed this resource wherever it is linked. By overriding `#isEmbedded()` you can control when this should happen.

In this example, the clients are giving full control over whether they want to have employee and manager resources embedded or not. By default, these resources are usually embedded, but clients can follow the `company:preferences` link from the entry point, and set the `useEmbeddedResources` variable to `false`. This will reload the entry point with all links modified so that the server will only render links to these resources.

## Consuming HAL resources with Rhyme client proxies

See the [DetailedEmployeeController](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/DetailedEmployeeController.java) as an example for a controller that fetches other resources from an upstream service to build its own response. 

In this case this makes the implementation more complicated than it needs to be, as it's just retrieving other resources from the same API on localhost (which we could also call directly). But the point is to show how resource implementations can use Rhyme to fetch and aggregate data using client proxies, and this would work the same way for an external service.

Loading such a detailed employee resource for the very first time after startup is quite slow, as this will initialize Spring's `WebClient` and everything behind it. But follow-up requests should be much faster, and in-memory caching will avoid any unnecessary HTTP requests being executed.

## Caching and URL Fingerprinting

Any link from the entry point contains an additional `timestamp` parameter. It's based on the last modification date of the repositories. This URL fingerprinting allows any of the linked resource to set a long value (100 days) for the `max-age` cache-control directive. Because the entry point is only cached for a short amount of time (10 seconds), any clients will still receive updated data quickly (as long as they always start their journey at the entry point).

There is no code required within the resource implementations (and no parameters exposed in the API) to achieve this. All this is handled by the central [CompanyApiLinkBuilder](src/main/java/io/wcm/caravan/rhyme/examples/spring/hypermedia/CompanyApiLinkBuilder.java).

## Including embedded metadata

For any resource in the example, you can add an `embedRhymeMetadata` query parameter to the URL to see additional details from the framework that were collected while rendering the resource. If you set this parameter in the `company:preferences` link template in the entry point, this parameter will be automatically added to all links while browsing through the resources.

This metadata is especially interesting on the `company:detailedEmployee` resources, as it will also show you exactly which other employee and manager resources have been loaded in the background using the `HalApiClient`.

You'll also see in the metadata how the amount of upstream resources, their URLs and the `max-age` will change if you are using different values for the `useEmbeddedResources` or `useUrlFingerprinting` preferences.

## Error Handling

If you expand the `company:detailedEmployee` link template in the entry point with an invalid value for the `id` variable, you will see how any exception that is thrown will result in a response with content type `application/vnd.error+json` being rendered.

If you do enter a non-numeric id, you'll see a nice chain of errors explaining why a 400 / Bad Request response is returned.

If you are using a non-existant integer employee id, things get more interesting: You'll see that the expected 404 / Not Found was caused by a failed request to load the regular employee resource. Again you see the full chain of exceptions, and the last entry in the embedded `errors` resources is actually originating from the response body of the request that has failed. There's also a `via` link to the exact URL that failed to load.

Having this constent error representation allows a very quick root cause analysis (without having to correlate log entries from multiple systems) when you have a larger distributed system with multiple web services.

Of course you wouldn't want to show such detailed error information on a public-facing service, but it can all be easily stripped out by an API gateway.
