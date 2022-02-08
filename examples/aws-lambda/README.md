<img src="https://wcm.io/images/favicon-16@2x.png"/> Rhyme - AWS Lambda Example
======

# Summary

This is an example project that shows how Rhyme can be used to implement a HAL+JSON API as an AWS Lambda function.

It implements a simple movie search functionality that is built on top of [Kai Toedter](https://github.com/toedter)'s Hypermedia Movie Demo:
* http://hypermedia-movies-demo.herokuapp.com/
* https://github.com/toedter/movies-demo

#  Explore

The example movie search API can be accessed directly at https://088zuopqd6.execute-api.eu-central-1.amazonaws.com/default or through the [HAL Explorer](https://toedter.github.io/hal-explorer/release/hal-explorer/#theme=Cosmo&uri=https://088zuopqd6.execute-api.eu-central-1.amazonaws.com/default)

The **Doc** icons in the HAL Explorer will link to an HTML documentation (which is automatically generated from the annotated API interfaces).

You can have a look at the embedded `rhyme:metadata` to get an idea of what's happening in the background. For example you can see that for search terms that have many hits from the top ranked movies, only the first page of movies from the upstream API has to be loaded:
* https://088zuopqd6.execute-api.eu-central-1.amazonaws.com/default/results?page=0&searchTerm=the

For search terms where there is no hit at all, you can see that all pages of movies had to be loaded before the (empty) result could be rendered:
* https://088zuopqd6.execute-api.eu-central-1.amazonaws.com/default/results?page=0&searchTerm=foo

Note that the first load of these resources is quite slow, as this may trigger a cold start of the lambda function, and the responses from the upstream resource are not yet available in the local in-memory cache. Subsequent searches will be much faster, as they will be using responses that are cached for up to one hour. 

The JVM will also further optimize the compiled code after repeated requests, so it's worth to reload the search results multiple times if you are interested in the performance details.


# Source Code

* the [API package](src/main/java/io/wcm/caravan/rhyme/examples/movies/api) defines a `@HalApiInterface` for each type of resource from the original Hypermedia Movie Demo API, and the new resources added by this example
* the [impl package](src/main/java/io/wcm/caravan/rhyme/examples/movies/impl) contains
  * a simple class that contains the search logic and all interaction with the upstream service (using streams of the annotated API interfaces)
  * two server-side resource implementation classes that render the links and embedded resources
  * an AWS Lambda `RequestHandler` that wires everything together
* [integration tests](src/test/java/io/wcm/caravan/rhyme/examples/movies/impl) show how the implementation is tested using either static responses from the file system, or with a dynamic stub implementation of the HAL API interfaces.
* the [awslambda folder](src/main/java/io/wcm/caravan/rhyme/awslambda) contains additional integration code that is not specific to this example (and will be moved to its own integration bundle in the future)
