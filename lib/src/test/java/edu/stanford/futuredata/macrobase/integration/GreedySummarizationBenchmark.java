package edu.stanford.futuredata.macrobase.integration;

import edu.stanford.futuredata.macrobase.analysis.summary.ExplanationSimulator;
import edu.stanford.futuredata.macrobase.analysis.summary.greedy.GreedySummarizer;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;

public class GreedySummarizationBenchmark {
    public static void benchmark1() throws Exception {
        int n = 2000000;
        int d = 50;
        int k = 4;
        int[] cs = {3, 5, 20, 10};
        double p = 0.02;
        double pr = 0.7;

        ExplanationSimulator es = new ExplanationSimulator(n, d, k, cs);
        es.setP(p);
        es.setMatchPrecision(0.7);
        DataFrame df = es.generate();

        long startTime = System.currentTimeMillis();
        GreedySummarizer summ = new GreedySummarizer();
        summ.setAttributes(es.getAttrColumnNames());
        summ.process(df);
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println(elapsed);
    }

    public static void main(String[] args) throws Exception {
        benchmark1();
    }
}
