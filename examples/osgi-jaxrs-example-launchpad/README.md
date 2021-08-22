<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Rhyme - OSGi JAX-RS Launchpad
======

# Introduction

This module contains a provisioned [Sling Launchpad](https://sling.apache.org/documentation/the-sling-engine/the-sling-launchpad.html) 
which is used to run the [osgi-jaxrs-example-service](../osgi-jaxrs-example-service) in an Apache Felix OSGi container.

## Build an run integration tests

```mvn clean install```

This will run all integration tests defined [here](src/test/java/io/wcm/caravan/rhyme/osgi/it/tests).

These integration tests not only ensure that the resources itself are successfully generated. Because the tests itself are also using a 
`CaravanHalApiClient` to navigate through the resources, it is also ensured that the links between those resources are correct, and all content 
is properly parsed by Rhyme's dynamic client proxy code.

## Build and keep launchpad running

If you don't want to run the integration tests, but use the launchpad to *run* the example service, you can do this with another maven profile:

```mvn clean install -Pkeep-running```

You can then load the HAL browser at http://localhost:8080/hal to navigate through the resources of the example service. 

Use the 'docs' icons in the HAL browser to see documentation generated with the [rhyme-docs-maven-plugin](../../docs-maven-plugin)
