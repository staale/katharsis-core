package io.katharsis.resource.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by staaleu on 5/11/15.
 */
public class ClassLookupDefault implements ClassLookup {

    private static final Logger log = LoggerFactory.getLogger(ClassLookupDefault.class);

    private final String[] searchPackages;
    private final List<? extends Class<?>> clazzes;

    public ClassLookupDefault(final String ... searchPackages) {
        this.searchPackages = searchPackages;
        final URLClassLoader urlClassLoader =
                (URLClassLoader) Thread.currentThread().getContextClassLoader();
        clazzes = Stream.of(urlClassLoader.getURLs())
                .flatMap(this::uriToEntries)
                .filter(this::includeEntry)
                .flatMap(name -> {
                    try {
                        return Stream.of(Class.forName(name.replace('/', '.').replace(".class", "")));
                    } catch (ClassNotFoundException e) {
                        log.warn("Failed to load class for entry {}", name, e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation) {
        return getClassStream()
                .filter(clazz -> clazz.isAnnotationPresent(annotation))
                .collect(Collectors.toSet());
    }

    private Stream<? extends Class<?>> getClassStream() {
        return clazzes.stream();
    }

    private final Stream<String> uriToEntries(final URL url) {
        try {
            final File file = new File(url.toURI());
            if (file.isDirectory()) {
                final Path p = file.toPath();
                return Files.walk(p).map(path -> {
                    final String relativePath = p.relativize(path).toString();
                    return relativePath;
                });
            } else {
                return new ZipFile(file).stream().map(ZipEntry::getName);
            }
        } catch (URISyntaxException | IOException e) {
            log.warn("Failed to open classpath entry {}", url, e);
            return Stream.empty();
        }
    }

    private boolean includeEntry(final String name) {
        if (!name.endsWith(".class")) {
            return false;
        }
        if (searchPackages == null || searchPackages.length == 0) {
            return true;
        } else {
            for (String searchPackage : searchPackages) {
                if (name.startsWith(searchPackage.replace('.', '/'))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public <X> Set<Class<? extends X>> getSubTypesOf(final Class<X> resourceRepositoryClass) {
        return getClassStream()
                .filter(clazz -> resourceRepositoryClass.isAssignableFrom(clazz))
                .map(clazz -> (Class<? extends X>) clazz)
                .collect(Collectors.toSet());
    }
}
