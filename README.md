<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Rhyme
======
[![Build](https://github.com/wcm-io-caravan/caravan-rhyme/workflows/Build/badge.svg?branch=develop)](https://github.com/wcm-io-caravan/caravan-rhyme/actions?query=workflow%3ABuild+branch%3Adevelop)
[![Code Coverage](https://codecov.io/gh/wcm-io-caravan/caravan-rhyme/branch/develop/graph/badge.svg)](https://codecov.io/gh/wcm-io-caravan/caravan-rhyme)

wcm.io Caravan - Rhyme

![Caravan](https://github.com/wcm-io-caravan/caravan-tooling/blob/master/public_site/src/site/resources/images/caravan.gif)

# Introduction

**Rhyme** is a Java framework for providing or consuming RESTful APIs using the [HAL+JSON media format](http://stateless.co/hal_specification.html). It really shines when you need to do both, e.g. build a web service that aggregates data from one or more other HAL APIs.

Its core principles are
- HAL APIs are represented as type-safe **annotated Java interfaces**
- these interfaces are shared with the consumers, which can use them as a highly abstracted client API
- the same interfaces are also used to keep the server-side implementation well structured, and in sync with the published API

# Examples to understand the key concepts

## Representing a HAL API as annotated Java interfaces

As an example, here is the HAL entry point to a simple web service that provides access to a database of simple generic items that may be related to each other:

```
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

Here is how the corresponding Java interface looks like:

```
  @HalApiInterface
  public interface ApiEntryPoint extends LinkableResource {

    @Related("item")
    ItemResource getItemById(@TemplateVariable("id") String id);

    @Related("first")
    PageResource getFirstPage();
  }
```
- [@HalApiInterface](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/HalApiInterface.java) is just a marker interface that helps the framework identify the relevant interfaces through reflection
- by extending [LinkableResource](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/resources/LinkableResource.java) we define that this resource is directly accessible through a URL (which will be found in the `self` link)
- The `getItemById` function corresponds to the link template with `item` relation, and the parameter annotated with [@TemplateVariable](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/TemplateVariable.java) indicates that you must provide an `id` parameter that will be used to expand the link template into the final URL that will be used to retrieve the item resource
- Since a HAL API should allow to discover all available data whenever possible, there is also a `getFirstPage` function that allows you to start browsing all available items using the `first` link

The return type of these functions are again java interfaces that describe the structure and available relations of the linked resources:

````
  @HalApiInterface
  public interface PageResource extends LinkableResource {

    @Related("item")
    Stream<ItemResource> getItemsOnPage();

    @Related("next")
    Optional<PageResource> getNextPage();
  }
````

````
  @HalApiInterface
  public interface ItemResource extends LinkableResource {

    @ResourceState
    Item getState();

    @Related("related")
    Stream<ItemResource> getRelatedItems();
  }
````
- Note that none of these interfaces define anything regarding the URL structure of these resources. This matches the HAL/HATEOAS principle that client's only should have to know a single URL (of the entry point), and all other URLs should be found as links in the resources
- Again, methods annotated with [@Related](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) are used to model the relations / links between resources
- `Stream` is used as return type whenever where there may be multiple links with the same relation
- `Optional` is used when it is not guaranteed that a link will be present (e.g. on the last page, there will be no 'next' link)
- The method annotated with [@ResourceState](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) finally returns the actual data that represents an item 

As a return type for the @ResourceState method, you could either use a [jackson](https://github.com/FasterXML/jackson) `ObjectNode` or any other type that can be  parsed from and serialized to JSON using the default jackson `ObjectMapper`. If you want to provide a type-safe API to your consumers you should define simple classes that match the JSON structure. You shouldn't share any **code** with this classes, so a simple struct-like class like this works well:

````
  public class Item {

    public String id;
    public String title;
  }
````

An actual HAL resource that matches the `ItemResource` interface defined above looks like this:

````
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
````

## Rendering HAL resources in your web service 

For the server-side implementation of your HAL API, you will have to implement the annotated API interfaces you've defined before. You can then use the [Rhyme](core/src/main/java/io/wcm/caravan/rhyme/api/Rhyme.java) facade to automatically render a HAL+JSON representation based on the annotation found in the interfaces:

````
    // create a single Rhyme instance as early as possible in the request-cycle 
    Rhyme rhyme = RhymeBuilder.withoutResourceLoader().buildForRequestTo(incomingRequest.getUrl());
    
    // instantiate your server-side implementation of the @HalApiInterface resource that is requested, e.g.
    ApiEntryPoint entryPoint = new ApiEntryPointImpl(database);
    
    // create the HAL+JSON representation (and response headers) for this resource
    HalResponse response = rhyme.renderResponse(entryPoint);

    // finally convert that response to your framework's representation of a web/JSON response...
````

Here's what happens in the implementation class:

````
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
````

What [Rhyme#renderResponse] does is to scan your implementation class and **recursively** call all the annotated methods from the `@HalApiInterface`:

- `#createLink()` is called to generate the `self` link directly
- `#getFirstPage()` is called to create a PageResource instance, and then `#createLink()` is called on that resource to create the link to it
- `#getItemById()` is called (with the `id` parameter being null, as the entry point should only contain a link template and no specific id is known yet), and then again `#createLink()` is called on the implementation instance being returned (to actually create the link template)

## Related Links

Documentation: https://caravan.wcm.io/rhyme/<br/>
Issues: https://wcm-io.atlassian.net/<br/>
Wiki: https://wcm-io.atlassian.net/wiki/<br/>
Continuous Integration: https://github.com/wcm-io-caravan/caravan-rhyme/actions<br/>
Commercial support: https://wcm.io/commercial-support.html


## Build from sources

If you want to build wcm.io from sources make sure you have configured all [Maven Repositories](https://caravan.wcm.io/maven.html) in your settings.xml.

See [Maven Settings](https://github.com/wcm-io-caravan/caravan-rhyme/blob/develop/.maven-settings.xml) for an example with a full configuration.

Then you can build using

```
mvn clean install
```
