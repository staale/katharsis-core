package io.katharsis.resource.registry;

import io.katharsis.errorhandling.mapper.ExceptionMapperProvider;
import io.katharsis.repository.ResourceRepository;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Created by staaleu on 5/11/15.
 */
public interface ClassLookup {
    Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation);

    <X> Set<Class<? extends X>> getSubTypesOf(Class<X> resourceRepositoryClass);
}
