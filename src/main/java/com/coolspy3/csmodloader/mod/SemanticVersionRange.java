package com.coolspy3.csmodloader.mod;

/**
 * Represents a range of {@link SemanticVersion}s
 */
public class SemanticVersionRange
{

    public final SemanticVersion lowerBound;
    public final boolean lowerBoundExclusive;

    public final SemanticVersion upperBound;
    public final boolean upperBoundExclusive;

    /**
     * Generates a SemanticVersionRange which contains all versions
     */
    public SemanticVersionRange()
    {
        this.lowerBound = null;
        this.lowerBoundExclusive = true;
        this.upperBound = null;
        this.upperBoundExclusive = true;
    }

    /**
     * Generates a SemanticVersionRange which only contains the specified version
     *
     * @param version The version to include
     */
    public SemanticVersionRange(SemanticVersion version)
    {
        this.lowerBound = version;
        this.lowerBoundExclusive = false;
        this.upperBound = version;
        this.upperBoundExclusive = false;
    }

    /**
     * Generates a SemanticVersionRange which has the specified lower bound, but no upper bound
     *
     * @param lowerBoundExclusive Whether this SemanticVersionRange contains the lower bound
     * @param lowerBound The lower bound
     */
    public SemanticVersionRange(boolean lowerBoundExclusive, SemanticVersion lowerBound)
    {
        this.lowerBound = lowerBound;
        this.lowerBoundExclusive = lowerBoundExclusive;
        this.upperBound = null;
        this.upperBoundExclusive = true;
    }

    /**
     * Generates a SemanticVersionRange which has the specified upper bound, but no lower bound
     *
     * @param upperBound The upper bound
     * @param upperBoundExclusive Whether this SemanticVersionRange contains the upper bound
     */
    public SemanticVersionRange(SemanticVersion upperBound, boolean upperBoundExclusive)
    {
        this.lowerBound = null;
        this.lowerBoundExclusive = true;
        this.upperBound = upperBound;
        this.upperBoundExclusive = upperBoundExclusive;
    }

    /**
     * Generates a SemanticVersionRange which has the specified lower and upper bounds
     *
     * @param lowerBoundExclusive Whether this SemanticVersionRange contains the lower bound
     * @param lowerBound The lower bound
     * @param upperBound The upper bound
     * @param upperBoundExclusive Whether this SemanticVersionRange contains the upper bound
     */
    public SemanticVersionRange(boolean lowerBoundExclusive, SemanticVersion lowerBound,
            SemanticVersion upperBound, boolean upperBoundExclusive)
    {
        this.lowerBound = lowerBound;
        this.lowerBoundExclusive = lowerBoundExclusive;
        this.upperBound = upperBound;
        this.upperBoundExclusive = upperBoundExclusive;
    }

    /**
     * Checks whether this SemanticVersionRange contains the provided version
     *
     * @param version The version to check
     * @return Whether this SemanticVersionRange contains the provided version
     */
    public boolean contains(SemanticVersion version)
    {
        int lowerCheck = lowerBound == null ? -1 : lowerBound.compareTo(version);
        int upperCheck = upperBound == null ? 1 : upperBound.compareTo(version);

        return (lowerCheck == 0 ? !lowerBoundExclusive : lowerCheck < 0)
                && (upperCheck == 0 ? !upperBoundExclusive : upperCheck > 0);
    }

    /**
     * Attempts to parse the provided string as a SemanticVersionRange.
     *
     * @param range The string to parse
     * @return The SemanticVersionRange represented by the provided string
     *
     * @throws IllegalArgumentException If the provided string is invalid
     *
     * @see #validate(String)
     */
    public static SemanticVersionRange parse(String range) throws IllegalArgumentException
    {
        if (validate(range))
        {
            if (range.contains("(") || range.contains("["))
            {
                boolean lowerBoundExclusive = range.startsWith("(");
                boolean upperBoundExclusive = range.endsWith(")");

                String[] versions = range.substring(1, range.length() - 1).split(",");

                SemanticVersion lowerBound;

                if (versions[0].isEmpty()) lowerBound = null;
                else
                    lowerBound = new SemanticVersion(versions[0]);

                SemanticVersion upperBound;

                if (versions[1].isEmpty()) upperBound = null;
                else
                    upperBound = new SemanticVersion(versions[1]);

                return new SemanticVersionRange(lowerBoundExclusive, lowerBound, upperBound,
                        upperBoundExclusive);
            }
            else
            {
                return new SemanticVersionRange(new SemanticVersion(range));
            }
        }
        throw new IllegalArgumentException("Invalid Version Range: " + range);
    }

    /**
     * Checks whether the provided string represents a valid SemanticVersionRange.
     *
     * The string will be considered valid if:
     *
     * 1) It is a valid {@link SemanticVersion}
     *
     * or
     *
     * 2) It is formatted to look like any of the following:
     *
     * i) (a,)
     *
     * ii) (,b)
     *
     * iii) [a,)
     *
     * iv) (,b]
     *
     * v) (a,b)
     *
     * vi) [a,b)
     *
     * vii) (a,b]
     *
     * viii) [a,b]
     *
     * In this case, a and b must be valid {@link SemanticVersion}s. A parenthesis denotes that that
     * version is excluded and a square bracket that it is included in the resulting range.
     *
     * @param range The string to check
     *
     * @return Whether {@code range} is a valid SemanticVersionRange
     *
     * @see SemanticVersion#validate(String)
     */
    public static boolean validate(String range)
    {
        if (!range.matches("(?:[0-9\\.]+|[\\(\\[](?:[0-9\\.]+,(?:[0-9\\.]+)?|,[0-9\\.]+)[\\)\\]])"))
            return false;

        if (range.contains("(") || range.contains("["))
        {
            String versionRange = range.substring(1, range.length() - 1);

            if (versionRange.startsWith(","))
            {
                if (!versionRange.endsWith(",")
                        && !SemanticVersion.validate(versionRange.substring(1)))
                    return false;
            }
            else if (versionRange.endsWith(","))
            {
                if (!SemanticVersion.validate(versionRange.substring(0, versionRange.length() - 1)))
                    return false;
            }
            else
            {
                String[] versions = versionRange.split(",");

                if (!(SemanticVersion.validate(versions[0])
                        && SemanticVersion.validate(versions[1])))
                    return false;
            }
        }
        else
        {
            if (!SemanticVersion.validate(range)) return false;
        }

        return !(range.contains("[,") || range.contains(",]"));
    }

}
