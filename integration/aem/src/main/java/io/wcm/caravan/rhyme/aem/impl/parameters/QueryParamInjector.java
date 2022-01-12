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
package io.wcm.caravan.rhyme.aem.impl.parameters;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.parameters.QueryParam;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;

@Component(property = Constants.SERVICE_RANKING + ":Integer=4000", service = { Injector.class, StaticInjectAnnotationProcessorFactory.class })
public class QueryParamInjector implements Injector, StaticInjectAnnotationProcessorFactory, AcceptsNullName {

  public static final String NAME = "query-parameters";

  private static final Converter CONVERTER = Converters.standardConverter();

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element,
      DisposalCallbackRegistry callbackRegistry) {

    if (!(adaptable instanceof SlingRhyme)) {
      return null;
    }

    if (!element.isAnnotationPresent(QueryParam.class)) {
      return null;
    }

    SlingHttpServletRequest request = ((SlingRhyme)adaptable).adaptTo(SlingHttpServletRequest.class);


    Field field = getField(element);

    if (!request.getRequestParameterMap().containsKey(name)) {
      return getValueForMissingParam(field);
    }

    RequestParameter[] values = request.getRequestParameterMap().getValues(name);

    return convertValues(field, values);
  }

  private static Object convertValues(Field field, RequestParameter[] values) {

    String[] stringValues = Stream.of(values)
        .map(RequestParameter::toString)
        .toArray(String[]::new);

    try {

      // note that there is a known issue here where strings cannot be converted to int
      // with converter versions < 1.0.10 and JDK > 11
      // https://issues.apache.org/jira/browse/FELIX-6157
      return CONVERTER.convert(stringValues).to(field.getGenericType());
    }
    catch (ConversionException ex) {
      String valueString = StringUtils.join(stringValues, ",");
      throw new HalApiServerException(HttpStatus.SC_BAD_REQUEST, "Invalid value '" + valueString + "' for parameter '" + field.getName()
          + "' (a value of type " + field.getType().getSimpleName() + " was expected)", ex);
    }
  }

  private static Object getValueForMissingParam(Field field) {

    if (Collection.class.isAssignableFrom(field.getType())) {
      return CONVERTER.convert(new String[0]).to(field.getGenericType());
    }

    return null;
  }

  private static Field getField(AnnotatedElement element) {
    return (Field)element;
  }

  @Override
  public InjectAnnotationProcessor2 createAnnotationProcessor(AnnotatedElement element) {
    // check if the element has the expected annotation
    QueryParam annotation = element.getAnnotation(QueryParam.class);
    if (annotation != null) {
      return new QueryParamAnnotationProcessor(annotation);
    }
    return null;
  }

  private static class QueryParamAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

    private final QueryParam annotation;

    public QueryParamAnnotationProcessor(QueryParam annotation) {
      this.annotation = annotation;
    }

    @Override
    public InjectionStrategy getInjectionStrategy() {
      return InjectionStrategy.DEFAULT;
    }

    @Override
    public Boolean isOptional() {
      return true;
    }

    @Override
    public String getName() {
      // since null is not allowed as default value in annotations, the empty string means, the default should be
      // used!
      if (annotation.name().isEmpty()) {
        return null;
      }
      return annotation.name();
    }
  }


}
