package com.coolspy3.csmodloader.mod;

public class SemanticVersionRange {

    public final SemanticVersion lowerBound;
    public final boolean lowerBoundExclusive;
    public final SemanticVersion upperBound;
    public final boolean upperBoundExclusive;

    public SemanticVersionRange() {
        this.lowerBound = null;
        this.lowerBoundExclusive = true;
        this.upperBound = null;
        this.upperBoundExclusive = true;
    }

    public SemanticVersionRange(SemanticVersion version) {
        this.lowerBound = version;
        this.lowerBoundExclusive = false;
        this.upperBound = version;
        this. upperBoundExclusive = false;
    }

    public SemanticVersionRange(boolean lowerBoundExclusive, SemanticVersion lowerBound) {
        this.lowerBound = lowerBound;
        this.lowerBoundExclusive = lowerBoundExclusive;
        this.upperBound = null;
        this. upperBoundExclusive = true;
    }

    public SemanticVersionRange(SemanticVersion upperBound, boolean upperBoundExclusive) {
        this.lowerBound = null;
        this.lowerBoundExclusive = true;
        this.upperBound = upperBound;
        this. upperBoundExclusive = upperBoundExclusive;
    }

    public SemanticVersionRange(boolean lowerBoundExclusive, SemanticVersion lowerBound, SemanticVersion upperBound, boolean upperBoundExclusive) {
        this.lowerBound = lowerBound;
        this.lowerBoundExclusive = lowerBoundExclusive;
        this.upperBound = upperBound;
        this. upperBoundExclusive = upperBoundExclusive;
    }

    public boolean contains(SemanticVersion version) {
        int lowerCheck = lowerBound.compareTo(version);
        int upperCheck = upperBound.compareTo(version);
        return (lowerCheck == 0 ? !lowerBoundExclusive : lowerCheck < 0) && (upperCheck == 0 ? !upperBoundExclusive : upperCheck > 0);
    }

    public static SemanticVersionRange parse(String range) throws IllegalArgumentException {
        if(validate(range)) {
            if(range.contains("(") || range.contains("[")) {
                boolean lowerBoundExclusive = range.startsWith("(");
                boolean upperBoundExclusive = range.endsWith(")");
                String[] versions = range.substring(1, range.length()-1).split(",");
                SemanticVersion lowerBound;
                if(versions[0].isEmpty()) {
                    lowerBound = null;
                } else {
                    lowerBound = new SemanticVersion(versions[0]);
                }
                SemanticVersion upperBound;
                if(versions[1].isEmpty()) {
                    upperBound = null;
                } else {
                    upperBound = new SemanticVersion(versions[1]);
                }
                return new SemanticVersionRange(lowerBoundExclusive, lowerBound, upperBound, upperBoundExclusive);
            } else {
                return new SemanticVersionRange(new SemanticVersion(range));
            }
        }
        throw new IllegalArgumentException("Invalid Version Range: " + range);
    }

    public static boolean validate(String range) {
        if(!range.matches("(?:[0-9\\.]+|[\\(\\[](?:[0-9\\.]+,(?:[0-9\\.]+)?|,[0-9\\.]+)[\\)\\]])")) {
            return false;
        }
        if(range.contains("(") || range.contains("[")) {
            String versionRange = range.substring(1, range.length()-1);
            if(versionRange.startsWith(",")) {
                if(!versionRange.endsWith(",") && !SemanticVersion.validate(versionRange.substring(1))) {
                    return false;
                }
            } else if(versionRange.endsWith(",")) {
                if(!SemanticVersion.validate(versionRange.substring(0, versionRange.length()-1))) {
                    return false;
                }
            } else {
                String[] versions = versionRange.split(",");
                if(!(SemanticVersion.validate(versions[0]) && SemanticVersion.validate(versions[1]))) {
                    return false;
                }
            }
        } else {
            if(!SemanticVersion.validate(range)) {
                return false;
            }
        }
        return !(range.contains("[,") || range.contains(",]"));
    }

}
