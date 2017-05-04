package edu.stanford.futuredata.macrobase.analysis.summary.greedy;

import java.util.*;

public class PartialExplanation {
    private int matchedOutliers;
    private int matchedTotal;
    private Map<Integer, Integer> matchFilter;
    private PartialExplanation parent = null;

    private int globalOutliers;
    private int globalTotal;
    private double riskRatio;
    private double zScore;

    public PartialExplanation(
            int mo, int mt, Map<Integer, Integer> matchFilter,
            PartialExplanation parent) {
        this.matchedOutliers = mo;
        this.matchedTotal = mt;
        this.matchFilter = matchFilter;
        this.parent = parent;

        if (parent != null) {
            this.globalOutliers = parent.getMatchedOutliers();
            this.globalTotal = parent.getMatchedTotal();
            this.riskRatio = calcRiskRatio();
            this.zScore = calcZScore();
        }
    }
    private static Map<Integer, Integer> mapSingleton(int matchIndex, int matchValue) {
        Map<Integer, Integer> matchFilter = new LinkedHashMap<>(1);
        matchFilter.put(matchIndex, matchValue);
        return matchFilter;
    }
    public PartialExplanation(int mo, int mt, int matchIndex, int matchValue, PartialExplanation parent) {
        this(mo, mt, mapSingleton(matchIndex, matchValue), parent);
    }
    public static PartialExplanation initialState(int go, int gt) {
        return new PartialExplanation(go, gt, new LinkedHashMap<>(), null);
    }

    public double getSupport() {
        return 1.0*matchedOutliers / globalOutliers;
    }
    private double calcRiskRatio() {
        return (1.0*matchedOutliers / matchedTotal)
                / (1.0*(globalOutliers - matchedOutliers) / (globalTotal - matchedTotal));
    }

    private double calcZScore() {
        double p_outlier = 1.0*globalOutliers / globalTotal;
        double e_matchedOutliers = matchedTotal * p_outlier;
        double s_matchedOutliers = Math.sqrt(matchedTotal * p_outlier * (1-p_outlier));
        return 1.0*(matchedOutliers - e_matchedOutliers) / s_matchedOutliers;
    }

    @Override
    public String toString() {
        if (parent == null) {
            return String.format("Base: %d/%d", matchedOutliers, matchedTotal);
        } else {
            String baseString = String.format(
                    "Zs:%f Rr:%f %d/%d",
                    zScore,
                    riskRatio,
                    matchedOutliers,
                    matchedTotal
            );
            return baseString + "\n| " + parent.toString();
        }
    }

    public Map<Integer, Integer> getMatcher() {
        Map<Integer, Integer> ms = new LinkedHashMap<>(matchFilter);
        if (parent != null) {
            ms.putAll(parent.getMatcher());
        }
        return ms;
    }
    public Map<String, String> getStringMatcher(List<String> colNames, List<Map<Integer, String>> decoder) {
        Map<String, String> ms = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : matchFilter.entrySet()) {
            int matchColumnIndex = e.getKey();
            int matchColumnValue = e.getValue();
            String columnName = colNames.get(matchColumnIndex);
            String columnValue = decoder.get(matchColumnIndex).get(matchColumnValue);
            ms.put(columnName, columnValue);
        }
        if (parent != null) {
            Map<String, String> parentMap = parent.getStringMatcher(colNames, decoder);
            ms.putAll(parentMap);
        }
        return ms;
    }

    public void setParent(PartialExplanation pe) {
        this.parent = pe;
    }
    public double getRiskRatio() {
        return riskRatio;
    }
    public double getZScore() {
        return zScore;
    }
    public int getMatchedOutliers() {
        return matchedOutliers;
    }
    public int getMatchedTotal() {
        return matchedTotal;
    }
    public int getGlobalOutliers() {
        return globalOutliers;
    }
    public int getGlobalTotal() {
        return globalTotal;
    }
    public Map<Integer, Integer> getMatchFilter() {
        return matchFilter;
    }
}
