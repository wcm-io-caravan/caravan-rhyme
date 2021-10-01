package io.wcm.caravan.rhyme.spring.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Annotation required for test classes using the {@link SpringRhymeIntegrationTestExtension}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpringRhymeIntegrationTest {

  /**
   * @return the class annotated with {@link SpringBootApplication} that should be started
   */
  Class<?> applicationClass();

  /**
   * @return the fully qualified URI of the entry point to be loaded
   */
  String entryPointUri();
}
