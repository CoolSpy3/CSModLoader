package com.coolspy3.csmodloader.mod;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.filechooser.FileNameExtensionFilter;

import com.coolspy3.csmodloader.GameArgs;

final class ModLoader {

    public static ArrayList<Entrypoint> loadMods() {
        HashMap<Mod, Class<?>> mods = new HashMap<>();
        try {
            for(File file: GameArgs.get().gameDir.listFiles(new FileNameExtensionFilter("Jar Files", "jar")::accept)) {
                if(file.isDirectory()) {
                    continue;
                }
                for(Class<?> c: loadJar(file)) {
                    Mod mod = c.getAnnotation(Mod.class);
                    if(mod != null) {
                        mods.put(mod, c);
                    }
                }
            }
        } catch(ClassNotFoundException | IOException e) {
            e.printStackTrace(System.err);
            // TODO: Report Error
            return null;
        }

        if(!mods.values().stream().map(c -> Entrypoint.class.isAssignableFrom(c)).allMatch(Predicate.isEqual(true))) {
            // TODO: Report Error
            System.err.println("One or more mods do not implement Entrypoint");
            return null;
        }

        if(!mods.keySet().stream().map(ModLoader::validateMod).allMatch(Predicate.isEqual(true))) {
            // TODO: Report Error
            return null;
        }

        if(mods.keySet().stream().map(Mod::id).distinct().count() != mods.size()) {
            List<String> modIds = mods.keySet().stream().map(Mod::id).collect(Collectors.toList());
            mods.keySet().stream().map(Mod::id).distinct().forEach(modIds::remove);
            String overlappingId = modIds.get(0);
            System.err.println("Multiple mods are installed with id: " + overlappingId + "!");
            // TODO: Report Error
            return null;
        }

        ArrayList<Entrypoint> entrypoints = new ArrayList<>();
        ArrayList<Mod> loadedMods = new ArrayList<>();
        CopyOnWriteArrayList<Mod> unloadedMods = new CopyOnWriteArrayList<>(mods.keySet());
        AtomicInteger numLoadedMods = new AtomicInteger();
        do {
            numLoadedMods.set(0);
            try {
                unloadedMods.stream().filter(mod -> getMissingDependenciesValidated(mod, loadedMods).isEmpty()).forEach(mod -> {
                    numLoadedMods.incrementAndGet();
                    try {
                        entrypoints.add((Entrypoint)mods.get(mod).newInstance());
                    } catch(Exception e) {
                        // TODO: Report Error
                        System.err.println("Error loading mod: " + mod.id());
                        e.printStackTrace(System.err);
                        throw new RuntimeException(e);
                    }
                    loadedMods.add(mod);
                    unloadedMods.remove(mod);
                });
            } catch(RuntimeException e) {
                return null;
            }
        } while(numLoadedMods.intValue() != 0 && !unloadedMods.isEmpty());

        if(unloadedMods.size() != 0) {
            // TODO: Report Error
            System.err.println("Missing Dependencies!");
            return null;
        }

        return entrypoints;
    }

    public static List<String> getMissingDependencies(Mod mod, Collection<Mod> loadedMods) {
        if(validateMod(mod) && loadedMods.stream().map(ModLoader::validateMod).allMatch(Predicate.isEqual(true))) {
            return getMissingDependenciesValidated(mod, loadedMods);
        }
        throw new IllegalArgumentException("One or more mods are invalid!");
    }

    private static List<String> getMissingDependenciesValidated(Mod mod, Collection<Mod> loadedMods) {
        return Stream.of(mod.dependencies()).filter(((Predicate<String>)dependency -> {
            String[] dependencyParts = dependency.split(":");
            String dependencyId = dependencyParts[0];
            SemanticVersionRange dependencyVersionRange = dependencyParts.length > 1 ?
                SemanticVersionRange.parse(dependencyParts[1]) : new SemanticVersionRange();
            return loadedMods.stream().filter(loadedMod -> dependencyId.equals(loadedMod.id()) &&
                dependencyVersionRange.contains(new SemanticVersion(loadedMod.version()))).count() > 0;
        }).negate()).collect(Collectors.toList());
    }

    public static boolean validateMod(Mod mod) {
        if(!mod.id().matches("[a-zA-Z0-9_\\-]+")) {
            System.err.println("Invalid Mod Id: " + mod.id());
            return false;
        }
        if(!SemanticVersion.validate(mod.version())) {
            System.err.println("Invalid Version: " + mod.version());
            return false;
        }
        /* Matches:
           modid
           modid:version
           modid:(minVersionExclusive,)
           modid:(,maxVersionExclusive)
           modid:[minVersionInclusive,)
           modid:(,maxVersionInclusive]
           modid:(minVersionExclusive,maxVersionExclusive)
           modid:[minVersionInclusive,maxVersionExclusive)
           modid:(minVersionExclusive,maxVersionInclusive]
           modid:[minVersionInclusive,maxVersionInclusive]
        */
        if(!Stream.of(mod.dependencies()).map(dependency -> {
                if(!dependency.matches("[a-zA-Z0-9_\\-]+(?::[0-9,\\Q.()[]\\E]+)?")) {
                    System.err.println("Invalid Dependency Specification: " + dependency);
                    return false;
                }
                if(dependency.contains(":") && !SemanticVersionRange.validate(dependency.split(":")[1])) {
                    return false;
                }
                return true;
            }).allMatch(Predicate.isEqual(true))) {
            return false;
        }
        return true;
    }

    public static Class<?>[] loadJar(File file) throws ClassNotFoundException, IOException {
        ArrayList<Class<?>> classes = new ArrayList<>();
        try(JarFile jar = new JarFile(file);
            URLClassLoader cl = new URLClassLoader(new URL[] {new URL("jar:file:" + file.getCanonicalPath() + "!/")})) {
            for(JarEntry entry: Collections.list(jar.entries())) {
                String name = entry.getName();
                if(entry.isDirectory() || !name.endsWith(".class")) {
                    continue;
                }
                classes.add(cl.loadClass(name.substring(0, name.length()-6)));
            }
            return classes.toArray(new Class[0]);
        }
    }

    private ModLoader() {}

}
