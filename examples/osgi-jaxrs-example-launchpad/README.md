<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Rhyme - OSGi JAX-RS Launchpad
======

# Introduction

This module contains a provisioned [Sling Launchpad](https://sling.apache.org/documentation/the-sling-engine/the-sling-launchpad.html) 
which is used to run the [osgi-jaxrs-example-service](../osgi-jaxrs-example-service) in an Apache Felix OSGi container.

## Build an run integration tests

```mvn clean install````

## Build and keep launchpad running

```mvn clean install -Pkeep-running````

You can then load the HAL browser at http://localhost:8080/hal to navigate through the resources of the example service. 

Use the 'docs' icons in the HAL browser to see documentation generated with the [rhyme-docs-maven-plugin](../../rhyme-docs-maven-plugin)
