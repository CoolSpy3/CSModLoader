package com.coolspy3.csmodloader.mod;

import java.util.Arrays;
import java.util.stream.Stream;

public class SemanticVersion implements Comparable<SemanticVersion>
{

    private final int[] version;

    public SemanticVersion(int[] version)
    {
        this.version = Arrays.copyOf(version, version.length);
    }

    public SemanticVersion(String version) throws IllegalArgumentException
    {
        if (!validate(version))
        {
            throw new IllegalArgumentException("Invalid Version: " + version);
        }

        this.version = Stream.of(version.split(".")).mapToInt(Integer::parseInt).toArray();
    }

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

        return Integer.compare(version.length, o.version.length);
    }

}
