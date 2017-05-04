package edu.stanford.futuredata.macrobase.analysis.summary;

import edu.stanford.futuredata.macrobase.datamodel.DataFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExplanationSimulator {
    private int n;
    private int d;
    private int k;
    private int[] cs = {2};
    private double p = 0.0;
    private double matchPrecision = 1.0;

    private long seed = 0;

    private List<String> attrColumnNames;

    public ExplanationSimulator(int n, int d, int k, int[] cs) {
        this.n = n;
        this.d = d;
        this.k = k;
        this.cs = cs;
    }

    public DataFrame generate() {
        initialize();
        Random r = new Random(seed);

        String[][] attrs = new String[d][n];
        double[] isOutlier = new double[n];

        String[][] attrPrimitiveValues = new String[d][];
        for (int j = 0; j < d; j++) {
            int curC = cs[j];
            attrPrimitiveValues[j] = new String[curC];
            for (int i = 0; i < curC; i++) {
                attrPrimitiveValues[j][i] = String.format("a%d:%d", j, i);
            }
        }

        for (int i = 0; i < n; i++) {
            int[] attrValues = new int[d];
            for (int j = 0; j < d; j++) {
                attrValues[j] = r.nextInt(cs[j]);
                attrs[j][i] = attrPrimitiveValues[j][attrValues[j]];
            }

            // Outliers arise from random noies
            // and also from matching key combination after the "event" occurs
            boolean curOutlier = r.nextFloat() < p;
            boolean match = true;
            for (int j = 0; j < k; j++) {
                if (attrValues[j] != 1) {
                    match = false;
                }
            }
            if (match) {
                curOutlier = (r.nextFloat() < matchPrecision);
            }
            isOutlier[i] = curOutlier ? 1.0 : 0.0;
        }

        attrColumnNames = new ArrayList<>(d);
        DataFrame df = new DataFrame();
        for (int j = 0; j < d; j++) {
            String curColName = "a"+j;
            df.addStringColumn(curColName, attrs[j]);
            attrColumnNames.add(curColName);
        }
        df.addDoubleColumn("_OUTLIER", isOutlier);
        return df;
    }
    private void initialize() {
        if (cs.length < d) {
            int[] oldcs = cs;
            cs = new int[d];
            for (int i = 0; i < d; i++) {
                cs[i] = oldcs[i%oldcs.length];
            }
        }
    }

    public List<String> getAttrColumnNames() {
        return this.attrColumnNames;
    }

    public void setN(int n) {
        this.n = n;
    }
    public void setD(int d) {
        this.d = d;
    }
    public void setK(int k) {
        this.k = k;
    }
    public void setCs(int[] cs) {
        this.cs = cs;
    }
    public void setCs(List<Integer> cs) {
        this.cs = new int[cs.size()];
        for (int i = 0; i < cs.size(); i++) {
            this.cs[i] = cs.get(i);
        }
    }
    public void setP(double p) {
        this.p = p;
    }
    public void setMatchPrecision(double match_precision) {
        this.matchPrecision = match_precision;
    }
    public void setSeed(long seed) {
        this.seed = seed;
    }
}
