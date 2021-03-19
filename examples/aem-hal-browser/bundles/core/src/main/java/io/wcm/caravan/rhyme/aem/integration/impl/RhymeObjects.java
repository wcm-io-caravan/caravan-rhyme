package io.wcm.caravan.rhyme.aem.integration.impl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;

public class RhymeObjects {

  static void injectIntoSlingModel(@NotNull Object slingModel, Supplier<SlingRhyme> rhymeSupplier) {

    List<Field> allFields = getAllFieldsAnnotatedWithRhymeObject(slingModel.getClass());

    if (!allFields.isEmpty()) {
      SlingRhyme slingRhyme = rhymeSupplier.get();

      for (Field field : allFields) {
        if (hasSlingRhymeType(field)) {
          writeFieldUnchecked(slingModel, field, slingRhyme);
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

  private static void writeFieldUnchecked(Object instance, Field field, Object value) {
    try {
      FieldUtils.writeField(field, instance, value, true);
    }
    catch (IllegalAccessException ex) {
      throw new RuntimeException(
          "Failed to inject new instance of " + value.getClass().getName() + " into field " + field.getName() + " of class " + instance.getClass().getName(),
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
