/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014-2017 Marc de Verdelhan, 2017-2021 Ta4j Organization & respective
 * authors (see AUTHORS)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ta4jexamples.strategies;

import static org.ta4j.core.indicators.helpers.TransformIndicator.minus;
import static org.ta4j.core.indicators.helpers.TransformIndicator.multiply;
import static org.ta4j.core.indicators.helpers.TransformIndicator.plus;
import static ta4jexamples.strategies.intelligenthelper.CombineIndicator.divide;
import static ta4jexamples.strategies.intelligenthelper.CombineIndicator.minus;
import static ta4jexamples.strategies.intelligenthelper.CombineIndicator.multiply;
import static ta4jexamples.strategies.intelligenthelper.CombineIndicator.plus;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BacktestExecutor;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.Trade;
import org.ta4j.core.cost.LinearTransactionCostModel;
import org.ta4j.core.cost.ZeroCostModel;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.ChandelierExitLongIndicator;
import org.ta4j.core.indicators.ChandelierExitShortIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SellIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.UnstableIndicator;
import org.ta4j.core.indicators.candles.BullishEngulfingIndicator;
import org.ta4j.core.indicators.candles.BullishHaramiIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.ConstantIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuKijunSenIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuTenkanSenIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelLowerIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelMiddleIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelUpperIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.PerformanceReport;
import org.ta4j.core.reports.PositionStatsReport;
import org.ta4j.core.reports.TradingStatement;
import org.ta4j.core.rules.BooleanIndicatorRule;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.FixedRule;
import org.ta4j.core.rules.IsEqualRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;

import ta4jexamples.strategies.intelligenthelper.CombineIndicator;
import ta4jexamples.strategies.intelligenthelper.DelayIndicator;
import ta4jexamples.strategies.intelligenthelper.HighestPivotPointIndicator;
import ta4jexamples.strategies.intelligenthelper.IchimokuLaggingSpanIndicator;
import ta4jexamples.strategies.intelligenthelper.IchimokuLead1FutureIndicator;
import ta4jexamples.strategies.intelligenthelper.IchimokuLead2FutureIndicator;
import ta4jexamples.strategies.intelligenthelper.IntelligentJsonSeriesLoader;
import ta4jexamples.strategies.intelligenthelper.IntelligentTrailIndicator;
import ta4jexamples.strategies.intelligenthelper.JsonRecordingTimeInterval;
import ta4jexamples.strategies.intelligenthelper.LowestPivotPointIndicator;
import ta4jexamples.strategies.intelligenthelper.TripleKeltnerChannelMiddleIndicator;

public class IntelligentTa4jOhlcBenchmarks {

    private static final Logger LOG = LoggerFactory.getLogger(IntelligentTa4jOhlcBenchmarks.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.########");
    private Set<BarSeries> allSeries;
    private BigDecimal enterFee;
    private BigDecimal exitFee;
    private double upPercentage;
    private int lookback_max;
    private BigDecimal enterFeeFactor;
    private BigDecimal exitFeeFactor;
    private BigDecimal upPercentageBig;
    private BigDecimal percentageUpperBound;
    private JsonRecordingTimeInterval interval;
    private int atrRatio;

    @Before
    public void setupTests() {
        interval = JsonRecordingTimeInterval.OneMinute;
        allSeries = loadSeries();

        enterFee = new BigDecimal("0.0024");
        exitFee = new BigDecimal("0.0024");
        enterFeeFactor = BigDecimal.ONE.add(enterFee);
        exitFeeFactor = BigDecimal.ONE.subtract(exitFee);
        atrRatio = 5;

        //upPercentage = 10;
        upPercentage = 1.309;
        upPercentage = 1.1545;
        upPercentage = 2;

        //lookback_max = 11;
        lookback_max = 400;

        upPercentageBig = new BigDecimal("1.1545");

        //percentageUpperBound = new BigDecimal("0.001");
        percentageUpperBound = new BigDecimal("0.1");

    }

    @Test
    public void benchmarkHiddenConversionThreeLookbackWithFixedTakeProfitSeriesTa4j() {
        runBenchmarkForFourVariables("HiddenConversionThreeLookbackWithFixedTakeProfitSeries",
                i -> j -> k -> l -> series -> {
                    String uuid = UUID.randomUUID().toString();
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

                    EMAIndicator longEma = new EMAIndicator(closePriceIndicator, i);
                    EMAIndicator shortEma = new EMAIndicator(closePriceIndicator, j);
                    RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), 14);


                    ATRIndicator trueRangeIndicator = new ATRIndicator(series, 14);
                    TransformIndicator trueRangeFactor = multiply(trueRangeIndicator, 2);
                    CombineIndicator emaUpTrendLine = plus(longEma, trueRangeFactor);

                    LowestPivotPointIndicator lastLow = new LowestPivotPointIndicator(series, uuid,l );
                    HighestPivotPointIndicator lastHigh = new HighestPivotPointIndicator(series, uuid, l);
                    lastLow.setOppositPivotIndicator(lastHigh);
                    lastHigh.setOppositPivotIndicator(lastLow);

                    LowestPivotPointIndicator rsiAtLastLow = new LowestPivotPointIndicator(series, rsi, uuid,l);
                    HighestPivotPointIndicator rsiAtLastHigh = new HighestPivotPointIndicator(series, rsi, uuid,l);
                    rsiAtLastHigh.setOppositPivotIndicator(rsiAtLastLow);
                    rsiAtLastLow.setOppositPivotIndicator(rsiAtLastHigh);

                    LowestPivotPointIndicator secondLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(lastLow, 1), uuid,l);
                    HighestPivotPointIndicator secondLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(lastLow, 1), uuid,l);
                    secondLastLow.setOppositPivotIndicator(secondLastHigh);
                    secondLastHigh.setOppositPivotIndicator(secondLastLow);

