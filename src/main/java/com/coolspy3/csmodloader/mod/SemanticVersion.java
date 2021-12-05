package com.coolspy3.csmodloader.mod;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Represents a semantic version as defined at <a href="https://semver.org/">https://semver.org/</a>
 * with the exception that only one major, minor, or patch version is required, and more may be
 * provided. (ex. 1, 1.0, 1.0.1, 1.2.3.4) are all valid SemanticVersions. In all cases, the leading
 * numbers will be treated as having higher precedence.
 */
public class SemanticVersion implements Comparable<SemanticVersion>
{

    private final int[] version;

    /**
     * Creates a semantic version with the provided version numbers
     *
     * @param version The version numbers to use
     */
    public SemanticVersion(int[] version)
    {
        this.version = Arrays.copyOf(version, version.length);
    }

    /**
     * Checks that the provided string is a valid SemanticVersion and parses it if it is.
     *
     * @param version The version string
     *
     * @throws IllegalArgumentException If the provided version string is invalid
     *
     * @see #validate(String)
     */
    public SemanticVersion(String version) throws IllegalArgumentException
    {
        if (!validate(version))
        {
            throw new IllegalArgumentException("Invalid Version: " + version);
        }

        this.version = Stream.of(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    /**
     * Checks if the provided string is a valid SemanticVersion.
     *
     * @param version The string to check
     * @return Whether the provided string is a valid SemanticVersion.
     */
    public static boolean validate(String version)
    {
        return version.matches("[0-9\\.]+") && !version.contains("..") && !version.startsWith(".")
                && !version.endsWith(".");
    }

    @Override
    public int compareTo(SemanticVersion o)
    {
        for (int i = 0; i < Math.min(version.length, o.version.length); i++)
        {
            int compare = Integer.compare(version[i], o.version[i]);

            if (compare != 0) return compare;
        }

        int lengthComparison = Integer.compare(version.length, o.version.length);
        int[] remainder = lengthComparison < 0 ? o.version : lengthComparison > 0 ? version : null;

        if (remainder == null) return 0;

        for (int i = Math.min(version.length, o.version.length); i < remainder.length; i++)
            if (remainder[i] != 0) return lengthComparison;

        return 0;
    }

    /**
     * @return The integer representation of this SemanticVersion
     */
    public int[] getVersion()
    {
        return Arrays.copyOf(version, version.length);
    }

}
