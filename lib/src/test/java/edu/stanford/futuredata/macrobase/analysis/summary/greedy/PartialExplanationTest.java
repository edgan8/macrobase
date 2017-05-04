package edu.stanford.futuredata.macrobase.analysis.summary.greedy;

import org.junit.Test;

import static org.junit.Assert.*;

public class PartialExplanationTest {
    @Test
    public void testStatistics() {
        PartialExplanation base = PartialExplanation.initialState(20, 100);
        PartialExplanation pe = new PartialExplanation(10,20,0,0,base);
        assertTrue(pe.getRiskRatio() > 3.0);
        assertTrue(pe.getZScore() > 3.0);
    }
}