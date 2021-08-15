/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.maven.plugins.rhymedocs.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaType;

public class AnnotationUtils {

  static boolean hasAnnotation(JavaAnnotatedElement element, Class<? extends Annotation> annotationClazz) {

    return element.getAnnotations().stream()
        .anyMatch(item -> item.getType().isA(annotationClazz.getName()));
  }

  static Method getMethod(JavaClass javaClazz, JavaMethod javaMethod, ClassLoader classLoader) {
    try {
      Class<?> clazz = loadClass(javaClazz, classLoader);

      Class[] paramTypes = javaMethod.getParameterTypes().stream()
          .map(paramClazz -> loadClass(paramClazz, classLoader))
          .toArray(Class[]::new);

      return clazz.getMethod(javaMethod.getName(), paramTypes);
    }
    catch (RuntimeException | NoSuchMethodException ex) {
      throw new RuntimeException("Unable to get method '" + javaClazz.getName() + "#" + javaMethod.getName(), ex);
    }
  }

  private static Class<?> loadClass(JavaType javaType, ClassLoader classLoader) {
    try {
      return classLoader.loadClass(javaType.getFullyQualifiedName());
    }
    catch (ClassNotFoundException ex) {
      throw new RuntimeException("Failed to load class " + javaType.getFullyQualifiedName(), ex);
    }
  }


}
