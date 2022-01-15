<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Rhyme
======
[![Build](https://github.com/wcm-io-caravan/caravan-rhyme/workflows/Build/badge.svg?branch=develop)](https://github.com/wcm-io-caravan/caravan-rhyme/actions?query=workflow%3ABuild+branch%3Adevelop)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=wcm-io-caravan_caravan-rhyme&metric=coverage)](https://sonarcloud.io/component_measures?id=wcm-io-caravan_caravan-rhyme&metric=coverage)

![Caravan](https://github.com/wcm-io-caravan/caravan-tooling/blob/master/public_site/src/site/resources/images/caravan.gif)

# Introduction

**Rhyme** is a Java framework for providing or consuming hypermedia APIs using the [HAL+JSON media format](http://stateless.co/hal_specification.html). It's main use case is when you need to do both, i.e. build a distributed system of web services that are connected through several HAL APIs.

**Rhyme** stands for **R**eactive **Hy**per**me**dia, as it fully supports asynchronous generation and retrieval of HAL+JSON resources (using [RxJava 3](https://github.com/ReactiveX/RxJava) internally). Using reactive types however is mostly optional (with very few exceptions). This document mostly sticks to using only simpler blocking code examples, but there is a section that explains how reactive types can be used.

The key concepts and features of **Rhyme** are:
- HAL APIs are represented as **annotated Java interfaces** that define the structure of the resource state, and the available related resources
- These interfaces can be used by consumers as a **highly abstracted client API**, for which Rhyme will create a client implementation at runtime
- The same interfaces are also used to **keep the server-side implementation well structured**, and always in sync with the API interfaces.
- Writing extensive **integration tests** is easy without wasting time on constructing and verifying URL details
- simple and transparent support for **embedded resources**
- generation and integration of **HTML API documentation** from the annotated interfaces
- a **simple and effecting caching model** based on URL fingerprinting and the `cache-control: max-age` header
- Simplifying **data debugging and performance analysis** (by including embedded metadata in every response)
- **Forwarding error information over service boundaries** using the [vnd.error](https://github.com/blongden/vnd.error) media type
- **Supporting asynchronous, reactive programming** on the client and server side

Each of thease concepts and features are explained in more detail below.

**Rhyme** is based on several years of experience and best practices from a large production HAL microservice platform (based on OSGi, JAX-RS, RxJava & wcm.io Caravan code). Since we switched over to using to Spring Boot for new services, we took the opportunity to rewrite the HAL client and rendering code to be usable within any Java project, and make it accessible to anyone. 

## Limitations

An important thing to note is that the **Rhyme** framework currently has no support for [HAL-FORMS](https://rwcbook.github.io/hal-forms/), and the annotations and client implementations don't even try to support anything but **GET** requests to API resources. This is because our primary use case was the aggregation and caching of read-only resources from many different data sources. 

You can still use **Rhyme**'s client interfaces to follow links to a resource, and then extract the URL to which you can POST or PUT using any other HTTP client library.

If you do think a more sophisticated support for other HTTP methods should be added, then please open an issue to discuss what's the best way to extend **Rhyme**!

## Modules in this repository

- [api-interfaces](api-interfaces) - contains only annotations, interfaces and dependencies to be used in your API interface definitions
- [core](core) - the core framework that can be integrated within any Java project
- **Integration** modules for using Rhyme with specific web service frameworks:
  - [spring](integration/spring) - for implementing HAL web services as a Spring (Boot) application
  - [osgi-jaxrs](integration/osgi-jaxrs) - for implementing HAL web services using the [OSGi R7 JAX-RS Whiteboard](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html) and related [wcm.io Caravan](https://github.com/wcm-io-caravan) projects
  - [aem](integration/aem) - code and APIs for usage in Adobe Experience Manager (work in progress and not yet released)
- **Examples** that show how to use Rhyme in these frameworks:
  - [spring-hypermedia](examples/spring-hypermedia) - a well documented Spring Boot application with examples for most of the key concepts of the core framework
  - [spring-hello-world](examples/spring-hello-world) - a very simple Spring Boot application with minimal dependencies 
  - [osgi-jaxrs-example-service](examples/osgi-jaxrs-example-service) - an example service using reactive types in its API
  - [osgi-jaxrs-example-launchpad](examples/osgi-jaxrs-example-launchpad) - a [Sling launchpad](https://sling.apache.org/documentation/the-sling-engine/the-sling-launchpad.html) to start the OSGi/JAX-RS example service (and run some integration tests)
  - [aem-hal-browser](examples/aem-hal-browser) - an example project for AEM that shows how HAL resources can be implemented as Sling models (work in progress)
- [testing](testing) - additional integration testing support classes (to be used in test scope only)
- Maven **Tooling**
  - [coverage](tooling/coverage) - Maven module to generate aggregated code coverage reports
  - [docs-maven-plugin](tooling/docs-maven-plugin) - Maven Plugin to generate and embed HTML API docs from annotated interfaces
  - [parent](tooling/parent) - common parent POM used by most other modules

Another example for usage with Spring Boot can be found at https://github.com/feffef/reactive-hal-spring-example.

## Build from sources

If you want to build wcm.io from sources make sure you have configured the `OSS Sonatype Snapshots` repository in your `~/.m2/settings.xml` file. See the CI build's [Maven Settings](.maven-settings.xml) for an example with a full configuration.

Using **JDK 8, 11 or 17** and **Apache Maven 3.6.3** (or higher) you should then be able to build all modules (and run the integration tests) from the root directory:

```
mvn clean install
```

If this build fails on your machine, please open an issue on github or [Jira](https://wcm-io.atlassian.net/projects/WCARAV/) and include the full stack trace after running the Maven build again with the `-e` switch.

# Key concepts explained

## Define a HAL API with annotated interfaces

As an example, here is the HAL+JSON entry point resource of a simple web service that provides access to a database of simple generic items:

```json
{
  "_links":{
    "self":{
      "href":"https://hal-api.example.org/",
      "title":"The entry point for this example HAL API"
    },
    "item":{
      "href":"https://hal-api.example.org/items/{id}",
      "templated":true,
      "title":"A link template to retrieve the item with the specified id from the database"
    },
    "first":{
      "href":"https://hal-api.example.org/items",
      "title":"A pageable list of all available items in the database"
    }
  }
}
```

And this is how the corresponding Java interface looks like:

```java
  @HalApiInterface
  public interface ApiEntryPoint extends LinkableResource {

    @Related("item")
    ItemResource getItemById(@TemplateVariable("id") String id);

    @Related("first")
    PageResource getFirstPage();
  }
```
- [@HalApiInterface](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/HalApiInterface.java) is just a marker annotation that helps the framework identify the relevant interfaces through reflection.
- By extending [LinkableResource](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/resources/LinkableResource.java) we define that this resource is directly accessible through a URL (which will be found in the `self` link).
- The `getItemById` function corresponds to the link template with `item` relation, and the parameter annotated with [@TemplateVariable](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/TemplateVariable.java) indicates that you must provide an `id` parameter. It will be used to expand the link template into the final URL that will be used to retrieve the item resource.
- A HAL API should allow to discover all available data whenever possible. That's why there is also a `getFirstPage` function which allows you to start browsing all available items using the `first` link.

The return type of these functions are again annotated java interfaces. They describe the structure and available relations of the linked resources:

```java
  @HalApiInterface
  public interface PageResource extends LinkableResource {

    @Related("item")
    Stream<ItemResource> getItemsOnPage();

    @Related("next")
    Optional<PageResource> getNextPage();
  }
```

```java
  @HalApiInterface
  public interface ItemResource extends LinkableResource {

    @ResourceState
    Item getState();

    @Related("related")
    Stream<ItemResource> getRelatedItems();
  }
```
- Note that none of these interfaces define anything regarding the URL structure of these resources. This matches the [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS) principle that clients should only need to know a single URL (of the entry point). All other URLs should be discoverable through links in the resources.
- Methods annotated with [@Related](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) are used to define all possible relations / links between resources.
- `Stream` is used as return type whenever where there may be multiple links with the same relation. If you don't like Streams you can use `List` instead.
- `Optional` is used when it is not guaranteed that a link will be present (e.g. on the last page, there will be no `next` link).
- The method annotated with [@ResourceState](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) finally returns the actual data structure containing the core properties of an item.

### Representing JSON resource state

As return type for the `@ResourceState` method, you could either use a [Jackson](https://github.com/FasterXML/jackson) `ObjectNode` or any other type that can be  parsed from and serialized to JSON using the default jackson `ObjectMapper`. Using generic JSON types in your API is preferred if you are forwarding JSON resources from an external source, and those JSON resources' structure is expected to be extended frequently.

If you want to provide a strongly **typed** API to your consumers, you should define simple classes that match the JSON structure of your resources' state. You shouldn't share any **code** with theses classes, so a simple struct-like class like this works well:

```java
  public class Item {

    public String id;
    public String title;
  }
```
If you don't like this style with public mutable fields, you can define the class with private fields and access methods or even use an interface. But be aware that instances of this class or interface will have to be deserialized with Jackson on the client side, so you may have to use annotations (e.g. `@JsonCreator`) that allow your resource state instances to be created from the parsed JSON.

If you do not have such (de)serializable domain classes in your project yet that you could use with `@ResourceState`, then you can also consider adding multiple methods (one for each JSON property) annotated with [@ResourceProperty](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceProperty.java) directly in your `@HalApiInterface`. This is also useful if a resource just has very few JSON properties, and you want to avoid creating a Java class to represent that JSON structure.

No matter which Java representation you prefer: in the end, an actual HAL+JSON resource that matches the `ItemResource` interface defined above would look like this:

```json
{
  "id":"2",
  "title":"Item #2",
  "_links":{
    "self":{
      "href":"https://hal-api.example.org/items/2",
      "title":"The item with id '2'",
      "name":"2"
    },
    "related":[
      {
        "href":"https://hal-api.example.org/items/12",
        "title":"The item with id '12'",
        "name":"12"
      }
    ]
  }
}
```

### Sharing API interfaces between services

Some may argue that providing such interfaces that define the API is a bad idea, as it encourages sharing out-of-band information about your API with your clients. However, any consumer needs to have some reasonable expectations about the available links, parameter types and data structures provided by your API to create reliable client code. The aim of those annotated interfaces is to specify exactly these kind of expectations on the API in a very concise way, using a machine-readable format. Exposing any implementation details (such as URL structures or even logic) however is avoided. 

If you don't like the idea of sharing the same interfaces in client and server code (as outlined in the next sections), then keep in mind that this is entirely optional. Your API will still be using plain HAL+JSON data structures, and nothing forces you to use the **Rhyme** framework and these interfaces on **both** sides. If you are just worried about introducing binary dependency issues, it's also an option to just copy the java sources for those interfaces between projects. There is absolutely no requirement that client and server are using the same interfaces to represent the API. You can also consume a HAL+JSON API written with a completely different technology, you'll just have to create your own interfaces with annotated methods for the API's link relations that you are using.

But especially as long as your team is the sole consumer of your API anyway, sharing these interfaces between your services will give you many benefits:
- your IDE is able to understand where a specific API method is actually used
- your IDE is able to find the server-side implementation(s) of every API method
- refactoring of your API before it is published (e.g. renaming relations or parameter names) is very easy and reliable

## Consuming HAL resources with Rhyme client proxies

When you have a set of interfaces that represent a HAL API, you can use the Rhyme framework to automatically create a client implementation of those interfaces. This is similar to the concepts of [Feign](https://github.com/OpenFeign/feign) or [retrofit](https://github.com/square/retrofit), but much better suited to the HAL concepts (as for example methods are mapped to **relations** rather then endpoints, and no URL patterns are being exposed in the interfaces).

It just requires two lines of code to create a client implementation of your HAL API's entry point interface:

```java
  // create a HalApiClient that uses a default HTTP implementation
  HalApiClient client = HalApiClient.create();

  // create a dynamic proxy that knows how to fetch the entry point from the given URL.
  ApiEntryPoint api = rhyme.getRemoteResource("https://hal-api.example.org", ApiEntryPoint.class);
```

Note: If you are also using Rhyme to **render** your resources, you shouldn't use `HalApiClient` directly, but call the `Rhyme#getRemoteResource` method instead,
which has the exact same signature and behaviour. This ensures that the same `HalApiClient` instance will be used throught your incoming request, which
allows some caching and collection of performance metrics as explained in a later section.

Using the proxy instance of your entry point you can easily navigate through all resources of the API by simply calling the methods defined in your interfaces: 
```java
    // calling a method on the proxy will fetch the entry point, and then find and expand the URI template.
    ItemResource itemResource = api.getItemById("foo");
    
    // now you have a ItemResource that knows the full URL of the resource (and how to fetch it),
    // but again that resource is only actually fetched when you call a method on the resource proxy
    Item foo = itemResource.getState();

    // You can call another method on the same resource instance (without any resource being fetched twice),
    // and use Stream operations to fetch multiple related resources with a simple expression:
    List<Item> relatedToFoo = itemResource.getRelatedItems()
        .map(ItemResource::getState)
        .collect(Collectors.toList());
```
The Rhyme client proxy instances will take care of
- fetching and parsing the resource
- finding the links (or embedded resources) that correspond to the method being called (based on the relations defined in the interface methods' annotations)
- expanding link templates with the parameters from the method invocation
- automatically fetching further linked resources as required (as soon as any method on a related resource instance is called)
- converting the resource JSON state to the corresponding Java type
- keeping track of all resources that have been retrieved
 
A local in-memory caching will ensure that each resource is not fetched more than once, and repeated calls to the same method (with the same parameters) return a cached value immediately (as long as you are using the same `Rhyme` or `HalApiClient` instance).

### Using a custom HTTP client implementation

By default the HTTP requests will be executed using the JDK's `HttpURLConnection` class with default configuration. In many cases you will need to have more control over
executing the HTTP request (e.g. add authentication), and use a more sophisticated HTTP client library that is already used in your project or framework.

The execution of any HTTP requests by the **Rhyme** framework is fully customizable through the [HalResourceLoader](core/src/main/java/io/wcm/caravan/rhyme/api/spi/HalResourceLoader.java) SPI interface. 

The interface just consists of a single method that will load a HAL resource from a given URL, and return an RxJava Single which emits a [HalResponse](core/src/main/java/io/wcm/caravan/rhyme/api/common/HalResponse.java) object when the response has been retrieved (or fail with a [HalApiClientException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiClientException.java) if this wasn't possible)

```java
Single<HalResponse> getHalResource(String uri);
```

You can implement this interface completely by yourself, but this will require you to also implement the JSON parsing and exception handling according to the expectations of the framework. 

A simpler way is to implement the callback-style [HttpClientSupport](core/src/main/java/io/wcm/caravan/rhyme/api/spi/HttpClientSupport.java)
interface, and then use the [HalResourceLoaderBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/client/HalResourceLoaderBuilder.java).

In both cases, you should extend the [AbstractHalResourceLoaderTest](testing/src/main/java/io/wcm/caravan/rhyme/testing/client/AbstractHalResourceLoaderTest.java) 
(from the testing module module) to test your implementation against a Wiremock server. These unit tests ensure that all expectations regarding response and error handling are met.

The `HalResourceLoaderBuilder` also has further methods to enable persistent caching of responses which are explained in a later section.


## Rendering HAL resources in your web service 

For the server-side implementation of your HAL API, you will have to implement the annotated API interfaces you've defined before. You can then use the [Rhyme](core/src/main/java/io/wcm/caravan/rhyme/api/Rhyme.java) facade to automatically render a HAL+JSON representation based on the annotations found in the interfaces.

What's important to note is that **you should create a single `Rhyme` instance** for each incoming request: 

```java
    // create a single Rhyme instance as early as possible in the request cycle 
    Rhyme rhyme = RhymeBuilder.create().buildForRequestTo(incomingRequest.getUrl());
    
    // instantiate your server-side implementation of the requested @HalApiInterface resource
    ApiEntryPoint entryPoint = new ApiEntryPointImpl(database);
    
    // create the HAL+JSON representation (and response headers) for this resource
    HalResponse response = rhyme.renderResponse(entryPoint).blockingGet();

    // finally convert that response to your framework's representation of a web/JSON response...
```

What `Rhyme#renderResponse` does is to scan your implementation class for methods from the annotated `@HalInterface` and **recursively** call all those methods:

- `#createLink()` is called to generate the `self` link directly
- `#getFirstPage()` is called to create a PageResource instance, and then `PageResource#createLink()` is called to create the link to it
- `#getItemById()` is called (with the `id` parameter being null, as the entry point should only contain a link template and no specific id is known yet), and then again `ItemResource#createLink()` is called on the implementation instance being returned (to actually create the link template)

### Implementing HAL API interfaces on the server-side

Here's how the server-side implementation of the `ApiEntryPoint` interface could look like:

```java
  class ApiEntryPointImpl implements ApiEntryPoint {

    private final ItemDatabase database;

    ApiEntryPointImpl(ItemDatabase database) {
      this.database = database;
    }

    @Override
    public PageResource getFirstPage() {
      return new PageResourceImpl(database, 0);
    }

    @Override
    public ItemResource getItemById(String id) {
      return new ItemResourceImpl(database, id);
    }

    @Override
    public Link createLink() {

      return new Link("https://hal-api.example.org/")
          .setTitle("The entry point for this example HAL API");
    }
  }
```

Note that the implementation of the `@Related` methods look exactly the same as if you were implementing a normal Java service interface. This ensures that all consumer code running in the same JVM could use your implementation directly through the same interfaces that external clients are using. That would avoid the overhead of http requests and JSON (de)serialization for internal consumers.

Having the same interfaces on the server- and client-side allows the following approach when designing a larger software system:
- You can start with keeping everything in the same JVM, but separate the code into modules that are using `@HalApiInterface`s as internal API from the beginning.
- This encourages that your modules are only sharing data structures and common IDs, but not share any service implementations or other dependencies
- During development you can easily expose these internal APIs through HTTP using the **Rhyme** framework (even though your other modules are still using the services directly). This can for eaxample be helpful for inspecting data sources in detail without using a debugger.
- You can still refactor everything with full IDE support during development, and continuously verify that the API is designed well.
- When there is an actual reason to break up your system into multiple services, you can easily do so. As the interfaces for remote access via HAL+API are exactly the same as for internal consumers, you can keep much of the existing code. 

The [Spring Hypermedia Example](/examples/spring-hypermedia) contains some integration tests which show that the behaviour of that example's API is exactly the same for internal consumers and external client.

### Additional best practices

There are a few more concepts and best practices you need to stick to when implementing your server-side resources. Here is another example to explain what you need to be aware of:

```java
  class ItemResourceImpl implements ItemResource {

    // all dependencies and request parameters required to render the resource will be provided
    // when the instance is created. We are using constructor injection here, but you can also
    // use whatever IoC injection mechanism is available in the framework of your choice.
    private final ItemDatabase database;

    // be aware that 'id' can be null (e.g. if this resource is created/linked to from the entry point)
    private final String id;

    // your constructors should be as leight-weight as possible, as an instance of your
    // resource is created even if only a link to the resource is rendered
    ItemResourceImpl(ItemDatabase database, String id) {
      this.database = database;
      this.id = id;
    }

    // any I/O should only happen in the methods that are annotated with @Related
    // or @ResourceState which are being called when the resource is actually rendered
    // (and it's guaranteed that the 'id' parameter is set)
    @Override
    public Item getState() {
      return database.getById(id);
    }

    // to generate links to related resources, you'll simply instantiate their resource
    // implementation classes with the right 'id' parameter. This also ensures the method
    // is perfectly usable when called directly by an internal consumer.
    @Override
    public Stream<ItemResource> getRelatedItems() {

      return database.getIdsOfItemsRelatedTo(id).stream()
          .map(relatedId -> new ItemResourceImpl(database, relatedId));
    }

    @Override
    public Link createLink() {

      // this method is the one and only location where all links to this resource are rendered.
      // This includes the generation of link templates (e.g. when 'id' is null)
      UriTemplate uriTemplate = UriTemplate.fromTemplate("https://hal-api.example.org/items/{id}");
      if (id != null) {
        uriTemplate.set("id", id);
      }
      Link link = new Link(uriTemplate.expandPartial());

      if (id != null) {
        // it's good practice to always provide a human readable 'title' attribute for the link,
        // as this will appear in tools such as the HAL browser
        link.setTitle("The item with id '" + id + "'");
        // for machines, you should also always set a 'name' attribute to distinguish
        // multiple links with the same relations
        link.setName(id);
      }
      else {
        // especially link templates should always have a good description in title, as these
        // are likely to appear in the entry point of your resource, and will help to make
        // your API self-explaining
        link.setTitle("A link template to retrieve the item with the specified id from the database");
      }

      return link;
    }
  }

```

As you can see, link generation is quite complex even for this simple example. This is due to the fact that the `#createLink()` method of a resource implementation is responsible to render **all** possible variations of links and link templates to this kind of resource. The benefit of this approach is that the link generation code is not cluttered all over your project. Instead it can all be found in exactly the same class that will be using the parameters encoded in the links.

To keep your individual resource implementations simple, you are likely to end up with something like a service-specific `LinkBuilder` class to avoid duplication of code and URLs. Since the best way to create links varies a lot depending on the web framework your are using, the core **Rhyme** framework does not try to provide or enforce a solution for this.

The [Spring integration module](/integration/spring) however does have additional classes to simplify link building, including support for URL fingerprinting as explained below.

## Writing Integration Tests

One main goal of the `Rhyme` framework is to allow writing extensive integration tests for your application that are easy to read and written entirely from the perspective of an external client.

Since the clients shouldn't make any assumptions on the full URL structures of an API's resources, your integration tests shouldn't do either. So instead of manually creating URLs to your resoures, fetching them and verifying that related link URLs are constructed correctly, the tests should check if **following** the links (and expanding link templates where neccessary) according to the HAL specifications will actually lead to the expected resource. This ensures that changes to the URL structure (which you can do at any time without breaking API compatibility) don't require any adjustments to your tests.

The [integration test](/examples/spring-hello-world/src/test/java/io/wcm/caravan/rhyme/examples/spring/helloworld/HelloWorldTest.java) for the [Spring Hello World example](/examples/spring-hello-world) shows how there is only a single URL being constructed manually (the URL of the entry point running on a random port). All other assertions are based on following links by calling methods on the dynamic proxies created by the `HalApiClient` instance.

Using `Rhyme` for integration tests of an existing API implementation is probably the best way to get comfortable with the concepts of Rhyme. You can start modelling your API as `HalApiInterface`s (beginning from the entry point), and then add additional tests and resource interfaces as required. It's a good way to test if your API works well with a client library which has certain expectations that may be different from the way that you are using the API.

If you notice some behaviour where you think the `HalApiClient` is not acting according to the specification or common conventions, then please open an issue (or of course a PR if you narrowed the problem down yourself). The same applies if there are some gaps that won't allow you to fully represent your API as annotated `HalApiInterface`s. 


## Embedded Resources

If you have many small resources in your API, there will be an additional overhead if each of them is retrieved individually. To avoid this, the HAL+JSON format specifies a way to embed resources (in addition to link them) in a context resource.

Embedding resources with **Rhyme** is straight-forward: Simply make your server-side resource implementation classes also implement the [EmbeddableResource](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/resources/EmbeddableResource.java) interface.

If you don't override any of the default methods from the interface, the `Rhyme#renderResponse` method will automatically embed a complete representation of your resource wherever it is linked to (with a fully resolved URI, not for templated links). You can also override `EmbeddableResource#isEmbedded` to have more control over the process, and decide based on the context or configuration whether embedding a resource is beneficial to performance.

Proxy client implementations created with the **Rhyme** framework will also always look if any embedded resources are present when a method annotated with `@Related` is being called. If they find embedded resources for that relation, they will be used instead of following the corresponding links.

You can also create a nested hierarchy of embedded resources. This may for example be useful if you have a deep hierarchy tree of objects, where the leaf object require links to other resources be present. In those cases it may not always be reasonable to have a unique URI to access each resource in the tree individually. To achieve that, simply make your server-side resources **only** implement `EmbeddedResource` (but not `LinkableResource`). 

## Generation and Integration of HTML API documentation

Since the interfaces annotated with `@HalApiInterface` define the structure and relations of all resources in the API, they can also be used to generate a nice context-dependant documentation for your API. Using `curies` links, this documentation can be automatically integrated in tools such as the [HAL Browser](https://github.com/mikekelly/hal-browser) or [HAL Explorer](https://github.com/toedter/hal-explorer).

All you have to do to is to configure the [Rhyme Maven Documention Plugin](/tooling/docs-maven-plugin) in your project, and add Javadocs comments to your annotated interfaces and methods. The generated documenation will be deployed (and served) with your application, and is always guaranteed to be up to date with the current implementation on each environment.

## Cache-Control Header and URL Fingerprinting

The `cache-control: max-age` header is arguably the most useful way of controlling caching in a system of distributed stateless web services. It does not require clients to keep track of last-modified dates or Etags, and there is also good support for it in CDNs, browsers and caching proxies.

### Controlling the 'max-age' directive on the server

Within your server-side implementation, you can simply call `Rhyme#setResponseMaxAge(Duration)` at any time to set this cache header in the respose. If you call it multiple times, the lowest duration will be used. 

If you are building a service that is also fetching HAL+JSON responses from other services (which is the main use case for **Rhyme**), the `max-age` headers from these upstream responses should also be taken into account: If any of those responses are only to be cached for a short time, the derived response that you are creating must also not be cached any longer than that. Otherwise you'll run into issues that changes to these upstream resources won't become effective for your consumers. This will all happen automatically if you make sure to re-use the same `Rhyme` instance to fetch upstream resources and render your own response.

### Enabling client-side caching

To enable caching for your upstream requests executed with `Rhyme` or `HalApiClient`, you need to use the [HalResourceLoaderBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/client/HalResourceLoaderBuilder.java) and call either `#withMemoryCache` or `#withCustomCache` to create a single instance of a caching `HalResourceLoader` implementation. This instance should then be shared and re-used across your application by passing it to the `RhymeBuilder#withResourceLoader` or `HalApiClient#create` methods.

Any response retrieved by the **Rhyme** framework will then be stored in this cache, and following requests to the same URL will use the cached response instance. Again, the `max-age` cache directive will be taken into account:

- if the response in the cache is older than the `max-age` value, it it considered stale and will not be used. A fresh copy will be loaded (and again stored in the cache) 
- the 'max-age' value of responses taken from cache will be updated automatically: if an upstream response required by your resource had specified a max-age of 60 seconds, but was already requested and cached 55 seconds ago, calling `HalResponse#getMaxAge` on the cachged instance will return 5 seconds
- If you are using this cached response in a service that is also rendering HAL+JSON resources with Rhyme, loading this cached response with the adjusted max-age will automatically reduce the max-age of your own response: Since your response depends on data that is about to go stale in 5 seconds, your consumers shouldn't cache your response for longer than that either. 

By default only responses with status code 200 will be cached, and a default max-age of 60 seconds will be used if no such directive is found in the upstream response headers. You can override these defaults by providing your own implementation of [CachingConfiguration](core/src/main/java/io/wcm/caravan/rhyme/api/client/CachingConfiguration.java) to `HalResourceLoaderBuilder#withCachingConfiguration`.

### Using immutable resources and URL fingerprinting

For all this to work best, you should build your API with the following pattern:
- for the entry point resource to your HAL API you should set a short max-age value via `Rhyme#setResponseMaxAge(Duration)`
- you should ensure that the entry point resource is rendering quickly, as it will be requested often (since any interaction with your API starts with requesting the entry point)
- any links pointing to resources that are expensive to generate should contain some kind of fingerprint (e.g. a hash or timestamp) in the URL that changes whenever the data changes. 
- when a resource with such a fingerprint in the URL is rendered, you can set the max-age to a very high value as they are now essentially immutable (because if data changes, the clients will fetch them with a different URL instead)
- Consumers will now automatically "poll" the entry point repeatedly. But as long as the data (and therefore the URLs) doesn't change, they will continue to use the same fingerprinted URLs to fetch the more expensive resources (and there is a high chance that those can be found in cache)

Your consumers will not have to do anything to benefit from these immutable resoures, as the additional fingerprinting in your URLs is not exposed anywhere in your API. It's entirely up to the server-side implementation to decide for which links these fingerprints are added, and the clients will just pick it up by following the links.

The [SpringRhyme](integration/spring/src/main/java/io/wcm/caravan/rhyme/spring/api/SpringRhyme.java) integration has some built-in support for this via the [UrlFingerprinting](integration/spring/src/main/java/io/wcm/caravan/rhyme/spring/api/UrlFingerprinting.java) interface. To see it in action, check out the [examples/spring-hypermedia](examples/spring-hypermedia) module.

## Data debugging and performance analysis

There is another benefit of re-using the same `Rhyme` instance while handling an incoming request: Every upstream resource that is fetched, and every annotated method called by the framework will be tracked by the Rhyme instance.

When a HAL response is rendered, it can include a small embedded resource (using the `rhyme:metadata` relation) that allows you to inspect what exactly the framework did to generate the response. This is disabled by default, but for the existing integration modules (e.g. Spring, OSGi/JAX-RS) you can add a `embedRhymeMetadata` query parameter to any request to have this embedded resource included.

This resource will contain the following information:
- a list of `via` links to every HAL resource that was fetched from an upstream service. The `title` and `name` attributes from the original `self` links of those resources will be included as well, giving you a very nice overview which external resources were retrieved to generate your resource. This is super helpful to dive directly into the relevant source data of your upstream services.
- a sorted list of the measured response times for each of those upstream resources. This allows you to identify which upstream service may be slowing down your system
- a sorted list of the `max-age`headers for each of those upstream resources. This allows you to identify the reason why your own response's `max-age` may be lower as expected
- some extensive statistics about the time spent in method calls to your resource implementation classes, or the dynamic client proxies provided by the framework: 

```json
{
  "measurements": [
    "6.774 ms - sum of 50x calling #createLink of DelayableItemResourceImpl",
    "0.954 ms - sum of 2x calling #createLink of DelayableCollectionResourceImpl",
    "0.307 ms - 1x calling #renderLinkedOrEmbeddedResource with DelayableCollectionResourceImpl",
    "0.026 ms - 1x calling #getItems of DelayableCollectionResourceImpl",
    "0.024 ms - 1x calling #getState of DelayableCollectionResourceImpl",
    "0.018 ms - 1x calling #getAlternate of DelayableCollectionResourceImpl"
  ],
  "title": "A breakdown of time spent in blocking method calls by AsyncHalResourceRenderer"
}
```

While the overhead of using the **Rhyme** framework is usually neglible (especially compared to the latency introduced by external services), this information can be useful to identify hotspots that can be optimized (without firing up a profiler).

If you do have a section of yor own code that you suspect to be a hotspot for performance optimization, you can easily add your own metrics to the response metadata:

```
  class YourClass {

    private void doExpensiveStuffWith(Object param) {

      try (RequestMetricsStopwatch sw = rhyme.startStopwatch(YourClass.class, () -> "calls to #doExpensiveStuffWith(" + param + ")")) {
        // ... actually do expensive stuff with param
      }
    }
  }
```

This will make the **Rhyme** instance count the number of calls to your method, and sum up the overall execution time so you can check if it's worth optimizing this bit of code (or avoid that it's being called repeatedly). 

The advantage to using an external profiler is that execution times for the same section of code with different parameters can be distinguished, and only execution times from the current request are taken into account.


## Forwarding error information over service boundaries

Any runtime exceptions that are thrown by your implementation classes (or any **Rhyme** framework code) during the execution of `Rhyme#renderResponse` will be caught and handled: Instead of the regular HAL+JSON response with 200 status code, the `renderResponse` method will render a response with an appropriate status code, and a JSON body according to the [vnd.error+json](https://github.com/blongden/vnd.error) media type. This media type is just a very simple convention how error information is represented in a HAL+JSON compatible format, and will include the exception classes and messages of the whole exception chain.

The **status code** will be determined as follows:
- any errors while retrieving an upstream resource should lead to a [HalApiClientException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiClientException.java) from which the original status code can be extracted and by default is also used in your service's response
- this works well in many cases, but you may of course also catch those exception yourself (in your resource implementations), and either return some suitable fallback content or re-throw a [HalApiServerException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiServerException.java) with a different status code
- the Rhyme framework classes may also throw a [HalApiDeveloperException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiDeveloperException.java) that usually indicates that you did something wrong (and hopefully have a clear explanation, otherwise please open an issue). In this case a 500 status code is used.
- when you use the [RhymeBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/RhymeBuilder.java) to create your `Rhyme` instance you may also register a [ExceptionStatusAndLoggingStrategy](core/src/main/java/io/wcm/caravan/rhyme/api/spi/ExceptionStatusAndLoggingStrategy.java)) that can extract a suitable status code for any other exception (e.g. an exception used by your web framework)
- any other runtime exceptions will lead to a 500 status code

Some exceptions may also be thrown **before** you are calling `Rhyme#renderResponse`. You should try to catch those as well and call `Rhyme#renderVndErrorResponse(Throwable)` yourself to create a vnd.error response yourself.

The benefit of rendering this vnd.error response body for any error is that clients using the `Rhyme` framework will be able to parse this body, and include the error information in the [HalApiClientException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiClientException.java). If an upstream service fails, your service will not only render the information from the exception that has been caught, but also all information extracted from the vnd.error response of the upstream service. Even if you have multiple layers of remote services, your vnd.error response will have a neat list of all exception classes and messages, up to the original root cause on the remote server. This may sound trivial, but is very helpful to immediately understand what went wrong, without having to look into the logs of several external services.

The embedded `rhyme:metadata` resource with `via` links to all upstream resources that were loaded (until the error occured) is also present in the vnd.error resource. This can be useful to investigate the root cause as often a runtime exception is caused by unexpected data in an upstream service.

## Using reactive types in your API

Using asynchronous code to retrieve and render HAL resources can be desired if you have to deal with a lot of long-running requests. It will allow you to execute upstream requests in parallel without blocking your main request handling thread. But don't underestimate the increased complexity that comes with it.

If you want to keep your client and server-side code completely asynchronous and non-blocking, you start with using RxJava3 reactive types as return values throughout your API interfaces: 

```java
  @HalApiInterface
  public interface ReactiveResource extends LinkableResource {

    @ResourceState
    Single<Item> getState();

    @Related("related")
    Observable<ReactiveResource> getRelatedItems();

    @Related("parent")
    Maybe<ReactiveResource> getParentItem();
  }
```

- `Single<T>` is used (instead of `T`) whenever it's guaranteed that exactly one value is emitted
- `Observable<T>` is used (instead of `Stream<T>`) whenever multiple values can be emitted
- `Maybe<T>` is used (instead of `Optional<T>`) when a value may be present or not

If you rather want to use [Spring Reactor](https://projectreactor.io/) types (or types from othe RxJava versions), you can add support for that through the [HalApiReturnTypeSupport](core/src/main/java/io/wcm/caravan/rhyme/api/spi/HalApiReturnTypeSupport.java) SPI. You'll just need to implement a couple of functions that convert the additional types to/from RxJava3's `Observable`. You can register your return type extension before you create a `Rhyme` instance with the [RhymeBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/RhymeBuilder.java).

On the client side, you'll have to implement [HalResourceLoader](core/src/main/java/io/wcm/caravan/rhyme/api/spi/HalResourceLoader.java) using a fully asynchronous HTTP client library. 

Then you can use the full range of RxJava operators to construct a chain of API operations that are all executed lazily and asynchronously (and in parallel where possible):

```java
    ReactiveResource resource = rhyme.getRemoteResource("https://foo.bar", ReactiveResource.class);

    Single<List<Item>> parentsOfRelated = resource.getRelatedItems()
        .concatMapMaybe(ReactiveResource::getParentItem)
        .concatMapSingle(ReactiveResource::getState)
        .distinct(item -> item.id)
        .toList();

    // All Observables provided by Rhyme are *cold*, i.e. no HTTP requests would have been executed so far.
    // But they all will be executed (in parallel where possible) when you subscribe to the Single:

    List<Item> parentItems = parentsOfRelated.blockingGet();
```

On the server-side, just use the `renderResponse` function from the `Rhyme` interface, without calling `Single#blockingGet()` to ensure that your code is not blocking the main thread while rendering the HAL representation of your server-side resource implementations:

```java
Single<HalResponse> response = rhyme.renderResponse(resource);
```

Only when you subscribe to this `Single`, all implementation methods of your resource will be called and should return a Maybe, Single or Observable immediately. When all of these return values have emitted their results, the `HalResponse` will finally be constructed and emitted by the `Single`. 

# Related Links

Issues: https://wcm-io.atlassian.net/projects/WCARAV/<br/>
Continuous Integration: https://github.com/wcm-io-caravan/caravan-rhyme/actions<br/>
Commercial support: https://wcm.io/commercial-support.html
