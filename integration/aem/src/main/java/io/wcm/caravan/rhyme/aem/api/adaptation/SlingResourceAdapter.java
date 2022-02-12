package io.wcm.caravan.rhyme.aem.api.adaptation;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;

/**
 * A sling model (directly adaptable from {@link SlingRhyme}) that provides a convenient way to navigate to related
 * resources and adapt them to your sling models implementing your {@link HalApiInterface}s. It defines a fluent API
 * with the following steps (that should be called in the given order)
 * <ol>
 * <li>(optional): Calling a <b>from*</b> method chooses a different context resource for the following <b>select*</b>
 * step.
 * If you don't call any of these methods, resources will be selected using the context resource being rendered by
 * the model class in which you are using the SlingResourceAdapter.</li>
 * <li>(required): Calling a <b>select*</b> method chooses the sling resource(s) that you want to adapt</li>
 * <li>(optional): Calling a <b>filter*</b> method allows to limit adaptation to resources that match the specified
 * criteria</li>
 * <li>(required): Calling an <b>adaptTo</b> method defines to which interface or sling model class you want these
 * resources to be adapted to. It returns a generic steps {@link PostAdaptationStage} instance that knows these types,
 * and will be used for the following steps</li>
 * <li>(optional): Calling a <b>with*</b> method allows you to modify the sling model instances after they have been
 * created. A common use-case is to override link properties.</li>
 * <li>(required): Calling a <b>get*</b> method will finally execute the adaption</li>
 * </ol>
 */
public interface SlingResourceAdapter {

  SlingResourceAdapter fromCurrentPage();

  SlingResourceAdapter fromParentPage();

  SlingResourceAdapter fromGrandParentPage();

  SlingResourceAdapter fromResourceAt(String path);


  SlingResourceAdapter select(Stream<Resource> resources);

  SlingResourceAdapter selectCurrentResource();

  SlingResourceAdapter selectContentResource();

  SlingResourceAdapter selectParentResource();

  SlingResourceAdapter selectChildResources();

  SlingResourceAdapter selectChildResource(String name);

  SlingResourceAdapter selectSiblingResource(String name);

  SlingResourceAdapter selectGrandChildResources();

  SlingResourceAdapter selectContainingPage();

  SlingResourceAdapter selectChildPages();

  SlingResourceAdapter selectGrandChildPages();

  SlingResourceAdapter selectContentOfCurrentPage();

  SlingResourceAdapter selectContentOfChildPages();

  SlingResourceAdapter selectContentOfChildPage(String name);

  SlingResourceAdapter selectContentOfGrandChildPages();

  SlingResourceAdapter selectResourceAt(String path);


  SlingResourceAdapter filter(Predicate<Resource> predicate);

  SlingResourceAdapter filterAdaptableTo(Class<?> adapterClazz);

  <T> SlingResourceAdapter filterAdaptableTo(Class<T> adapterClazz, Predicate<T> predicate);

  SlingResourceAdapter filterWithName(String resourceName);


  <I> PostAdaptationStage<I, I> adaptTo(Class<I> halApiInterface);

  <I, M extends I> PostAdaptationStage<I, M> adaptTo(Class<I> halApiInterface, Class<M> slingModelClass);


}
