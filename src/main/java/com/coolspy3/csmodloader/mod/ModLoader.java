package com.coolspy3.csmodloader.mod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.filechooser.FileNameExtensionFilter;

import com.coolspy3.csmodloader.GameArgs;
import com.coolspy3.csmodloader.Main;
import com.coolspy3.csmodloader.gui.ListFrame;
import com.coolspy3.csmodloader.gui.TextAreaFrame;
import com.coolspy3.csmodloader.util.Utils;
import com.coolspy3.csmodloader.util.WrapperException;

public final class ModLoader
{

    public static final String[] BASIC_MOD_INFO = new String[] {"Mod Id", "Version"};
    public static final Function<Mod, String[]> MODINFOMAPP_FUNCTION =
            mod -> new String[] {mod.id(), mod.version()};
    private static boolean modsLoaded = false;
    private static ArrayList<Mod> modList = new ArrayList<>();

    public static ArrayList<Entrypoint> loadMods()
    {
        if (modsLoaded) return null;

        modsLoaded = true;

        HashMap<Mod, Class<?>> mods = new HashMap<>();

        File[] files = GameArgs.get().gameDir.toPath().resolve("csmods").toFile().listFiles(
                ((Predicate<File>) new FileNameExtensionFilter("Jar Files", "jar")::accept)
                        .and(File::isFile)::test);
        files = files == null ? new File[] {} : files;

        {
            try
            {
                Path tempDir = Files.createTempDirectory(null);
                tempDir.toFile().deleteOnExit();

                for (Class<?> c : loadJar(tempDir, files))
                {
                    try
                    {
                        Mod mod = c.getAnnotation(Mod.class);

                        if (mod != null) mods.put(mod, c);
                    }
                    catch (ArrayStoreException e)
                    {
                        // Error finding class - Shouldn't occur with actual mod definitions
                    }
                }
            }
            catch (IOException | ModLoadingException e)
            {
                e.printStackTrace(System.err);

                Utils.safeCreateAndWaitFor(() -> new TextAreaFrame("Error loading mod file!", e));

                return null;
            }
        }

        {
            List<Mod> nonEntrypointMods = mods.keySet().stream()
                    .filter(mod -> !Entrypoint.class.isAssignableFrom(mods.get(mod)))
                    .collect(Collectors.toList());

            if (!nonEntrypointMods.isEmpty())
            {
                System.err.println("The following mods do not implement Entrypoint:\n"
                        + nonEntrypointMods.stream().map(MODINFOMAPP_FUNCTION)
                                .map(info -> info[0] + ":" + info[1])
                                .collect(Collectors.joining("\n")));

                Utils.safeCreateAndWaitFor(
                        () -> new ListFrame("One or more mods do not implement Entrypoint!",
                                BASIC_MOD_INFO, nonEntrypointMods.stream().map(MODINFOMAPP_FUNCTION)
                                        .toArray(String[][]::new)));

                return null;
            }
        }

        {
            List<Mod> invalidMods = mods.keySet().stream().filter(mod -> !validateMod(mod))
                    .collect(Collectors.toList());

            if (!invalidMods.isEmpty())
            {
                System.err.println("The following mods are invalid:\n" + invalidMods.stream()
                        .map(MODINFOMAPP_FUNCTION).map(info -> info[0] + ":" + info[1])
                        .collect(Collectors.joining("\n")));

                Utils.safeCreateAndWaitFor(() -> new ListFrame("One or more mods are invalid!",
                        BASIC_MOD_INFO,
                        invalidMods.stream().map(MODINFOMAPP_FUNCTION).toArray(String[][]::new)));

                return null;
            }
        }

        if (mods.keySet().stream().map(Mod::id).distinct().count() != mods.size())
        {
            List<String> overlappingModIds =
                    mods.keySet().stream().map(Mod::id).collect(Collectors.toList());
            mods.keySet().stream().map(Mod::id).distinct().forEach(overlappingModIds::remove);

            List<Mod> overlappingMods =
                    mods.keySet().stream().filter(mod -> overlappingModIds.contains(mod.id()))
                            .collect(Collectors.toList());

            System.err.println("The following mods have the same id:\n" + overlappingMods.stream()
                    .map(MODINFOMAPP_FUNCTION).map(info -> info[0] + ":" + info[1])
                    .collect(Collectors.joining("\n")));

            Utils.safeCreateAndWaitFor(() -> new ListFrame("One or more mods have the same id!",
                    BASIC_MOD_INFO,
                    overlappingMods.stream().map(MODINFOMAPP_FUNCTION).toArray(String[][]::new)));

            return null;
        }

        ArrayList<Entrypoint> entrypoints = new ArrayList<>();
        ArrayList<Mod> loadedMods = new ArrayList<>();
        CopyOnWriteArrayList<Mod> unloadedMods = new CopyOnWriteArrayList<>(mods.keySet());
        AtomicInteger numLoadedMods = new AtomicInteger();

        Mod loaderMod = Main.class.getAnnotation(Mod.class);
        loadedMods.add(loaderMod);

        do
        {
            numLoadedMods.set(0);
            try
            {
                unloadedMods.stream()
                        .filter(mod -> getMissingDependenciesValidated(mod, loadedMods).isEmpty())
                        .forEach(mod -> {
                            numLoadedMods.incrementAndGet();

                            try
                            {
                                entrypoints.add(
                                        (Entrypoint) mods.get(mod).getConstructor().newInstance());
                            }
                            catch (Exception e)
                            {
                                System.err.println(
                                        "Error loading mod: " + mod.id() + ":" + mod.version());

                                e.printStackTrace(System.err);

                                Utils.safeCreateAndWaitFor(() -> new TextAreaFrame(
                                        "Error loading mod: " + mod.id() + ":" + mod.version(), e));

                                throw new RuntimeException(e);
                            }

                            loadedMods.add(mod);
                            unloadedMods.remove(mod);
                        });
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
        while (numLoadedMods.intValue() != 0 && !unloadedMods.isEmpty());

        if (unloadedMods.size() != 0)
        {
            Map<Mod, List<String>> missingDependencies =
                    unloadedMods.stream().collect(Collectors.toMap(Function.identity(),
                            mod -> getMissingDependenciesValidated(mod, loadedMods)));

            System.err.println("Missing Dependencies:\n" + missingDependencies.entrySet().stream()
                    .map(entry -> entry.getKey().id() + ":" + entry.getKey().version() + ":\n"
                            + entry.getValue().stream().collect(Collectors.joining("\n")))
                    .collect(Collectors.joining("\n\n")));

            Utils.safeCreateAndWaitFor(() -> new ListFrame(
                    "The following mods are missing one or more dependencies!",
                    new String[] {"Dependent Id", "Dependent Version", "Dependency"},
                    missingDependencies.entrySet().stream().map(entry -> entry.getValue().stream()
                            .map(dependency -> new String[] {entry.getKey().id(),
                                    entry.getKey().version(), dependency})
                            .toArray(String[][]::new)).flatMap(Arrays::stream)
                            .toArray(String[][]::new)));

            return null;
        }

        modList = new ArrayList<>(mods.keySet());
        modList.add(loaderMod);

        return entrypoints;
    }

    public static List<String> getMissingDependencies(Mod mod, Collection<Mod> loadedMods)
    {
        if (validateMod(mod) && loadedMods.stream().map(ModLoader::validateMod)
                .allMatch(Predicate.isEqual(true)))
            return getMissingDependenciesValidated(mod, loadedMods);

        throw new IllegalArgumentException("One or more mods are invalid!");
    }

    private static List<String> getMissingDependenciesValidated(Mod mod, Collection<Mod> loadedMods)
    {
        return Stream.of(mod.dependencies()).filter(dependency -> {
            String[] dependencyParts = dependency.split(":");
            String dependencyId = dependencyParts[0];

            SemanticVersionRange dependencyVersionRange =
                    dependencyParts.length > 1 ? SemanticVersionRange.parse(dependencyParts[1])
                            : new SemanticVersionRange();

            return loadedMods.stream().filter(loadedMod -> dependencyId.equals(loadedMod.id())
                    && dependencyVersionRange.contains(new SemanticVersion(loadedMod.version())))
                    .count() == 0;
        }).collect(Collectors.toList());
    }

    public static boolean validateMod(Mod mod)
    {
        if (!mod.id().matches("[a-zA-Z0-9_\\-]+"))
        {
            System.err.println("Invalid Mod Id: " + mod.id());

            return false;
        }

        if (!SemanticVersion.validate(mod.version()))
        {
            System.err.println("Invalid Version: " + mod.version());

            return false;
        }
        /*
         * Matches: modid modid:version modid:(minVersionExclusive,) modid:(,maxVersionExclusive)
         * modid:[minVersionInclusive,) modid:(,maxVersionInclusive]
         * modid:(minVersionExclusive,maxVersionExclusive)
         * modid:[minVersionInclusive,maxVersionExclusive)
         * modid:(minVersionExclusive,maxVersionInclusive]
         * modid:[minVersionInclusive,maxVersionInclusive]
         */
        if (!Stream.of(mod.dependencies()).map(dependency -> {
            if (!dependency.matches("[a-zA-Z0-9_\\-]+(?::[0-9,\\Q.()[]\\E]+)?"))
            {
                System.err.println("Invalid Dependency Specification: " + dependency);

                return false;
            }

            if (dependency.contains(":")
                    && !SemanticVersionRange.validate(dependency.split(":")[1]))
                return false;

            return true;
        }).allMatch(Predicate.isEqual(true)))
        {
            return false;
        }

        return true;
    }

    public static Class<?>[] loadJar(Path tempDir, File... modFile)
            throws IOException, ModLoadingException
    {
        // Wrap in ArrayList to avoid UnsupportedOperationException
        ArrayList<File> files = new ArrayList<>(Arrays.asList(modFile));

        files.addAll(recursivelyLoadLibraries(files, tempDir));

        ArrayList<Class<?>> classes = new ArrayList<>();
        ArrayList<String> loadedClasses = new ArrayList<>();

        try (URLClassLoader cl =
                new URLClassLoader(
                        files.stream().map(Utils.wrap(File::getCanonicalPath))
                                .map(filename -> "jar:file:" + filename + "!/")
                                .map(Utils.wrap(URL::new)).toArray(URL[]::new),
                        ModLoader.class.getClassLoader()))
        {
            for (File file : files)
                try (JarFile jar = new JarFile(file))
                {
                    for (JarEntry entry : Collections.list(jar.entries()))
                    {
                        String name = entry.getName();

                        if (entry.isDirectory() || !name.endsWith(".class")
                                || loadedClasses.contains(name))
                            continue;

                        loadedClasses.add(name);

                        classes.add(cl
                                .loadClass(name.substring(0, name.length() - 6).replace('/', '.')));
                    }
                }
                catch (ClassNotFoundException | IOException | NoClassDefFoundError e)
                {
                    throw new ModLoadingException(
                            Utils.safe(file::getCanonicalPath, file.getAbsolutePath()), e);
                }

            return classes.toArray(new Class[0]);
        }
        catch (WrapperException e)
        {
            try
            {
                throw e.getCause();
            }
            catch (IOException exc)
            {
                throw exc;
            }
            catch (RuntimeException exc)
            {
                throw exc;
            }
            catch (Throwable t)
            {
                throw e;
            }
        }
    }

    public static ArrayList<File> recursivelyLoadLibraries(List<File> files, Path tempDir)
            throws IOException
    {
        if (files.size() == 0) return new ArrayList<>();

        ArrayList<File> libraries = new ArrayList<>();

        for (File file : files)
            try (JarFile jar = new JarFile(file))
            {
                for (JarEntry entry : Collections.list(jar.entries()))
                    if (entry.getName().matches("\\QMETA-INF/libraries/\\E.+\\.jar"))
                    {
                        File newFile = Files.createTempFile(tempDir, null, null).toFile();
                        newFile.deleteOnExit();
                        try (FileOutputStream os = new FileOutputStream(newFile))
                        {
                            os.getChannel().transferFrom(
                                    Channels.newChannel(jar.getInputStream(entry)), 0,
                                    Long.MAX_VALUE);
                        }

                        libraries.add(newFile);
                    }
            }

        libraries.addAll(recursivelyLoadLibraries(libraries, tempDir));

        return libraries;
    }

    public static ArrayList<Mod> getModList()
    {
        return new ArrayList<>(modList);
    }

    private ModLoader()
    {}

}
