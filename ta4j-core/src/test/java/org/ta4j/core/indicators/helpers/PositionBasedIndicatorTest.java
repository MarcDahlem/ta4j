/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2017 Marc de Verdelhan, 2017-2021 Ta4j Organization & respective
 * authors (see AUTHORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ta4j.core.indicators.helpers;

import org.junit.Before;
import org.junit.Test;
import org.ta4j.core.*;
import org.ta4j.core.indicators.AbstractIndicatorTest;
import org.ta4j.core.mocks.MockBarSeries;
import org.ta4j.core.num.Num;

import java.util.function.Function;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;
import static org.ta4j.core.num.NaN.NaN;

public class PositionBasedIndicatorTest extends AbstractIndicatorTest<Indicator<Num>, Num> {

    private BarSeries series;
    private BaseTradingRecord tradingRecord;
    private TestPositionIndicator positionIndicator;

    public PositionBasedIndicatorTest(Function<Number, Num> numFunction) {
        super(numFunction);
    }

    @Before
    public void setUp() {
        series = new MockBarSeries(numFunction);
        tradingRecord = new BaseTradingRecord(PositionBasedIndicatorTest.class.getSimpleName());
        positionIndicator = new TestPositionIndicator(series, tradingRecord);
    }

    @Test
    public void indicatorReturnNanIfNoTradeAvailable() {
        for (int index = series.getBeginIndex(); index <= series.getEndIndex(); index++) {
            assertEquals(NaN, positionIndicator.getValue(index));
            assertNull(positionIndicator.lastCalledEntryTrade);
            assertNull(positionIndicator.lastCalledExitTrade);
            assertEquals(-1, positionIndicator.lastCalledExitIndex);
            assertEquals(-1, positionIndicator.lastCalledEntryIndex);
            positionIndicator.reset();
        }
    }

    @Test
    public void indicatorReturnsCorrectEntryCalculations() {

        assertEquals(NaN, positionIndicator.getValue(0));
        assertNull(positionIndicator.lastCalledEntryTrade);
        assertNull(positionIndicator.lastCalledExitTrade);
        assertEquals(-1, positionIndicator.lastCalledExitIndex);
        assertEquals(-1, positionIndicator.lastCalledEntryIndex);
        positionIndicator.reset();
        tradingRecord.enter(1, NaN, NaN);

        for (int index = series.getBeginIndex() + 1; index <= series.getEndIndex(); index++) {

            Num actualValue = positionIndicator.getValue(index);
            assertEquals(positionIndicator.lastReturnedEntryNumber, actualValue);

            assertEquals(tradingRecord.getCurrentPosition().getEntry(), positionIndicator.lastCalledEntryTrade);
            assertEquals(index, positionIndicator.lastCalledEntryIndex);

            assertNull(positionIndicator.lastCalledExitTrade);
            assertEquals(-1, positionIndicator.lastCalledExitIndex);

            positionIndicator.reset();
        }
    }

    private class TestPositionIndicator extends PositionBasedIndicator {
        int lastCalledEntryIndex;
        Trade lastCalledEntryTrade;
        Num lastReturnedEntryNumber;

        int lastCalledExitIndex;
        Trade lastCalledExitTrade;
        Num lastReturnedExitNumber;

        public TestPositionIndicator(BarSeries series, BaseTradingRecord tradingRecord) {
            super(series, tradingRecord);
            reset();
        }

        @Override
        Num calculateLastTradeWasEntry(Trade entryTrade, int index) {
            lastCalledEntryTrade = entryTrade;
            lastCalledEntryIndex = index;
            lastReturnedEntryNumber = getBarSeries().numOf(Math.random());
            return lastReturnedEntryNumber;
        }

        @Override
        Num calculateLastTradeWasExit(Trade exitTrade, int index) {
            lastCalledExitTrade = exitTrade;
            lastCalledExitIndex = index;
            lastReturnedExitNumber = getBarSeries().numOf(Math.random());
            return lastReturnedExitNumber;
        }

        void reset() {
            lastCalledEntryIndex = -1;
            lastCalledEntryTrade = null;
            lastReturnedEntryNumber = null;

            lastCalledExitIndex = -1;
            lastCalledExitTrade = null;
            lastReturnedExitNumber = null;
        }
    }
}