                    LowestPivotPointIndicator rsiAtSecondLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(rsiAtLastLow, 1), uuid,l);
                    HighestPivotPointIndicator rsiAtSecondLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(rsiAtLastLow, 1), uuid,l);
                    rsiAtSecondLastLow.setOppositPivotIndicator(rsiAtSecondLastHigh);
                    rsiAtSecondLastHigh.setOppositPivotIndicator(rsiAtSecondLastLow);

                    LowestPivotPointIndicator thirdLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(secondLastLow, 1), uuid,l);
                    HighestPivotPointIndicator thirdLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(secondLastLow, 1), uuid,l);
                    thirdLastLow.setOppositPivotIndicator(thirdLastHigh);
                    thirdLastHigh.setOppositPivotIndicator(thirdLastLow);

                    LowestPivotPointIndicator rsiAtThirdLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(rsiAtSecondLastLow, 1), uuid,l);
                    HighestPivotPointIndicator rsiAtThirdLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(rsiAtSecondLastLow, 1), uuid,l);
                    rsiAtThirdLastLow.setOppositPivotIndicator(rsiAtThirdLastHigh);
                    rsiAtThirdLastHigh.setOppositPivotIndicator(rsiAtThirdLastLow);

                    Rule upTrend = new OverIndicatorRule(shortEma, emaUpTrendLine);
                    Rule priceOverLongReversalArea = new OverIndicatorRule(closePriceIndicator, emaUpTrendLine);
                    Rule lowPriceMovesUp = new OverIndicatorRule(lastLow, secondLastLow);

                    Rule oversoldIndicatorMovesDown = new UnderIndicatorRule(rsiAtLastLow, rsiAtSecondLastLow);

                    Rule lowPriceMovesUpLooback2 = new OverIndicatorRule(lastLow, thirdLastLow);
                    Rule oversoldIndicatorMovesDownLookback2 = new UnderIndicatorRule(rsiAtLastLow, rsiAtThirdLastLow);

                    Rule candleStickPattern = new BooleanIndicatorRule(new BullishEngulfingIndicator(series))
                            .or(new BooleanIndicatorRule(new BullishHaramiIndicator(series)))
                            //.or(new BooleanIndicatorRule(new ThreeWhiteSoldiersIndicator(series, pivotCalculationFrame, series.numOf(0.1))))
                            ;

                    Rule hiddenDivergenceBetweenLastTwo = lowPriceMovesUp.and(oversoldIndicatorMovesDown);
                    Rule hiddenDivergenceBetweenLastAndThird = lowPriceMovesUpLooback2.and(oversoldIndicatorMovesDownLookback2);

                    Rule hiddenDivergence = hiddenDivergenceBetweenLastTwo.or(hiddenDivergenceBetweenLastAndThird);

                    Rule lastLowIsInFrame = new IsEqualRule(lastLow, new LowestValueIndicator(new LowPriceIndicator(series), k));
                    Rule longEntryRule = upTrend
                            .and(priceOverLongReversalArea)
                            .and(hiddenDivergence)
                            //.and(candleStickPattern)
                            .and(lastLowIsInFrame)
                            ;

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Rule exitRule = createFixedStopLossGainProfitRule(series, breakEvenIndicator, atrRatio);

                    return new StrategyCreationResult(longEntryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkHiddenConversionThreeLookbackWithFixedTakeProfitRsi50SeriesTa4j() {
        runBenchmarkForFourVariables("HiddenConversionThreeLookbackWithFixedTakeProfitRsi50Series",
                i -> j -> k -> l -> series -> {
                    String uuid = UUID.randomUUID().toString();
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

                    EMAIndicator longEma = new EMAIndicator(closePriceIndicator, i);
                    EMAIndicator shortEma = new EMAIndicator(closePriceIndicator, j);
                    RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), 14);


                    ATRIndicator trueRangeIndicator = new ATRIndicator(series, 14);
                    TransformIndicator trueRangeFactor = multiply(trueRangeIndicator, 2);
                    CombineIndicator emaUpTrendLine = plus(longEma, trueRangeFactor);

                    LowestPivotPointIndicator lastLow = new LowestPivotPointIndicator(series, uuid,l );
                    HighestPivotPointIndicator lastHigh = new HighestPivotPointIndicator(series, uuid, l);
                    lastLow.setOppositPivotIndicator(lastHigh);
                    lastHigh.setOppositPivotIndicator(lastLow);

                    LowestPivotPointIndicator rsiAtLastLow = new LowestPivotPointIndicator(series, rsi, uuid,l);
                    HighestPivotPointIndicator rsiAtLastHigh = new HighestPivotPointIndicator(series, rsi, uuid,l);
                    rsiAtLastHigh.setOppositPivotIndicator(rsiAtLastLow);
                    rsiAtLastLow.setOppositPivotIndicator(rsiAtLastHigh);

                    LowestPivotPointIndicator secondLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(lastLow, 1), uuid,l);
                    HighestPivotPointIndicator secondLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(lastLow, 1), uuid,l);
                    secondLastLow.setOppositPivotIndicator(secondLastHigh);
                    secondLastHigh.setOppositPivotIndicator(secondLastLow);

                    LowestPivotPointIndicator rsiAtSecondLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(rsiAtLastLow, 1), uuid,l);
                    HighestPivotPointIndicator rsiAtSecondLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(rsiAtLastLow, 1), uuid,l);
                    rsiAtSecondLastLow.setOppositPivotIndicator(rsiAtSecondLastHigh);
                    rsiAtSecondLastHigh.setOppositPivotIndicator(rsiAtSecondLastLow);

                    LowestPivotPointIndicator thirdLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(secondLastLow, 1), uuid,l);
                    HighestPivotPointIndicator thirdLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(secondLastLow, 1), uuid,l);
                    thirdLastLow.setOppositPivotIndicator(thirdLastHigh);
                    thirdLastHigh.setOppositPivotIndicator(thirdLastLow);

                    LowestPivotPointIndicator rsiAtThirdLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(rsiAtSecondLastLow, 1), uuid,l);
                    HighestPivotPointIndicator rsiAtThirdLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(rsiAtSecondLastLow, 1), uuid,l);
                    rsiAtThirdLastLow.setOppositPivotIndicator(rsiAtThirdLastHigh);
                    rsiAtThirdLastHigh.setOppositPivotIndicator(rsiAtThirdLastLow);

                    Rule upTrend = new OverIndicatorRule(shortEma, emaUpTrendLine);
                    Rule priceOverLongReversalArea = new OverIndicatorRule(closePriceIndicator, emaUpTrendLine);
                    Rule lowPriceMovesUp = new OverIndicatorRule(lastLow, secondLastLow);

                    Rule oversoldIndicatorMovesDown = new UnderIndicatorRule(rsiAtLastLow, rsiAtSecondLastLow);

                    Rule lowPriceMovesUpLooback2 = new OverIndicatorRule(lastLow, thirdLastLow);
                    Rule oversoldIndicatorMovesDownLookback2 = new UnderIndicatorRule(rsiAtLastLow, rsiAtThirdLastLow);

                    Rule candleStickPattern = new BooleanIndicatorRule(new BullishEngulfingIndicator(series))
                            .or(new BooleanIndicatorRule(new BullishHaramiIndicator(series)))
                            //.or(new BooleanIndicatorRule(new ThreeWhiteSoldiersIndicator(series, pivotCalculationFrame, series.numOf(0.1))))
                            ;

                    Rule hiddenDivergenceBetweenLastTwo = lowPriceMovesUp.and(oversoldIndicatorMovesDown);
                    Rule hiddenDivergenceBetweenLastAndThird = lowPriceMovesUpLooback2.and(oversoldIndicatorMovesDownLookback2);

                    Rule hiddenDivergence = hiddenDivergenceBetweenLastTwo.or(hiddenDivergenceBetweenLastAndThird);

                    Rule lastLowIsInFrame = new IsEqualRule(lastLow, new LowestValueIndicator(new LowPriceIndicator(series), k));
                    Rule longEntryRule = upTrend
                            .and(priceOverLongReversalArea)
                            .and(hiddenDivergence)
                            //.and(candleStickPattern)
                            .and(lastLowIsInFrame)
                            .and(new UnderIndicatorRule(rsi, new ConstantIndicator<>(series, series.numOf(50))))
                            ;

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Rule exitRule = createFixedStopLossGainProfitRule(series, breakEvenIndicator, atrRatio);

                    return new StrategyCreationResult(longEntryRule, exitRule, breakEvenIndicator);
                }
        );
    }



    @Test
    public void benchmarkHiddenConversionWithFixedTakeProfitRsi50SeriesTa4j() {
        runBenchmarkForFourVariables("HiddenConversionWithFixedTakeProfitRsi50Series",
                i -> j -> k -> l -> series -> {
                    String uuid = UUID.randomUUID().toString();
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

                    EMAIndicator longEma = new EMAIndicator(closePriceIndicator, i);
                    EMAIndicator shortEma = new EMAIndicator(closePriceIndicator, j);
                    RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), 14);

                    ATRIndicator trueRangeIndicator = new ATRIndicator(series, 14);
                    TransformIndicator trueRangeFactor = multiply(trueRangeIndicator, 2);
                    CombineIndicator emaUpTrendLine = plus(longEma, trueRangeFactor);

                    LowestPivotPointIndicator lastLow = new LowestPivotPointIndicator(series, uuid,l );
                    HighestPivotPointIndicator lastHigh = new HighestPivotPointIndicator(series, uuid, l);
                    lastLow.setOppositPivotIndicator(lastHigh);
                    lastHigh.setOppositPivotIndicator(lastLow);

                    LowestPivotPointIndicator rsiAtLastLow = new LowestPivotPointIndicator(series, rsi, uuid,l);
                    HighestPivotPointIndicator rsiAtLastHigh = new HighestPivotPointIndicator(series, rsi, uuid,l);
                    rsiAtLastHigh.setOppositPivotIndicator(rsiAtLastLow);
                    rsiAtLastLow.setOppositPivotIndicator(rsiAtLastHigh);

                    LowestPivotPointIndicator secondLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(lastLow, 1), uuid,l);
                    HighestPivotPointIndicator secondLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(lastLow, 1), uuid,l);
                    secondLastLow.setOppositPivotIndicator(secondLastHigh);
                    secondLastHigh.setOppositPivotIndicator(secondLastLow);

                    LowestPivotPointIndicator rsiAtSecondLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(rsiAtLastLow, 1), uuid,l);
                    HighestPivotPointIndicator rsiAtSecondLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(rsiAtLastLow, 1), uuid,l);
                    rsiAtSecondLastLow.setOppositPivotIndicator(rsiAtSecondLastHigh);
                    rsiAtSecondLastHigh.setOppositPivotIndicator(rsiAtSecondLastLow);

                    Rule upTrend = new OverIndicatorRule(shortEma, emaUpTrendLine);
                    Rule priceOverLongReversalArea = new OverIndicatorRule(closePriceIndicator, emaUpTrendLine);
                    Rule lowPriceMovesUp = new OverIndicatorRule(lastLow, secondLastLow);
                    Rule oversoldIndicatorMovesDown = new UnderIndicatorRule(rsiAtLastLow, rsiAtSecondLastLow);

                    Rule candleStickPattern = new BooleanIndicatorRule(new BullishEngulfingIndicator(series))
                            .or(new BooleanIndicatorRule(new BullishHaramiIndicator(series)))
                            //.or(new BooleanIndicatorRule(new ThreeWhiteSoldiersIndicator(series, pivotCalculationFrame, series.numOf(0.1))))
                            ;

                    Rule lastLowIsInFrame = new IsEqualRule(lastLow, new LowestValueIndicator(new LowPriceIndicator(series), k));
                    Rule longEntryRule = upTrend
                            .and(priceOverLongReversalArea)
                            .and(lowPriceMovesUp)
                            .and(oversoldIndicatorMovesDown)
                            //.and(candleStickPattern)
                            .and(lastLowIsInFrame)
                            .and(new UnderIndicatorRule(rsi, new ConstantIndicator<>(series, series.numOf(50))))
                            ;

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Rule exitRule = createFixedStopLossGainProfitRule(series, breakEvenIndicator, atrRatio);
                    return new StrategyCreationResult(longEntryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkHiddenConversionWithFixedTakeProfitSeriesTa4j() {
        runBenchmarkForFourVariables("HiddenConversionWithFixedTakeProfitSeries",
                i -> j -> k -> l -> series -> {
                    String uuid = UUID.randomUUID().toString();
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

                    EMAIndicator longEma = new EMAIndicator(closePriceIndicator, i);
                    EMAIndicator shortEma = new EMAIndicator(closePriceIndicator, j);
                    RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), 14);

                    ATRIndicator trueRangeIndicator = new ATRIndicator(series, 14);
                    TransformIndicator trueRangeFactor = multiply(trueRangeIndicator, 2);
                    CombineIndicator emaUpTrendLine = plus(longEma, trueRangeFactor);

                    LowestPivotPointIndicator lastLow = new LowestPivotPointIndicator(series, uuid,l );
                    HighestPivotPointIndicator lastHigh = new HighestPivotPointIndicator(series, uuid, l);
                    lastLow.setOppositPivotIndicator(lastHigh);
                    lastHigh.setOppositPivotIndicator(lastLow);

                    LowestPivotPointIndicator rsiAtLastLow = new LowestPivotPointIndicator(series, rsi, uuid,l);
                    HighestPivotPointIndicator rsiAtLastHigh = new HighestPivotPointIndicator(series, rsi, uuid,l);
                    rsiAtLastHigh.setOppositPivotIndicator(rsiAtLastLow);
                    rsiAtLastLow.setOppositPivotIndicator(rsiAtLastHigh);

                    LowestPivotPointIndicator secondLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(lastLow, 1), uuid,l);
                    HighestPivotPointIndicator secondLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(lastLow, 1), uuid,l);
                    secondLastLow.setOppositPivotIndicator(secondLastHigh);
                    secondLastHigh.setOppositPivotIndicator(secondLastLow);

                    LowestPivotPointIndicator rsiAtSecondLastLow = new LowestPivotPointIndicator(series, new DelayIndicator(rsiAtLastLow, 1), uuid,l);
                    HighestPivotPointIndicator rsiAtSecondLastHigh = new HighestPivotPointIndicator(series, new DelayIndicator(rsiAtLastLow, 1), uuid,l);
                    rsiAtSecondLastLow.setOppositPivotIndicator(rsiAtSecondLastHigh);
                    rsiAtSecondLastHigh.setOppositPivotIndicator(rsiAtSecondLastLow);

                    Rule upTrend = new OverIndicatorRule(shortEma, emaUpTrendLine);
                    Rule priceOverLongReversalArea = new OverIndicatorRule(closePriceIndicator, emaUpTrendLine);
                    Rule lowPriceMovesUp = new OverIndicatorRule(lastLow, secondLastLow);
                    Rule oversoldIndicatorMovesDown = new UnderIndicatorRule(rsiAtLastLow, rsiAtSecondLastLow);

                    Rule candleStickPattern = new BooleanIndicatorRule(new BullishEngulfingIndicator(series))
                            .or(new BooleanIndicatorRule(new BullishHaramiIndicator(series)))
                            //.or(new BooleanIndicatorRule(new ThreeWhiteSoldiersIndicator(series, pivotCalculationFrame, series.numOf(0.1))))
                            ;

                    Rule lastLowIsInFrame = new IsEqualRule(lastLow, new LowestValueIndicator(new LowPriceIndicator(series), k));
                    Rule longEntryRule = upTrend
                            .and(priceOverLongReversalArea)
                            .and(lowPriceMovesUp)
                            .and(oversoldIndicatorMovesDown)
                            //.and(candleStickPattern)
                            .and(lastLowIsInFrame)
                            ;

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Rule exitRule = createFixedStopLossGainProfitRule(series, breakEvenIndicator, atrRatio);
                    return new StrategyCreationResult(longEntryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkHiddenConversionWithChandelierExitTa4j() {
        runBenchmarkForFourVariables("HiddenConversionWithChandelierExit",
                i -> j -> k -> l -> series -> {
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
                    String uuid = UUID.randomUUID().toString();
                    int chandelierExitMultiplier = l;

                    EMAIndicator longEma = new EMAIndicator(closePriceIndicator, i);
                    EMAIndicator shortEma = new EMAIndicator(closePriceIndicator, j);
                    RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), 14);

                    HighestPivotPointIndicator highPivotPoints = new HighestPivotPointIndicator(series, uuid, 3);
                    LowestPivotPointIndicator lowPivotPoints = new LowestPivotPointIndicator(series, uuid, 3);
                    ATRIndicator trueRangeIndicator = new ATRIndicator(series, 14);
                    TransformIndicator trueRangeFactor = multiply(trueRangeIndicator, 2);
                    CombineIndicator emaUpTrendLine = plus(longEma, trueRangeFactor);
                    CombineIndicator emaDownTrendLine = minus(longEma, trueRangeFactor);

                    HighestPivotPointIndicator rsiAtHighPivotPoints = new HighestPivotPointIndicator(series, rsi, uuid, 3);
                    LowestPivotPointIndicator rsiAtLowPivotPoints = new LowestPivotPointIndicator(series, rsi, uuid, 3);

                    Rule upTrend = new OverIndicatorRule(shortEma, emaUpTrendLine);
                    Rule priceOverLongReversalArea = new OverIndicatorRule(closePriceIndicator, emaUpTrendLine);
                    Rule lowPriceMovesUp = new OverIndicatorRule(lowPivotPoints, new DelayIndicator(lowPivotPoints, 1));
                    Rule oversoldIndicatorMovesDown = new UnderIndicatorRule(rsiAtLowPivotPoints, new DelayIndicator(rsiAtLowPivotPoints, 1));

                    Rule longEntryRule = upTrend.and(priceOverLongReversalArea).and(lowPriceMovesUp).and(oversoldIndicatorMovesDown);

                    ChandelierExitLongIndicator chandelierExitLongIndicator = new ChandelierExitLongIndicator(series, 22, chandelierExitMultiplier);

                    Rule longExitRule = new UnderIndicatorRule(closePriceIndicator, chandelierExitLongIndicator);

                    Rule downTrend = new UnderIndicatorRule(shortEma, emaDownTrendLine);
                    Rule priceUnderLongReversalArea = new UnderIndicatorRule(closePriceIndicator, emaDownTrendLine);
                    Rule highPriceMovesDown = new UnderIndicatorRule(highPivotPoints, new DelayIndicator(highPivotPoints, 1));
                    Rule oversoldIndicatorMovesUp = new OverIndicatorRule(rsiAtHighPivotPoints, new DelayIndicator(rsiAtHighPivotPoints, 1));

                    Rule shortEntryRule = downTrend.and(priceUnderLongReversalArea).and(highPriceMovesDown).and(oversoldIndicatorMovesUp);

                    ChandelierExitShortIndicator chandelierExitShortIndicator = new ChandelierExitShortIndicator(series, 22, chandelierExitMultiplier);

                    Rule shortExitRule = new OverIndicatorRule(closePriceIndicator, chandelierExitShortIndicator);

                    return new StrategyCreationResult(longEntryRule, longExitRule, null);
                }
        );
    }

    @Test
    public void benchmarkIchimokuStopLossOrConversionCrossBaseLineTa4j() {
        runBenchmarkForTwoVariables("IchimokuStopLossOrConversionCrossBase",
                i -> j -> series -> {
                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    IchimokuRules ichimokuRules = createIchimokuBuyRule(series, i, j, breakEvenIndicator);

                    Rule entryRule = ichimokuRules.getEntryRule();

                    SellIndicator cloudLowerLineAtBuyPrice = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, ichimokuRules.currentCloudLowerLine.getValue(buyIndex)));

                    Rule conversionLineCrossesBackBaseline = new CrossedDownIndicatorRule(ichimokuRules.conversionLine, ichimokuRules.baseLine);
                    UnderIndicatorRule stopLossReached = new UnderIndicatorRule(ichimokuRules.closePriceIndicator, cloudLowerLineAtBuyPrice);

                    Rule exitRule = stopLossReached.or(conversionLineCrossesBackBaseline);

                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkIchimokuStopLossOrLaggingCrossConversionLineTa4j() {
        runBenchmarkForTwoVariables("IchimokuStopLossOrLaggingCrossConversionLine",
                i -> j -> series -> {
                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    IchimokuRules ichimokuRules = createIchimokuBuyRule(series, i, j, breakEvenIndicator);

                    UnstableIndicator delayedConversionLine = new UnstableIndicator(new DelayIndicator(ichimokuRules.conversionLine, i), i);
                    Rule entryRule = ichimokuRules.getEntryRule().and(new OverIndicatorRule(ichimokuRules.laggingSpan, delayedConversionLine));

                    SellIndicator cloudLowerLineAtBuyPrice = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, ichimokuRules.currentCloudLowerLine.getValue(buyIndex)));


                    UnderIndicatorRule laggingSpanEmergencyStopReached = new UnderIndicatorRule(ichimokuRules.laggingSpan, delayedConversionLine);
                    UnderIndicatorRule stopLossReached = new UnderIndicatorRule(ichimokuRules.closePriceIndicator, cloudLowerLineAtBuyPrice);

                    Rule exitRule = stopLossReached.or(laggingSpanEmergencyStopReached);

                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkIchimokuStopLossOrLaggingCrossBaseLineTa4j() {
        runBenchmarkForTwoVariables("IchimokuStopLossOrLaggingCrossBaseLine",
                i -> j -> series -> {
                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    IchimokuRules ichimokuRules = createIchimokuBuyRule(series, i, j, breakEvenIndicator);

                    UnstableIndicator delayedBaseline = new UnstableIndicator(new DelayIndicator(ichimokuRules.baseLine, i), i);
                    Rule entryRule = ichimokuRules.getEntryRule().and(new OverIndicatorRule(ichimokuRules.laggingSpan, delayedBaseline));

                    SellIndicator cloudLowerLineAtBuyPrice = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, ichimokuRules.currentCloudLowerLine.getValue(buyIndex)));


                    UnderIndicatorRule laggingSpanEmergencyStopReached = new UnderIndicatorRule(ichimokuRules.laggingSpan, delayedBaseline);
                    UnderIndicatorRule stopLossReached = new UnderIndicatorRule(ichimokuRules.closePriceIndicator, cloudLowerLineAtBuyPrice);

                    Rule exitRule = stopLossReached.or(laggingSpanEmergencyStopReached);

                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkIchimokuStopLossOrLaggingCrossMarketPriceTa4j() {
        runBenchmarkForTwoVariables("IchimokuStopLossOrLaggingCrossMarketPrice",
                i -> j -> series -> {
                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    IchimokuRules ichimokuRules = createIchimokuBuyRule(series, i, j, breakEvenIndicator);

                    Rule entryRule = ichimokuRules.getEntryRule();

                    SellIndicator cloudLowerLineAtBuyPrice = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, ichimokuRules.currentCloudLowerLine.getValue(buyIndex)));

                    UnderIndicatorRule laggingSpanEmergencyStopReached = new UnderIndicatorRule(ichimokuRules.laggingSpan, ichimokuRules.delayedMarketPrice);
                    UnderIndicatorRule stopLossReached = new UnderIndicatorRule(ichimokuRules.closePriceIndicator, cloudLowerLineAtBuyPrice);

                    Rule exitRule = stopLossReached.or(laggingSpanEmergencyStopReached);

                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkIchimokuStopLossOrGainWithoutLaggingSpanTa4j() {
        runBenchmarkForTwoVariables("IchimokuStopLossOrGainWithoutLaggingSpan",
                i -> j -> series -> {
                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    IchimokuRules ichimokuRules = createIchimokuBuyRule(series, i, j, breakEvenIndicator);

                    Rule entryRule = ichimokuRules.getEntryRule();


                    Number targetToRiskRatio = 2;


                    SellIndicator cloudLowerLineAtBuyPrice = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, ichimokuRules.currentCloudLowerLine.getValue(buyIndex)));
                    Indicator<Num> buyPriceIndicator = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, ichimokuRules.closePriceIndicator.getValue(buyIndex)));

                    CombineIndicator sellPriceGainCal = multiply(plus(multiply(minus(divide(ichimokuRules.closePriceIndicator, cloudLowerLineAtBuyPrice), 1), targetToRiskRatio), 1), buyPriceIndicator);
                    SellIndicator gainSellPriceCalculator = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, sellPriceGainCal.getValue(buyIndex)));

                    Rule takeProfitAndBreakEvenReached = new OverIndicatorRule(ichimokuRules.closePriceIndicator, gainSellPriceCalculator).and(new OverIndicatorRule(ichimokuRules.closePriceIndicator, breakEvenIndicator));
                    UnderIndicatorRule stopLossReached = new UnderIndicatorRule(ichimokuRules.closePriceIndicator, cloudLowerLineAtBuyPrice);

                    Rule exitRule = stopLossReached.or(takeProfitAndBreakEvenReached);


                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkIchimokuStopLossOrGainTa4j() {
        runBenchmarkForTwoVariables("IchimokuStopLossOrGain",
                i -> j -> series -> {
                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    IchimokuRules ichimokuRules = createIchimokuBuyRule(series, i, j, breakEvenIndicator);

                    Rule entryRule = ichimokuRules.getEntryRule();


                    Number targetToRiskRatio = 2;


                    SellIndicator cloudLowerLineAtBuyPrice = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, ichimokuRules.currentCloudLowerLine.getValue(buyIndex)));
                    Indicator<Num> buyPriceIndicator = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, ichimokuRules.closePriceIndicator.getValue(buyIndex)));

                    CombineIndicator sellPriceGainCal = multiply(plus(multiply(minus(divide(ichimokuRules.closePriceIndicator, cloudLowerLineAtBuyPrice), 1), targetToRiskRatio), 1), buyPriceIndicator);
                    SellIndicator gainSellPriceCalculator = new SellIndicator(series, breakEvenIndicator, (buyIndex, index) -> new ConstantIndicator<>(series, sellPriceGainCal.getValue(buyIndex)));

                    Rule takeProfitAndBreakEvenReached = new OverIndicatorRule(ichimokuRules.closePriceIndicator, gainSellPriceCalculator).and(new OverIndicatorRule(ichimokuRules.closePriceIndicator, breakEvenIndicator));

                    UnderIndicatorRule laggingSpanEmergencyStopReached = new UnderIndicatorRule(ichimokuRules.laggingSpan, ichimokuRules.delayedMarketPrice);
                    UnderIndicatorRule stopLossReached = new UnderIndicatorRule(ichimokuRules.closePriceIndicator, cloudLowerLineAtBuyPrice);

                    Rule exitRule = stopLossReached.or(takeProfitAndBreakEvenReached).or(laggingSpanEmergencyStopReached);


                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkIchimokuTrailingFixedTa4j() {
        runBenchmarkForTwoVariables("IchimokuTrailingFixed",
                i -> j -> series -> {
                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    IchimokuRules ichimokuRules = createIchimokuBuyRule(series, i, j, breakEvenIndicator);
                    Rule entryRule = ichimokuRules.getEntryRule();


                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.03"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.0125"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(ichimokuRules.closePriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkIchimokuTrailingFlexibleTa4j() {
        runBenchmarkForTwoVariablesWithTrailingStopLoss("IchimokuTrailingFlexible",
                i -> j -> intelligentTrailIndicator -> series -> {
                    IchimokuRules ichimokuRules = createIchimokuBuyRule(series, i, j, intelligentTrailIndicator.getBreakEvenIndicator());
                    Rule entryRule = ichimokuRules.getEntryRule();

                    UnderIndicatorRule exitRule = new UnderIndicatorRule(ichimokuRules.closePriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, intelligentTrailIndicator.getBreakEvenIndicator());
                }
        );
    }

    @Test
    public void benchmarkKeltnerTripleOnlyUpAndTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerTripleUp",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createTripleKeltner(i, j, series);

                    Rule entryRule = new OverIndicatorRule(keltnerRules.closePriceIndicator, keltnerRules.keltnerHigh);

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkKeltnerDefaultOnlyUpAndTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerUp",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createDefaultKeltner(i, j, series);

                    Rule entryRule = new OverIndicatorRule(keltnerRules.closePriceIndicator, keltnerRules.keltnerHigh);

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }


    @Test
    public void benchmarkKeltnerTripleUpAndDownTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerTripleUpDown",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createTripleKeltner(i, j, series);

                    Rule entryRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, keltnerRules.keltnerLow).or(new OverIndicatorRule(keltnerRules.closePriceIndicator, keltnerRules.keltnerHigh));

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkKeltnerDefaultUpAndDownTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerUpDown",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createDefaultKeltner(i, j, series);

                    Rule entryRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, keltnerRules.keltnerLow).or(new OverIndicatorRule(keltnerRules.closePriceIndicator, keltnerRules.keltnerHigh));

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void keltnerRulesKeltnerTripleDownAndTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerTripleDown",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createTripleKeltner(i, j, series);

                    Rule entryRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, keltnerRules.keltnerLow);

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    Rule exitRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, intelligentTrailIndicator);

                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkKeltnerDefaultDownTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerDown",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createDefaultKeltner(i, j, series);

                    Rule entryRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, keltnerRules.keltnerLow);

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.closePriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkPureEmaTa4j() {
        runBenchmarkForFourVariables("Ta4jPureEma",
                i -> j -> k -> l -> series -> {
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

                    EMAIndicator buyIndicatorLong = new EMAIndicator(closePriceIndicator, Math.toIntExact(i));
                    Indicator<Num> buyIndicatorShort = new EMAIndicator(closePriceIndicator, Math.toIntExact(j));

                    EMAIndicator sellIndicatorLong = new EMAIndicator(closePriceIndicator, Math.toIntExact(k));
                    Indicator<Num> sellIndicatorShort = new EMAIndicator(closePriceIndicator, Math.toIntExact(l));

                    Rule entryRule = new CrossedUpIndicatorRule(buyIndicatorShort, buyIndicatorLong) // Trend
                            ;

                    Rule exitRule = new CrossedDownIndicatorRule(sellIndicatorShort, sellIndicatorLong) // Trend
                            ;
                    return new StrategyCreationResult(entryRule, exitRule, null);
                });
    }

    @Test
    public void benchmarkEmaTa4j() {
        runBenchmarkForFourVariables("Ta4jMacd",
                i -> j -> k -> l -> series -> {
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

                    StochasticOscillatorKIndicator stochasticOscillaltorK = new StochasticOscillatorKIndicator(series, k);
                    MACDIndicator macd = new MACDIndicator(closePriceIndicator, j, i);
                    EMAIndicator emaMacd = new EMAIndicator(macd, l);


                    EMAIndicator buyIndicatorLong = new EMAIndicator(closePriceIndicator, Math.toIntExact(i));
                    Indicator<Num> buyIndicatorShort = new EMAIndicator(closePriceIndicator, Math.toIntExact(j));

                    EMAIndicator sellIndicatorLong = new EMAIndicator(closePriceIndicator, Math.toIntExact(i));
                    Indicator<Num> sellIndicatorShort = new EMAIndicator(closePriceIndicator, Math.toIntExact(j));

                    Rule entryRule = new OverIndicatorRule(buyIndicatorShort, buyIndicatorLong) // Trend
                            .and(new UnderIndicatorRule(stochasticOscillaltorK, 20)) // Signal 1
                            .and(new OverIndicatorRule(macd, emaMacd)); // Signal 2
                    ;

                    Rule exitRule = new UnderIndicatorRule(sellIndicatorShort, sellIndicatorLong) // Trend
                            .and(new OverIndicatorRule(stochasticOscillaltorK, 80)) // Signal 1
                            .and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2
                    ;
                    return new StrategyCreationResult(entryRule, exitRule, null);


                });
    }

    @Test
    public void benchmarkTa4jTrailing() {
        runBenchmarkForSixVariablesPercentage("Ta4jTrailing",
                i -> j -> k -> below -> above -> minAbove -> series -> {
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
                    LowestValueIndicator buyLongIndicator = new LowestValueIndicator(closePriceIndicator, Math.toIntExact(i));
                    LowestValueIndicator buyShortIndicator = new LowestValueIndicator(closePriceIndicator, Math.toIntExact(j));
                    TransformIndicator buyGainLine = TransformIndicator.multiply(buyLongIndicator, BigDecimal.ONE.add(k));

                    SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);

                    OverIndicatorRule entryRule = new OverIndicatorRule(buyShortIndicator, buyGainLine);


                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, below, breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, above, breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, minAbove, breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(closePriceIndicator, intelligentTrailIndicator);

                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                });
    }

    private void runBenchmarkForTwoVariables(
            String benchmarkName,
            Function<Integer, Function<Integer, Function<BarSeries, StrategyCreationResult>>> strategyCreator
    ) {
        Queue<StrategyBenchmarkConfiguration> strategies = new LinkedList<>();

        for (long i = 1; i < lookback_max; i = Math.round(Math.ceil(i * upPercentage))) {
            for (long j = 1; j < i; j = Math.round(Math.ceil(j * upPercentage))) {
                String currentStrategyName = "i(" + i + "), j(" + j + ")";
                LOG.info(currentStrategyName);
                List<StrategyConfiguration> strategiesForTheSeries = new LinkedList<>();
                for (BarSeries series : allSeries) {
                    StrategyCreationResult creationResult = strategyCreator.apply(Math.toIntExact(i)).apply(Math.toIntExact(j)).apply(series);
                    BaseStrategy strategy = new BaseStrategy(series.getName() + "_" + currentStrategyName, creationResult.getEntryRule(), creationResult.getExitRule());
                    strategiesForTheSeries.add(new StrategyConfiguration(strategy, series, creationResult.getBreakEvenIndicator()));
                }
                strategies.offer(new StrategyBenchmarkConfiguration(strategiesForTheSeries, currentStrategyName));
            }
        }

        List<TradingStatement> result = simulateStrategies(strategies);
        sortResultsByProfit(result);
        printAndSaveResults(result, "_" + benchmarkName + "_", null);
    }

    private IchimokuRules createIchimokuBuyRule(BarSeries series, int ICHIMOKU_LONG_SPAN, int ICHIMOKU_SHORT_SPAN, Indicator<Num> breakEvenIndicator) {
        IchimokuRules result = new IchimokuRules(series);
        IchimokuTenkanSenIndicator conversionLine = new IchimokuTenkanSenIndicator(series, ICHIMOKU_SHORT_SPAN); //9
        IchimokuKijunSenIndicator baseLine = new IchimokuKijunSenIndicator(series, ICHIMOKU_LONG_SPAN); //26
        result.baseLine = baseLine;
        result.conversionLine = conversionLine;
        result.laggingSpan = new IchimokuLaggingSpanIndicator(result.closePriceIndicator);

        IchimokuLead1FutureIndicator lead1Future = new IchimokuLead1FutureIndicator(conversionLine, baseLine); //26
        IchimokuLead2FutureIndicator lead2Future = new IchimokuLead2FutureIndicator(series, 2 * ICHIMOKU_LONG_SPAN); // 52

        UnstableIndicator lead1Current = new UnstableIndicator(new DelayIndicator(lead1Future, ICHIMOKU_LONG_SPAN), ICHIMOKU_LONG_SPAN);
        UnstableIndicator lead2Current = new UnstableIndicator(new DelayIndicator(lead2Future, ICHIMOKU_LONG_SPAN), ICHIMOKU_LONG_SPAN);

        Indicator<Num> lead1Past = new UnstableIndicator(new DelayIndicator(lead1Future, 2 * ICHIMOKU_LONG_SPAN), 2 * ICHIMOKU_LONG_SPAN);
        Indicator<Num> lead2Past = new UnstableIndicator(new DelayIndicator(lead2Future, 2 * ICHIMOKU_LONG_SPAN), 2 * ICHIMOKU_LONG_SPAN);


        CombineIndicator currentCloudUpperLine = CombineIndicator.max(lead1Current, lead2Current);
        CombineIndicator currentCloudLowerLine = CombineIndicator.min(lead1Current, lead2Current);
        result.currentCloudLowerLine = currentCloudLowerLine;
        result.delayedMarketPrice = new UnstableIndicator(new DelayIndicator(result.closePriceIndicator, ICHIMOKU_LONG_SPAN), ICHIMOKU_LONG_SPAN);

        OverIndicatorRule cloudGreenInFuture = new OverIndicatorRule(lead1Future, lead2Future);
        Rule conversionLineCrossesBaseLine = new CrossedUpIndicatorRule(conversionLine, baseLine);
        //Rule conversionLineCrossOverCloud = new OverIndicatorRule(baseLine, currentCloudUpperLine).and(conversionLineCrossesBaseLine);
        Rule laggingSpanAbovePastPrice = new OverIndicatorRule(result.laggingSpan, result.delayedMarketPrice);
        Rule priceAboveTheCloud = new OverIndicatorRule(result.closePriceIndicator, currentCloudUpperLine);
        Rule priceAboveConversionLine = new OverIndicatorRule(result.closePriceIndicator, result.conversionLine);

        Rule entryRule = priceAboveTheCloud
                .and(cloudGreenInFuture)
                .and(conversionLineCrossesBaseLine)
                .and(laggingSpanAbovePastPrice)
                .and(priceAboveConversionLine);

        result.setEntryRule(entryRule);
        return result;
    }

    private KeltnerRules createTripleKeltner(Integer keltnerBarCount, Integer keltnerRatio, BarSeries series) {
        KeltnerRules result = new KeltnerRules(series);
        result.midIndicator = new TripleKeltnerChannelMiddleIndicator(result.closePriceIndicator, keltnerBarCount);
        result.createOuterRules(keltnerBarCount, keltnerRatio);
        return result;
    }

    private KeltnerRules createDefaultKeltner(Integer keltnerBarCount, Integer keltnerRatio, BarSeries series) {
        KeltnerRules result = new KeltnerRules(series);
        result.midIndicator = new KeltnerChannelMiddleIndicator(result.closePriceIndicator, keltnerBarCount);
        result.createOuterRules(keltnerBarCount, keltnerRatio);
        return result;
    }

    private void runBenchmarkForFourVariables(
            String benchmarkName,
            Function<Integer, Function<Integer, Function<Integer, Function<Integer, Function<BarSeries, StrategyCreationResult>>>>> strategyCreator
    ) {
        Queue<StrategyBenchmarkConfiguration> strategies = new LinkedList<>();

        for (long i = 1; i < lookback_max; i = Math.round(Math.ceil(i * upPercentage))) {
            for (long j = 1; j < i; j = Math.round(Math.ceil(j * upPercentage))) {
                for (long k = 1; k < 33; k = Math.round(Math.ceil(k * upPercentage))) {
                    for (long l = 1; l < 33; l = Math.round(Math.ceil(l * upPercentage))) {
                        String currentStrategyName = "i(" + i + "), j(" + j + "), k(" + k + "),l(" + l + ")";
                        LOG.info(currentStrategyName);
                        List<StrategyConfiguration> strategiesForTheSeries = new LinkedList<>();
                        for (BarSeries series : allSeries) {
                            StrategyCreationResult creationResult = strategyCreator.apply(Math.toIntExact(i))
                                    .apply(Math.toIntExact(j))
                                    .apply(Math.toIntExact(k))
                                    .apply(Math.toIntExact(l))
                                    .apply(series);
                            BaseStrategy strategy = new BaseStrategy(series.getName() + "_" + currentStrategyName, creationResult.getEntryRule(), creationResult.getExitRule());
                            strategiesForTheSeries.add(new StrategyConfiguration(strategy, series, creationResult.getBreakEvenIndicator()));
                        }
                        strategies.offer(new StrategyBenchmarkConfiguration(strategiesForTheSeries, currentStrategyName));
                    }
                }
            }
        }

        List<TradingStatement> result = simulateStrategies(strategies);
        sortResultsByProfit(result);
        printAndSaveResults(result, "_" + benchmarkName + "_", null);
    }

    private void runBenchmarkForTwoVariablesWithTrailingStopLoss(
            String benchmarkName,
            Function<Integer, Function<Integer, Function<IntelligentTrailIndicator, Function<BarSeries, StrategyCreationResult>>>> strategyCreator
    ) {
        Queue<StrategyBenchmarkConfiguration> strategies = new LinkedList<>();

        for (long i = 1; i < lookback_max; i = Math.round(Math.ceil(i * upPercentage))) {
            for (long j = 1; j < i; j = Math.round(Math.ceil(j * upPercentage))) {
                for (BigDecimal below = new BigDecimal("0.001"); below.compareTo(percentageUpperBound) <= 0; below = below.multiply(upPercentageBig)) {
                    for (BigDecimal above = new BigDecimal("0.001"); above.compareTo(percentageUpperBound) <= 0; above = above.multiply(upPercentageBig)) {
                        for (BigDecimal minAbove = new BigDecimal("0.001"); minAbove.compareTo(above) <= 0; minAbove = minAbove.multiply(upPercentageBig)) {
                            String currentStrategyName = "i(" + i + "), j(" + j + "), below(" + DECIMAL_FORMAT.format(below) + "), above(" + DECIMAL_FORMAT.format(above) + "), minAbove(" + DECIMAL_FORMAT.format(minAbove) + ")";
                            LOG.info(currentStrategyName);
                            List<StrategyConfiguration> strategiesForTheSeries = new LinkedList<>();
                            for (BarSeries series : allSeries) {
                                SellIndicator breakEvenIndicator = SellIndicator.createClosepriceBreakEvenIndicator(series, enterFee, exitFee);

                                Indicator<Num> belowBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, below, breakEvenIndicator);
                                Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, above, breakEvenIndicator);
                                Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, minAbove, breakEvenIndicator);

                                IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);

                                StrategyCreationResult creationResult = strategyCreator.apply(Math.toIntExact(i))
                                        .apply(Math.toIntExact(j))
                                        .apply(intelligentTrailIndicator)
                                        .apply(series);
                                BaseStrategy strategy = new BaseStrategy(series.getName() + "_" + currentStrategyName, creationResult.getEntryRule(), creationResult.getExitRule());
                                strategiesForTheSeries.add(new StrategyConfiguration(strategy, series, creationResult.getBreakEvenIndicator()));
                            }
                            strategies.offer(new StrategyBenchmarkConfiguration(strategiesForTheSeries, currentStrategyName));
                        }
                    }
                }
            }
        }

        List<TradingStatement> result = simulateStrategies(strategies);
        sortResultsByProfit(result);
        printAndSaveResults(result, "_" + benchmarkName + "_", null);
    }

    private void runBenchmarkForSixVariablesPercentage(
            String benchmarkName,
            Function<Integer, Function<Integer, Function<BigDecimal, Function<BigDecimal, Function<BigDecimal, Function<BigDecimal, Function<BarSeries, StrategyCreationResult>>>>>>> strategyCreator
    ) {
        Queue<StrategyBenchmarkConfiguration> strategies = new LinkedList<>();

        for (long i = 1; i < lookback_max; i = Math.round(Math.ceil(i * upPercentage))) {
            for (long j = 1; j < i; j = Math.round(Math.ceil(j * upPercentage))) {
                for (BigDecimal k = new BigDecimal("0.001"); k.compareTo(percentageUpperBound) <= 0; k = k.multiply(upPercentageBig)) { // buy up percentage needed
                    for (BigDecimal below = new BigDecimal("0.001"); below.compareTo(percentageUpperBound) <= 0; below = below.multiply(upPercentageBig)) {
                        for (BigDecimal above = new BigDecimal("0.001"); above.compareTo(percentageUpperBound) <= 0; above = above.multiply(upPercentageBig)) {
                            for (BigDecimal minAbove = new BigDecimal("0.001"); minAbove.compareTo(above) <= 0; minAbove = minAbove.multiply(upPercentageBig)) {
                                String currentStrategyName = "i(" + i + "), j(" + j + "), k(" + DECIMAL_FORMAT.format(k) + "),below(" + DECIMAL_FORMAT.format(below) + "),above(" + DECIMAL_FORMAT.format(above) + "),minAbove(" + DECIMAL_FORMAT.format(minAbove) + ")";
                                LOG.info(currentStrategyName);
                                List<StrategyConfiguration> strategiesForTheSeries = new LinkedList<>();
                                for (BarSeries series : allSeries) {
                                    StrategyCreationResult creationResult = strategyCreator.apply(Math.toIntExact(i))
                                            .apply(Math.toIntExact(j))
                                            .apply(k)
                                            .apply(below)
                                            .apply(above)
                                            .apply(minAbove)
                                            .apply(series);
                                    BaseStrategy strategy = new BaseStrategy(series.getName() + "_" + currentStrategyName, creationResult.getEntryRule(), creationResult.getExitRule());
                                    strategiesForTheSeries.add(new StrategyConfiguration(strategy, series, creationResult.getBreakEvenIndicator()));
                                }
                                strategies.offer(new StrategyBenchmarkConfiguration(strategiesForTheSeries, currentStrategyName));
                            }
                        }
                    }
                }
            }
        }

        List<TradingStatement> result = simulateStrategies(strategies);
        sortResultsByProfit(result);
        printAndSaveResults(result, "_" + benchmarkName + "_", null);
    }

    private List<TradingStatement> simulateStrategies(Queue<StrategyBenchmarkConfiguration> benchmarkConfigurations) {
        List<TradingStatement> result = new LinkedList<>();
        int counter = 0;
        int originalSize = benchmarkConfigurations.size();
        while (benchmarkConfigurations.size() > 0) {
            counter++;
            LOG.info("Executing ta4j configurations " + counter + "/" + originalSize);

            StrategyBenchmarkConfiguration strats = benchmarkConfigurations.poll();
            Map<TradingStatement, BarSeries> currentSeriesResult = new HashMap<>();

            int amountStrategies = strats.list.size();
            int strategyCounter = 0;
            for (StrategyConfiguration entry : strats.list) {
                strategyCounter++;
                //LOG.info("  * Simulating strategy " + strategyCounter + "/" + amountStrategies);
                BacktestExecutor bte = new BacktestExecutor(entry.series, new LinearTransactionCostModel(0.0024), new ZeroCostModel());
                Map<Strategy, SellIndicator> toBeExecuted = new HashMap<>();
                toBeExecuted.put(entry.strategy, entry.breakEvenIndicator);
                List<TradingStatement> tradingStatement = bte.execute(toBeExecuted, entry.series.numOf(25), Trade.TradeType.BUY);
                if (tradingStatement.size() != 1) {
                    throw new IllegalStateException("Put only 1 Strategy. Why mutliple results?");
                }
                currentSeriesResult.put(tradingStatement.get(0), entry.series);
                entry.strategy.destroy();
            }
            result.add(combineTradingStatements(currentSeriesResult, strats.name));
            if (counter % 100 == 0) {
                //System.gc();
            }
        }
        return result;
    }

    private void printAndSaveResults(List<TradingStatement> result, String name, BigDecimal percentageUpperBound) {
        LOG.info("---Worst result:--- \n" + printReport(result.subList(0, Math.min(1, result.size()))) + "\n-------------");
        LOG.info("---best results:--- \n" + printReport(result.subList(Math.max(result.size() - 10, 0), result.size())) + "\n-------------");
        String suffix = name + System.currentTimeMillis() + "_steps_" + upPercentage + "_maxLookback_" + lookback_max;
        if (percentageUpperBound != null) {
            suffix += "_maxPercentage_" + DECIMAL_FORMAT.format(percentageUpperBound);
        }
        store(result, suffix);
    }

    private void sortResultsByProfitPercentage(List<TradingStatement> result) {
        result.sort((o1, o2) -> {
            Num trades1 = o1.getPositionStatsReport().getLossCount().plus(o1.getPositionStatsReport().getProfitCount()).plus(o1.getPositionStatsReport().getBreakEvenCount());
            Num trades2 = o2.getPositionStatsReport().getLossCount().plus(o2.getPositionStatsReport().getProfitCount()).plus(o2.getPositionStatsReport().getBreakEvenCount());

            if (trades1.isLessThanOrEqual(trades1.numOf(1))) {
                if (trades2.isLessThanOrEqual(trades1.numOf(1))) {
                    return 0;
                } else {
                    return -1;
                }
            }
            if (trades2.isLessThanOrEqual(trades1.numOf(1))) {
                return 1;
            }

            return o1.getPerformanceReport().getTotalProfitLossPercentage().compareTo(o2.getPerformanceReport().getTotalProfitLossPercentage());
        });
    }

    private void sortResultsByProfit(List<TradingStatement> result) {
        result.sort((o1, o2) -> {
            Num trades1 = o1.getPositionStatsReport().getLossCount().plus(o1.getPositionStatsReport().getProfitCount()).plus(o1.getPositionStatsReport().getBreakEvenCount());
            Num trades2 = o2.getPositionStatsReport().getLossCount().plus(o2.getPositionStatsReport().getProfitCount()).plus(o2.getPositionStatsReport().getBreakEvenCount());

            if (trades1.isLessThanOrEqual(trades1.numOf(1))) {
                if (trades2.isLessThanOrEqual(trades1.numOf(1))) {
                    return 0;
                } else {
                    return -1;
                }
            }
            if (trades2.isLessThanOrEqual(trades1.numOf(1))) {
                return 1;
            }

            return o1.getPerformanceReport().getTotalProfitLoss().compareTo(o2.getPerformanceReport().getTotalProfitLoss());
        });
    }

    private TradingStatement combineTradingStatements(Map<TradingStatement, BarSeries> statements, String strategyName) {
        Num totalProfitLoss = DecimalNum.valueOf(0);
        Num totalProfitLossPercentage = DecimalNum.valueOf(0);
        Num totalProfit = DecimalNum.valueOf(0);
        Num totalLoss = DecimalNum.valueOf(0);

        Num profitCount = DecimalNum.valueOf(0);
        Num lossCount = DecimalNum.valueOf(0);
        Num breakEvenCount = DecimalNum.valueOf(0);
        int currentTickSum = 0;

        for (Map.Entry<TradingStatement, BarSeries> entry : statements.entrySet()) {
            TradingStatement statement = entry.getKey();
            BarSeries series = entry.getValue();

            totalProfitLoss = totalProfitLoss.plus(statement.getPerformanceReport().getTotalProfitLoss());
            totalProfit = totalProfit.plus(statement.getPerformanceReport().getTotalProfit());
            totalLoss = totalLoss.plus(statement.getPerformanceReport().getTotalLoss());

            profitCount = profitCount.plus(statement.getPositionStatsReport().getProfitCount());
            lossCount = lossCount.plus(statement.getPositionStatsReport().getLossCount());
            breakEvenCount = breakEvenCount.plus(statement.getPositionStatsReport().getBreakEvenCount());

            Num totalProfitLossPercentageNew = statement.getPerformanceReport().getTotalProfitLossPercentage();
            totalProfitLossPercentage = combinePercentages(totalProfitLossPercentage, series.numOf(currentTickSum), totalProfitLossPercentageNew, series.numOf(series.getBarCount()));
            currentTickSum += series.getBarCount();
        }

        PerformanceReport combinedPerformceReport = new PerformanceReport(totalProfitLoss, totalProfitLossPercentage, totalProfit, totalLoss);
        PositionStatsReport combinedPositionReport = new PositionStatsReport(profitCount, lossCount, breakEvenCount);

        return new TradingStatement(new BaseStrategy(strategyName, new FixedRule(), new FixedRule()), combinedPositionReport, combinedPerformceReport);
    }

    private Num combinePercentages(Num totalProfitLossPercentage, Num currentTickSum, Num totalProfitLossPercentageNew, Num newTickSum) {
        if (currentTickSum.isZero()) {
            return totalProfitLossPercentageNew;
        }
        if (newTickSum.isZero()) {
            return totalProfitLossPercentage;
        }
        Num sumOfAllTicks = currentTickSum.plus(newTickSum);

        Num currentWeight = currentTickSum.dividedBy(sumOfAllTicks);
        Num newWeight = newTickSum.dividedBy(sumOfAllTicks);

        Num weightedNewValue = totalProfitLossPercentageNew.multipliedBy(newWeight);
        Num weightedOldValue = totalProfitLossPercentage.multipliedBy(currentWeight);
        return weightedOldValue.plus(weightedNewValue);
    }

    private Set<BarSeries> loadSeries() {
        List<String> folders = new LinkedList<>();
        folders.add("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\recordedMarketDataOhlc\\");
        folders.add("D:\\Documents\\Programmierung\\bxbot\\recordedMarketDataOhlc\\");

        IntelligentJsonSeriesLoader jsonLoader = new IntelligentJsonSeriesLoader(folders);
        return jsonLoader.loadRecordingsIntoSeries(interval);

    }


    private Indicator<Num> createMinAboveBreakEvenIndicator(BarSeries series, BigDecimal minAbove, SellIndicator breakEvenIndicator) {
        SellIndicator limitIndicator = SellIndicator.createClosepriceSellLimitIndicator(series, minAbove, breakEvenIndicator);
        BigDecimal minimumAboveBreakEvenAsFactor = BigDecimal.ONE.subtract(minAbove);
        TransformIndicator minimalDistanceNeededToBreakEven = TransformIndicator.divide(breakEvenIndicator, minimumAboveBreakEvenAsFactor);
        return CombineIndicator.min(limitIndicator, minimalDistanceNeededToBreakEven);
    }

    private Rule createFixedStopLossGainProfitRule(BarSeries series, SellIndicator breakEvenIndicator, int atrRatio) {
        ATRIndicator trueRangeIndicator = new ATRIndicator(series, 14);
        Number profitGainRatio = 2;
        Number stoplossAtrRatio = atrRatio;
        TransformIndicator trueRangeFactor = multiply(trueRangeIndicator, stoplossAtrRatio);

        Indicator<Num> enterPriceIndicator = SellIndicator.createEnterPriceIndicator(breakEvenIndicator);

        SellIndicator stopLoss = new SellIndicator(series, breakEvenIndicator,
                (entryIndex, index) -> new ConstantIndicator<>(series, minus(enterPriceIndicator, trueRangeFactor).getValue(entryIndex)));
        CombineIndicator factorBetweenStopLossAndEnterPrice = divide(enterPriceIndicator, stopLoss); // eg. 5/4==1.25 long or 4/5 = 0.8 short
        TransformIndicator percentageGainOrLossOfStopLoss = minus(factorBetweenStopLossAndEnterPrice, 1); // 0.25 long or -0.2 short
        Indicator<Num> percentageGainOrLossToReach = multiply(percentageGainOrLossOfStopLoss, profitGainRatio); // 0.25*2 = 0.5 long or -0.2*2 = -0.4
        TransformIndicator factorToReachForGainOrLoss = plus(percentageGainOrLossToReach, 1); // 1.5 long or 0.6 short
        CombineIndicator exitTakeProfitCalculator = multiply(factorToReachForGainOrLoss, enterPriceIndicator); // 5*1.5 = 7.5 Long or 5*0.6 = 3 short

        CombineIndicator halfTakeProfit = plus(enterPriceIndicator, multiply(minus(exitTakeProfitCalculator, enterPriceIndicator), 0.5));
        SellIndicator highestSinceEnter = SellIndicator.createHighestSinceLastEnterIndicator(new HighPriceIndicator(series), breakEvenIndicator);
        Rule belowHalfProfit = new UnderIndicatorRule(highestSinceEnter, halfTakeProfit);
        Rule aboveHalfProfit = new OverIndicatorRule(highestSinceEnter, halfTakeProfit);
        Rule stopLossUnderLineReached = new UnderIndicatorRule(new LowPriceIndicator(series), stopLoss);
        Rule stopLossWhenHalfProfitReached = new UnderIndicatorRule(new LowPriceIndicator(series), enterPriceIndicator).and(aboveHalfProfit);
        Rule stopLossReached = stopLossUnderLineReached
                //.or(stopLossWhenHalfProfitReached)
                ;
        Rule takeProfitReached = new OverIndicatorRule(new HighPriceIndicator(series), exitTakeProfitCalculator);
        return stopLossReached.or(takeProfitReached);
    }

    private static String printReport(List<TradingStatement> tradingStatements) {
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append(System.lineSeparator());
        for (TradingStatement statement : tradingStatements) {
            resultBuilder.append(printStatementReport(statement));
            resultBuilder.append(System.lineSeparator());
        }

        return resultBuilder.toString();
    }

    private static StringBuilder printStatementReport(TradingStatement statement) {
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("######### ").append(statement.getStrategy().getName()).append(" #########")
                .append(System.lineSeparator()).append(printPerformanceReport(statement.getPerformanceReport()))
                .append(System.lineSeparator()).append(printPositionStats(statement.getPositionStatsReport()))
                .append(System.lineSeparator()).append("###########################");
        return resultBuilder;
    }

    private static StringBuilder printPerformanceReport(PerformanceReport report) {
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("--------- performance report ---------").append(System.lineSeparator())
                .append("total loss: ").append(report.getTotalLoss()).append(System.lineSeparator())
                .append("total profit: ").append(report.getTotalProfit()).append(System.lineSeparator())
                .append("total profit loss: " + report.getTotalProfitLoss()).append(System.lineSeparator())
                .append("total profit loss percentage: ").append(report.getTotalProfitLossPercentage())
                .append(System.lineSeparator()).append("---------------------------");
        return resultBuilder;
    }

    private static StringBuilder printPositionStats(PositionStatsReport report) {
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("--------- trade statistics report ---------").append(System.lineSeparator())
                .append("loss trade count: ").append(report.getLossCount()).append(System.lineSeparator())
                .append("profit trade count: ").append(report.getProfitCount()).append(System.lineSeparator())
                .append("break even trade count: ").append(report.getBreakEvenCount()).append(System.lineSeparator())
                .append("---------------------------");
        return resultBuilder;
    }

    private void store(Collection<TradingStatement> results, String suffix) {

        JsonSerializer<Strategy> strategySerializer = (src, typeOfSrc, context) -> context.serialize(src.getName());
        JsonSerializer<Num> numSerializer = (src, typeOfSrc, context) -> context.serialize(src.getDelegate());
        Gson gson = new GsonBuilder().registerTypeAdapter(Strategy.class, strategySerializer).registerTypeAdapter(Num.class, numSerializer).setPrettyPrinting().create();
        switch (interval) {
            case All:
                suffix = suffix + "_all.json";
                break;
            case OneMinute:
                suffix = suffix + "_1min.json";
                break;
            case FiveMinutes:
                suffix = suffix + "_5min.json";
                break;
            case FifteenMinutes:
                suffix = suffix + "_15min.json";
                break;
            default:
                throw new IllegalStateException("Unknown time interval " + interval);
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter("benchmarkResults/OHLC_Benchmarks_" + suffix);
            gson.toJson(results, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class StrategyCreationResult {
        private final Rule entryRule;
        private final Rule exitRule;
        private final SellIndicator breakEvenIndicator;

        StrategyCreationResult(Rule entryRule, Rule exitRule, SellIndicator breakEvenIndicator) {

            this.entryRule = entryRule;
            this.exitRule = exitRule;
            this.breakEvenIndicator = breakEvenIndicator;
        }

        Rule getEntryRule() {
            return entryRule;
        }

        Rule getExitRule() {
            return exitRule;
        }

        SellIndicator getBreakEvenIndicator() {
            return breakEvenIndicator;
        }
    }

    private static class StrategyBenchmarkConfiguration {
        final List<StrategyConfiguration> list;
        final String name;

        public StrategyBenchmarkConfiguration(List<StrategyConfiguration> strategiesForTheSeries, String currentStrategyName) {
            list = strategiesForTheSeries;
            name = currentStrategyName;
        }
    }

    private static class StrategyConfiguration {
        final Strategy strategy;
        final BarSeries series;
        final SellIndicator breakEvenIndicator;

        private StrategyConfiguration(Strategy strategy, BarSeries series, SellIndicator breakEvenIndicator) {
            this.strategy = strategy;
            this.series = series;
            this.breakEvenIndicator = breakEvenIndicator;
        }
    }

    private static class KeltnerRules {
        private final ClosePriceIndicator closePriceIndicator;
        public KeltnerChannelMiddleIndicator midIndicator;
        private Indicator<Num> keltnerLow;
        private Indicator<Num> keltnerHigh;

        private KeltnerRules(BarSeries series) {
            closePriceIndicator = new ClosePriceIndicator(series);
        }

        public void createOuterRules(int keltnerBarCount, double keltnerRatio) {
            keltnerLow = new UnstableIndicator(new KeltnerChannelLowerIndicator(midIndicator, keltnerRatio, keltnerBarCount), keltnerBarCount);
            keltnerHigh = new UnstableIndicator(new KeltnerChannelUpperIndicator(midIndicator, keltnerRatio, keltnerBarCount), keltnerBarCount);
        }
    }

    private static class IchimokuRules {
        private final ClosePriceIndicator closePriceIndicator;
        public Indicator<Num> currentCloudLowerLine;
        public Indicator<Num> baseLine;
        public IchimokuLaggingSpanIndicator laggingSpan;
        public Indicator<Num> delayedMarketPrice;
        public Indicator<Num> conversionLine;
        private Rule entryRule;


        private IchimokuRules(BarSeries series) {
            closePriceIndicator = new ClosePriceIndicator(series);
        }

        public void setEntryRule(Rule entryRule) {
            this.entryRule = entryRule;
        }

        public Rule getEntryRule() {
            return entryRule;
        }
    }
}
