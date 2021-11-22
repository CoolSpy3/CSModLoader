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

        this.version = Stream.of(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
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

        int lengthComparison = Integer.compare(version.length, o.version.length);
        int[] remainder = lengthComparison < 0 ? o.version : lengthComparison > 0 ? version : null;

        if (remainder == null) return 0;

        for (int i = Math.min(version.length, o.version.length); i < remainder.length; i++)
            if (remainder[i] != 0) return lengthComparison;

        return 0;
    }

    public int[] getVersion()
    {
        return Arrays.copyOf(version, version.length);
    }

}
