package io.wcm.caravan.rhyme.aem.integration.impl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

import io.wcm.caravan.rhyme.aem.api.RhymeObject;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

final class RhymeObjects {

  private RhymeObjects() {
    // only static utility methods
  }

  static void injectIntoSlingModel(@NotNull Object slingModel, Supplier<SlingRhyme> rhymeSupplier) {

    List<Field> allFields = getAllFieldsAnnotatedWithRhymeObject(slingModel.getClass());

    if (!allFields.isEmpty()) {
      SlingRhyme slingRhyme = rhymeSupplier.get();

      for (Field field : allFields) {
        if (hasSlingRhymeType(field)) {
          writeFieldUnchecked(slingModel, field, slingRhyme);
        }
        else {
          Object adapted = slingRhyme.adaptTo(field.getType());
          if (adapted == null) {
            throw new HalApiDeveloperException(
                "Cannot inject " + field.getName() + " field, as " + field.getType() + " couldn't be adapted from " + slingRhyme.getClass().getSimpleName());
          }
          writeFieldUnchecked(slingModel, field, adapted);
        }
      }
    }
  }

  private static boolean hasSlingRhymeType(Field field) {

    return field.getType().isAssignableFrom(SlingRhymeImpl.class);
  }

  static List<Field> getAllFieldsAnnotatedWithRhymeObject(Class<?> clazz) {

    return getAllFields(clazz)
        .filter(field -> field.getAnnotation(RhymeObject.class) != null)
        .collect(Collectors.toList());
  }

  static void writeFieldUnchecked(Object instance, Field field, Object value) {
    try {
      FieldUtils.writeField(field, instance, value, true);
    }
    catch (IllegalAccessException | RuntimeException ex) {
      throw new HalApiDeveloperException(
          "Failed to inject instance of " + value.getClass().getName() + " into field " + field.getName() + " of class " + instance.getClass().getName(),
          ex);
    }
  }

  private static Stream<Field> getAllFields(Class<?> clazz) {

    if (clazz == null) {
      return Stream.empty();
    }

    Stream<Field> fieldsInThisClass = Stream.of(clazz.getDeclaredFields());
    Stream<Field> fieldsInSuperClass = getAllFields(clazz.getSuperclass());

    return Stream.concat(fieldsInThisClass, fieldsInSuperClass);
  }
}
