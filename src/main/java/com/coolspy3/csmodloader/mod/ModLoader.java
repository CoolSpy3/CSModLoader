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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the loading and validation of mods
 */
public final class ModLoader
{

    private static final Logger logger = LoggerFactory.getLogger(ModLoader.class);

    public static final String[] BASIC_MOD_INFO = new String[] {"Mod Id", "Version"};
    public static final Function<Mod, String[]> MOD_INFO_MAP_FUNCTION =
            mod -> new String[] {mod.id(), mod.version()};
    private static boolean modsLoaded = false;
    private static ArrayList<Mod> modList = new ArrayList<>();

    /**
     * Attempts to load all mods in the "csmods" directory under {@link GameArgs#gameDir}.
     *
     * This method may only be called once (typically as part of the mod loader startup routine). It
     * imposes the following restrictions on loaded mods:
     *
     * 1) All classes annotated with {@link Mod} must implement {@link Entrypoint}
     *
     * 2) All mod definitions must be valid
     *
     * 3) No two mods may have the same id
     *
     * 4) All mods which require dependencies must have those dependencies met. Circular
     * dependencies are not allowed.
     *
     * 5) All classes annotated with {@link Mod} must provide a public constructor which takes 0
     * arguments.
     *
     * The exception is the mod definition provided by {@link Main} which does not have an
     * associated entrypoint or constructor, but is provided to allow this version of the loader to
     * be used as a dependency.
     *
     * If any of the above conditions are not met, this function will report the error to the error
     * log and the user and return {@code null}.
     *
     * All classes should be available at the time their &lt;clinit&gt; blocks are called, however,
     * mods should attempt to execute code in their constructors which are guaranteed to be called
     * such that mods' dependencies' constructors will be invoked first.
     *
     * When mods are initialized during the server connection phase, their {@code create} and
     * {@code init} methods are guaranteed to be called in the same order as their constructors.
     *
     * @return The entrypoints of all loaded mods in the order in which they were loaded or
     *         {@code null} if an error occurred
     *
     * @see #loadJars(Path, File...)
     * @see #validateMod(Mod)
     */
    public static ArrayList<Entrypoint> loadMods()
    {
        synchronized (ModLoader.class)
        {
            if (modsLoaded) return null;

            modsLoaded = true;
        }

        logger.info("Loading Mods...");

        HashMap<Mod, Class<?>> mods = new HashMap<>();

        // Get all jar files
        File[] files = GameArgs.get().gameDir.toPath().resolve("csmods").toFile().listFiles(
                ((Predicate<File>) new FileNameExtensionFilter("Jar Files", "jar")::accept)
                        .and(File::isFile)::test);
        files = files == null ? new File[] {} : files;

        // Find all classes with a Mod declaration
        {
            try
            {
                Path tempDir = Files.createTempDirectory(null);
                tempDir.toFile().deleteOnExit();

                for (Class<?> c : loadJars(tempDir, files))
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
                logger.error("Error loading mod file!", e);

                Utils.safeCreateAndWaitFor(() -> new TextAreaFrame("Error loading mod file!", e));

                return null;
            }
        }

        // Ensure all mods implement Entrypoint
        {
            List<Mod> nonEntrypointMods = mods.keySet().stream()
                    .filter(mod -> !Entrypoint.class.isAssignableFrom(mods.get(mod)))
                    .collect(Collectors.toList());

            if (!nonEntrypointMods.isEmpty())
            {
                logger.error("The following mods do not implement Entrypoint:\n" + nonEntrypointMods
                        .stream().map(MOD_INFO_MAP_FUNCTION).map(info -> info[0] + ":" + info[1])
                        .collect(Collectors.joining("\n")));

                Utils.safeCreateAndWaitFor(
                        () -> new ListFrame("One or more mods do not implement Entrypoint!",
                                BASIC_MOD_INFO, nonEntrypointMods.stream()
                                        .map(MOD_INFO_MAP_FUNCTION).toArray(String[][]::new)));

                return null;
            }
        }

        // Validate mod definitions
        {
            List<Mod> invalidMods = mods.keySet().stream().filter(mod -> !validateMod(mod))
                    .collect(Collectors.toList());

            if (!invalidMods.isEmpty())
            {
                logger.error("The following mods are invalid:\n" + invalidMods.stream()
                        .map(MOD_INFO_MAP_FUNCTION).map(info -> info[0] + ":" + info[1])
                        .collect(Collectors.joining("\n")));

                Utils.safeCreateAndWaitFor(() -> new ListFrame("One or more mods are invalid!",
                        BASIC_MOD_INFO,
                        invalidMods.stream().map(MOD_INFO_MAP_FUNCTION).toArray(String[][]::new)));

                return null;
            }
        }

        // Require distinct mod ids
        if (mods.keySet().stream().map(Mod::id).distinct().count() != mods.size())
        {
            List<String> overlappingModIds =
                    mods.keySet().stream().map(Mod::id).collect(Collectors.toList());
            mods.keySet().stream().map(Mod::id).distinct().forEach(overlappingModIds::remove);

            List<Mod> overlappingMods =
                    mods.keySet().stream().filter(mod -> overlappingModIds.contains(mod.id()))
                            .collect(Collectors.toList());

            logger.error("The following mods have the same id:\n" + overlappingMods.stream()
                    .map(MOD_INFO_MAP_FUNCTION).map(info -> info[0] + ":" + info[1])
                    .collect(Collectors.joining("\n")));

            Utils.safeCreateAndWaitFor(() -> new ListFrame("One or more mods have the same id!",
                    BASIC_MOD_INFO,
                    overlappingMods.stream().map(MOD_INFO_MAP_FUNCTION).toArray(String[][]::new)));

            return null;
        }

        // Load mods (dependencies first)

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
                                logger.error("Error loading mod: " + mod.id() + ":" + mod.version(),
                                        e);

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
        // If true, one or more mods was loaded during the previous iteration and more mods have to
        // be loaded. (ie. something can happen during the next iteration)
        while (numLoadedMods.intValue() != 0 && !unloadedMods.isEmpty());

        // One or more mods were missing dependencies
        if (unloadedMods.size() != 0)
        {
            Map<Mod, List<String>> missingDependencies =
                    unloadedMods.stream().collect(Collectors.toMap(Function.identity(),
                            mod -> getMissingDependenciesValidated(mod, loadedMods)));

            logger.error("Missing Dependencies:\n" + missingDependencies.entrySet().stream()
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

        logger.info("{} Mods loaded!", mods.size());

        modList = new ArrayList<>(mods.keySet());
        modList.add(loaderMod);

        return entrypoints;
    }

    /**
     * Checks whether all of a mods dependencies have been loaded. This method checks to make sure
     * that all provided mod definitions are valid
     *
     * @param mod The mod to check
     * @param loadedMods A list of all mods which have already been loaded
     * @return A list of all missing dependencies in the same format as {@link Mod#dependencies()}
     *
     * @throws IllegalArgumentException If one or more provided mods are invalid
     */
    public static List<String> getMissingDependencies(Mod mod, Collection<Mod> loadedMods)
            throws IllegalArgumentException
    {
        if (validateMod(mod) && loadedMods.stream().map(ModLoader::validateMod)
                .allMatch(Predicate.isEqual(true)))
            return getMissingDependenciesValidated(mod, loadedMods);

        throw new IllegalArgumentException("One or more mods are invalid!");
    }

    /**
     * Checks whether all of a mods dependencies have been loaded. This method does not check to
     * make sure that all provided mod definitions are valid
     *
     * @param mod The mod to check
     * @param loadedMods A list of all mods which have already been loaded
     * @return A list of all missing dependencies in the same format as {@link Mod#dependencies()}
     */
    private static List<String> getMissingDependenciesValidated(Mod mod, Collection<Mod> loadedMods)
    {
        return Stream.of(mod.dependencies()).filter(dependency -> {
            String[] dependencyParts = dependency.split(":");
            String dependencyId = dependencyParts[0];

            SemanticVersionRange dependencyVersionRange =
                    dependencyParts.length > 1 ? SemanticVersionRange.parse(dependencyParts[1])
                            : new SemanticVersionRange();

            return loadedMods.stream().noneMatch(loadedMod -> dependencyId.equals(loadedMod.id())
                    && dependencyVersionRange.contains(new SemanticVersion(loadedMod.version())));
        }).collect(Collectors.toList());
    }

    /**
     * Validates a mod definition.
     *
     * A mod definition is considered valid if:
     *
     * 1) {@link Mod#id()} contains only: alpha-numeric characters, "-", and "_"
     *
     * 2) {@link Mod#version()} is a valid {@link SemanticVersion}
     *
     * 3) All {@link Mod#dependencies()} are a valid modid followed by a colon and a valid
     * {@link SemanticVersionRange}
     *
     * @param mod The mod definition to validate
     * @return Whether the provided mod definition is valid
     *
     * @see SemanticVersion#validate(String)
     * @see SemanticVersionRange#validate(String)
     */
    public static boolean validateMod(Mod mod)
    {
        if (!mod.id().matches("[a-zA-Z0-9_\\-]+"))
        {
            logger.error("Invalid Mod Id: " + mod.id());

            return false;
        }

        if (!SemanticVersion.validate(mod.version()))
        {
            logger.error("Invalid Version: " + mod.version());

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
                logger.error("Invalid Dependency Specification: " + dependency);

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

    /**
     * Loads all of the provided mod jars and their contained classes. During this process, all jars
     * contained in the mods' META-INF/libraries directory are recursively extracted into
     * {@code tempDir}, loaded, and marked for deletion at the end of the program
     *
     * @param tempDir The directory in which to extract library jars
     * @param modFiles The jars of the mod files to load
     * @return An array containing the classes of all loaded mods and libraries
     *
     * @throws IOException If an I/O error occurs
     * @throws ModLoadingException If an error occurs while loading the mod classes
     *
     * @see #recursivelyLoadLibraries(List, Path)
     * @see File#deleteOnExit()
     */
    public static Class<?>[] loadJars(Path tempDir, File... modFiles)
            throws IOException, ModLoadingException
    {
        logger.info("Attempting to load {} jar files...", modFiles.length);

        logger.debug("Temp directory is: {}", tempDir);

        // Wrap in ArrayList to allow adding of new elements
        ArrayList<File> files = new ArrayList<>(Arrays.asList(modFiles));

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
                    logger.trace("Loading {}...", file);
                    for (JarEntry entry : Collections.list(jar.entries()))
                    {
                        try
                        {
                            String name = entry.getName();

                            if (entry.isDirectory() || !name.endsWith(".class")
                                    || loadedClasses.contains(name)
                                    || name.startsWith("META-INF/versions")) // Java 9+ class files
                                continue;

                            loadedClasses.add(name);

                            classes.add(cl.loadClass(
                                    name.substring(0, name.length() - 6).replace('/', '.')));
                        }
                        catch (Exception | NoClassDefFoundError e)
                        {
                            for (int i = 0; i < modFiles.length; i++)
                                if (modFiles[i] == file) throw e;
                        }
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
            catch (ModLoadingException exc)
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

    /**
     * Recursively extracts all jars in the provides jars' META-INF/libraries directory into the
     * provided directory and marks them for deletion at the end of the program.
     *
     * @param files The jar files to search
     * @param tempDir The directory in which to extract the found libraries
     * @return A list containing all of the extracted jars
     *
     * @throws IOException If an I/O error occurs
     *
     * @see File#deleteOnExit()
     */
    public static ArrayList<File> recursivelyLoadLibraries(List<File> files, Path tempDir)
            throws IOException
    {
        if (files.size() == 0) return new ArrayList<>();

        ArrayList<File> libraries = new ArrayList<>();

        for (File file : files)
            try (JarFile jar = new JarFile(file))
            {
                for (JarEntry entry : Collections.list(jar.entries()))
                    if (entry.getName().matches("\\QMETA-INF/libraries/\\E[^\\/]*\\.jar"))
                    {
                        logger.trace("Extracting Library: {}/!{}", file, entry);
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

    /**
     * @return The mod definitions of all mods which have been loaded.
     *
     * @see #loadMods()
     */
    public static ArrayList<Mod> getModList()
    {
        return new ArrayList<>(modList);
    }

    private ModLoader()
    {}

}
