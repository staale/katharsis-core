package io.katharsis.resource.registry;

import io.katharsis.locator.JsonServiceLocator;
import io.katharsis.resource.annotations.JsonApiResource;
import io.katharsis.resource.information.ResourceInformation;
import io.katharsis.resource.information.ResourceInformationBuilder;
import io.katharsis.resource.registry.repository.RelationshipEntry;
import io.katharsis.resource.registry.repository.ResourceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builder responsible for building an instance of ResourceRegistry.
 */
public class ResourceRegistryBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRegistryBuilder.class);

    private final JsonServiceLocator jsonServiceLocator;
    private final ResourceInformationBuilder resourceInformationBuilder;

    private final RepositoryEntryBuilderFacade repositoryEntryBuilder;

    public ResourceRegistryBuilder(JsonServiceLocator jsonServiceLocator, ResourceInformationBuilder resourceInformationBuilder) {
        this.jsonServiceLocator = jsonServiceLocator;
        this.resourceInformationBuilder = resourceInformationBuilder;
        this.repositoryEntryBuilder = new RepositoryEntryBuilderFacade(jsonServiceLocator);
    }

    /**
     * Scans all classes in provided package and finds all resources and repositories associated with found resource.
     *
     * @param packageName Package containing resources (models) and repositories.
     * @param serviceUrl  URL to the service
     * @return an instance of ResourceRegistry
     */
    public ResourceRegistry build(String packageName, @SuppressWarnings("SameParameterValue") String serviceUrl) {
        ClassLookup reflections;
        if (packageName != null) {
            String[] packageNames = packageName.split(",");
            reflections = new ClassLookupDefault(packageNames);
        } else {
            reflections = new ClassLookupDefault();
        }

        Set<Class<?>> jsonApiResources = reflections.getTypesAnnotatedWith(JsonApiResource.class);
        Set<ResourceInformation> resourceInformationSet = jsonApiResources.stream()
            .map(resourceInformationBuilder::build)
            .collect(Collectors.toSet());

        Set<RegistryEntry> registryEntries = new HashSet<>(resourceInformationSet.size());
        for (ResourceInformation resourceInformation : resourceInformationSet) {
            Class<?> resourceClass = resourceInformation.getResourceClass();

            ResourceEntry<?, ?> resourceEntry = repositoryEntryBuilder.buildResourceRepository(reflections, resourceClass);
            List<RelationshipEntry<?, ?>> relationshipEntries = repositoryEntryBuilder
            .buildRelationshipRepositories(reflections, resourceClass);

            registryEntries.add(new RegistryEntry(resourceInformation, resourceEntry, relationshipEntries));

        }

        ResourceRegistry resourceRegistry = new ResourceRegistry(serviceUrl);
        for (RegistryEntry registryEntry : registryEntries) {
            Class<?> resourceClass = registryEntry.getResourceInformation().getResourceClass();
            RegistryEntry registryEntryParent = findParent(resourceClass, registryEntries);
            registryEntry.setParentRegistryEntry(registryEntryParent);
            resourceRegistry.addEntry(resourceClass, registryEntry);
        }

        return resourceRegistry;
    }

    /**
     * Finds the closest resource, that is resource annotated with {@link JsonApiResource} annotation, in the class
     * inheritance hierarchy. If no resource parent is found, <i>null</i> is returned.
     *
     * @param resourceClass    information about the searched resource
     * @param registryEntries a set of available resources
     * @return resource's parent resource
     */
    private RegistryEntry findParent(Class<?> resourceClass, Set<RegistryEntry> registryEntries) {
        RegistryEntry foundRegistryEntry = null;
        Class<?> currentClass = resourceClass.getSuperclass();
        classHierarchy:
        while (currentClass != null && currentClass != Object.class) {
            for (RegistryEntry availableRegistryEntry : registryEntries) {
                if (availableRegistryEntry.getResourceInformation().getResourceClass().equals(currentClass)) {
                    foundRegistryEntry = availableRegistryEntry;
                    break classHierarchy;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return foundRegistryEntry;
    }
}
