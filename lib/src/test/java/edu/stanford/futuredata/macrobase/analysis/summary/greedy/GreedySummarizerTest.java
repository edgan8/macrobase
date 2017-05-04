package edu.stanford.futuredata.macrobase.analysis.summary.greedy;

import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class GreedySummarizerTest {
    @Test
    public void testEncodeColumns() throws Exception {
        DataFrame df = new DataFrame();
        String[] s1 = {"hello", "world"};
        df.addStringColumn("a1", s1);
        df.addStringColumn("a2", s1);

        GreedySummarizer summ = new GreedySummarizer();
        summ.setAttributes(Arrays.asList("a1", "a2"));
        int[][] encoded = summ.encodeColumns(df);
        int[] expected = {0,1};
        assertArrayEquals(expected, encoded[0]);
        assertArrayEquals(expected, encoded[1]);
    }
}