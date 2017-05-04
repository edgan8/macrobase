package edu.stanford.futuredata.macrobase.analysis.summary.greedy;

import edu.stanford.futuredata.macrobase.analysis.summary.Explanation;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import edu.stanford.futuredata.macrobase.operator.Operator;

import java.util.*;
import java.util.function.DoublePredicate;

public class GreedySummarizer implements Operator<DataFrame, Explanation> {
    private String outlierColumn = "_OUTLIER";
    private DoublePredicate predicate = d -> d != 0.0;
    private List<String> attributes;

    private int[] attributeDistinctCount;
    private List<Map<String, Integer>> attributeEncodings;
    private List<Map<Integer, String>> attributeDecodings;

    public GreedySummarizer() {
        attributes = new ArrayList<>();
    }

    class PEComparator implements Comparator<PartialExplanation> {
        @Override
        public int compare(PartialExplanation o1, PartialExplanation o2) {
            return -Double.compare(o1.getZScore(), o2.getZScore());
        }
    }
    @Override
    public void process(DataFrame input) {
        int n = input.getNumRows();
        int d = attributes.size();
        double[] outliers = input.getDoubleColumnByName(outlierColumn);

        boolean[] flag = new boolean[n];
        int numOutliers = 0;
        for (int i = 0; i < n; i++) {
            flag[i] = predicate.test(outliers[i]);
            if (flag[i]) {
                numOutliers++;
            }
        }
        int[][] values = encodeColumns(input);

        PriorityQueue<PartialExplanation> pq = new PriorityQueue<>(new PEComparator());
        PartialExplanation basePE = PartialExplanation.initialState(numOutliers, n);
        refineExplanation(
                values,
                flag,
                basePE,
                pq
        );

        PartialExplanation pe = pq.poll();
        for (int i = 0; i < 5; i++) {
            System.out.println(pe);
            System.out.println(pe.getStringMatcher(attributes, attributeDecodings));

            // Set up new context
            refineExplanation(values, flag, pe, pq);
            pe = pq.poll();
        }
    }

    private PriorityQueue<PartialExplanation> refineExplanation2(
            int[][] gvalues,
            boolean[] gflag,
            PartialExplanation base,
            PriorityQueue<PartialExplanation> basePQ
    ) {
        int d = attributes.size();
        int n = base.getMatchedTotal();
        int gN = gflag.length;
        Map<Integer, Integer> contextMatcher = base.getMatcher();
        ArrayList<Integer> baseIndices = new ArrayList<>(gN);
        for (int i = 0; i < gN; i++) {
            baseIndices.add(i);
        }
        for (Map.Entry<Integer, Integer> e : contextMatcher.entrySet()) {
            int colIdx = e.getKey();
            int colValue = e.getValue();
            int[] colValues = gvalues[colIdx];
            ArrayList<Integer> newIndices = new ArrayList<>(baseIndices.size());
            for (int i : baseIndices) {
                if (colValues[i] == colValue) {
                    newIndices.add(i);
                }
            }
            baseIndices = newIndices;
        }


        for (int j = 0; j < d; j++) {
            int[] curColumn = gvalues[j];
            int C = attributeDistinctCount[j];
            int[] outlierCounts = new int[C];
            int [] totalCounts = new int[C];
            for (int i : baseIndices) {
                int curValue = curColumn[i];
                totalCounts[curValue]++;
                if (gflag[i]) {
                    outlierCounts[curValue]++;
                }
            }

            for (int i = 0; i < C; i++) {
                if (outlierCounts[i] > 0 && totalCounts[i] > 0) {
                    PartialExplanation pe = new PartialExplanation(
                            outlierCounts[i],
                            totalCounts[i],
                            j, i,
                            base
                    );
                    basePQ.add(pe);
                }
            }
        }

        return basePQ;
    }


    private PriorityQueue<PartialExplanation> refineExplanation(
            int[][] gvalues,
            boolean[] gflag,
            PartialExplanation base,
            PriorityQueue<PartialExplanation> basePQ
    ) {
        int[][] values;
        boolean[] flag;
        int d = attributes.size();
        int n = 0;

        Map<Integer, Integer> contextMatcher = base.getMatcher();
        if (contextMatcher.size() == 0) {
            n = gflag.length;
            values = gvalues;
            flag = gflag;
        } else {
            n = base.getMatchedTotal();
            values = new int[d][n];
            flag = new boolean[n];

            int newIdx = 0;
            int oldN = gflag.length;
            for (int i = 0; i < oldN; i++) {
                boolean match = true;
                for (Map.Entry<Integer, Integer> e : contextMatcher.entrySet()) {
                    int colIdx = e.getKey();
                    int colValue = e.getValue();
                    if (gvalues[colIdx][i] != colValue) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    flag[newIdx] = gflag[i];
                    for (int j = 0; j < d; j++) {
                        values[j][newIdx] = gvalues[j][i];
                    }
                    newIdx++;
                }
            }
            System.out.println("Copied: "+newIdx);
        }

        for (int j = 0; j < d; j++) {
            int[] curColumn = values[j];
            int C = attributeDistinctCount[j];
            int[] outlierCounts = new int[C];
            int [] totalCounts = new int[C];
            for (int i = 0; i < n; i++) {
                int curValue = curColumn[i];
                totalCounts[curValue]++;
                if (flag[i]) {
                    outlierCounts[curValue]++;
                }
            }

            for (int i = 0; i < C; i++) {
                if (outlierCounts[i] > 0 && totalCounts[i] > 0) {
                    PartialExplanation pe = new PartialExplanation(
                            outlierCounts[i],
                            totalCounts[i],
                            j, i,
                            base
                    );
                    basePQ.add(pe);
                }
            }
        }

        return basePQ;
    }

    public int[][] encodeColumns(DataFrame input) {
        int d = attributes.size();
        int n = input.getNumRows();
        int[][] values = new int[d][n];

        attributeEncodings = new ArrayList<>(d);
        attributeDecodings = new ArrayList<>(d);
        attributeDistinctCount = new int[d];

        for (int j = 0; j < d; j++) {
            int maxValue = 0;
            String colName = attributes.get(j);
            Map<String, Integer> encoding = new HashMap<>();
            Map<Integer, String> decoding = new HashMap<>();

            String[] strings = input.getStringColumnByName(colName);
            for (int i = 0; i < n; i++) {
                String curString = strings[i];
                Integer curValue = encoding.getOrDefault(curString, null);
                if (curValue == null) {
                    curValue = maxValue;
                    maxValue++;
                    encoding.put(curString, curValue);
                    decoding.put(curValue, curString);
                }
                values[j][i] = curValue;
            }

            attributeEncodings.add(encoding);
            attributeDecodings.add(decoding);
            attributeDistinctCount[j] = maxValue;
        }

        return values;
    }

    @Override
    public Explanation getResults() {
        return null;
    }

    public String getOutlierColumn() {
        return outlierColumn;
    }
    public void setOutlierColumn(String outlierColumn) {
        this.outlierColumn = outlierColumn;
    }
    public DoublePredicate getPredicate() {
        return predicate;
    }
    public void setPredicate(DoublePredicate predicate) {
        this.predicate = predicate;
    }
    public List<String> getAttributes() {
        return attributes;
    }
    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }
    public List<Map<String, Integer>> getAttributeEncodings() {
        return attributeEncodings;
    }
    public List<Map<Integer, String>> getAttributeDecodings() {
        return attributeDecodings;
    }
}
