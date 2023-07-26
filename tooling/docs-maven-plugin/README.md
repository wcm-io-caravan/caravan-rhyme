<img src="https://wcm.io/images/favicon-16@2x.png"/> Rhyme Documentation Maven Plugin
======
[![Maven Central](https://img.shields.io/maven-central/v/io.wcm.caravan.maven.plugins/rhyme-docs-maven-plugin)](https://repo1.maven.org/maven2/io/wcm/caravan/maven/plugins/rhyme-docs-maven-plugin)
# Introduction

This plugin will generate HTML documentation from annotated HAL API Java interfaces, and embed them into the packaged jar so they are deployed and served together with your application.

It will use the Javadoc comments from the interfaces annotated with `HalApiInterface` in the current project to generate the content of the documentation.

You can run the [Spring Hypermedia Example](/examples/spring-hypermedia) to see how the documentation is automatically linked from the HAL resources and shows up in tools such as the HAL browser (via the `docs` icon that shows up for every link with a relation using a curie prefix). If you compare the source code of the API interfaces with the generated HTML documentation, you will see how they relate to each other.

Note that the documentation for a relation depends on the **context** of the resource where this it is used in. A link template with relation `foo:bar` can have different parameters (and also different meaning) in different resources, and the generated documentation considers this by documenting each link **relation** based on the Javadocs of the corresponding method annotated with `@Related`. The documentation of the resource/media **types** is taken directly from the Javadoc description of the interface annotated with `@HalApiInterface`.

# Usage

## Generating HTML documentation

Add the following to the pom.xml of the module that contains your interfaces annotated with `HalApiInterface`:

````
      <!-- Generate HTML documentation into classpath resources-->
      <plugin>
        <groupId>io.wcm.caravan.maven.plugins</groupId>
        <artifactId>rhyme-docs-maven-plugin</artifactId>
        <version>1.0.0</version>
        <executions>
          <execution>
            <goals>
              <goal>generate-rhyme-docs</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
````

This will ensure that all source files in the project will be scanned, and one HTML file for each interface will be written to the `target/generated-rhyme-docs` folder.

When a jar file is packaged for the project, these files should be added to a `RHYME-DOCS-INF` folder within the jar, where the integration modules for Spring, OSGI-JAX & AEM will find them.

However this doesn't work for more complicated maven builds. Sometimes it's required to add another copy-resources task to your project:

````
      <!-- Copy the HTML documentation into the classes folder so they are included in the JAR -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-resources-plugin</artifactId>
        <executions>
          <execution>
            <id>custom-resources</id>
            <phase>prepare-package</phase>
            <goals>
              <goal>copy-resources</goal>
            </goals>
            <configuration>
              <outputDirectory>${basedir}/target/classes/RHYME-DOCS-INF</outputDirectory>
              <resources>
                <resource>
                  <directory>target/generated-rhyme-docs</directory>
                  <filtering>false</filtering>
                </resource>
              </resources>
              <encoding>UTF-8</encoding>
            </configuration>
          </execution>
        </executions>
      </plugin>
````

If anyone knows how this configuration can be simplified then please open up a PR! Until then, if you are seeing 404 errors when following the curies links, you should double-check the content of your packaged jar, to see if the documentation exists at the expected location `/RHYME-DOCS-INF`.

## Integration in your service

Since the generated HTML documentation is contained in the JAR, it is meant to be served directly from within the application. This ensures that the documentation is matching the implementation on each environment.

To ensure that your rendered API responses contain valid CURIE links pointing to your documentation, an implementation of the [RhymeDocsSupport](core/src/main/java/io/wcm/caravan/rhyme/api/spi/RhymeDocsSupport.java) interface and a call to `RhymeBuilder#withRhymeDocsSupport` is required.

You also have to write some kind of controller to expose the documentation files from the class path at the base URL defined in your `RhymeDocsSupport` implementation.

But all this is usually done once in the integration module for your platform. For example if you are using OSGi / JAX-RS, this all happens in the [docs package](integration/osgi-jaxrs/src/main/java/io/wcm/caravan/rhyme/jaxrs/impl/docs), and as a service developer you don't need to do anything than configure the maven plugin.  
