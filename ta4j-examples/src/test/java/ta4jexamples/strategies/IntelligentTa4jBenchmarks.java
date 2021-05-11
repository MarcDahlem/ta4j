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

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SellIndicator;
import org.ta4j.core.indicators.TripleEMAIndicator;
import org.ta4j.core.indicators.UnstableIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelLowerIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelMiddleIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelUpperIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.PerformanceReport;
import org.ta4j.core.reports.PositionStatsReport;
import org.ta4j.core.reports.TradingStatement;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.FixedRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;

import ta4jexamples.loaders.JsonBarsSerializer;
import ta4jexamples.strategies.intelligenthelper.CombineIndicator;
import ta4jexamples.strategies.intelligenthelper.IntelligentTrailIndicator;
import ta4jexamples.strategies.intelligenthelper.TripleKeltnerChannelMiddleIndicator;

public class IntelligentTa4jBenchmarks {

    private static final Logger LOG = LoggerFactory.getLogger(IntelligentTa4jBenchmarks.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.########");
    private Set<BarSeries> allSeries;
    private BigDecimal buyFee;
    private BigDecimal sellFee;
    private double upPercentage;
    private int lookback_max;
    private BigDecimal buyFeeFactor;
    private BigDecimal sellFeeFactor;
    private BigDecimal upPercentageBig;
    private BigDecimal percentageUpperBound;

    @Before
    public void setupTests() {
        allSeries = loadSeries();

        buyFee = new BigDecimal("0.0026");
        sellFee = new BigDecimal("0.0026");
        buyFeeFactor = BigDecimal.ONE.add(buyFee);
        sellFeeFactor = BigDecimal.ONE.subtract(sellFee);

        //upPercentage = 10;
        upPercentage = 1.1545;
        //lookback_max = 11;
        lookback_max = 500;

        upPercentageBig = new BigDecimal(upPercentage);

        percentageUpperBound = new BigDecimal("0.001");
        percentageUpperBound = new BigDecimal("0.1");

    }

    @Test
    public void benchmarkKeltnerTripleOnlyUpAndTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerTripleUp",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createTripleKeltner(i, j, series);

                    Rule entryRule = new OverIndicatorRule(keltnerRules.askPriceIndicator, keltnerRules.keltnerHigh);

                    SellIndicator breakEvenIndicator = SellIndicator.createBreakEvenIndicator(series, buyFee, sellFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.bidPriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkKeltnerDefaultOnlyUpAndTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerUp",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createDefaultKeltner(i, j, series);

                    Rule entryRule = new OverIndicatorRule(keltnerRules.askPriceIndicator, keltnerRules.keltnerHigh);

                    SellIndicator breakEvenIndicator = SellIndicator.createBreakEvenIndicator(series, buyFee, sellFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.bidPriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }



    @Test
    public void benchmarkKeltnerTripleUpAndDownTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerTripleUpDown",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createTripleKeltner(i, j, series);

                    Rule entryRule = new UnderIndicatorRule(keltnerRules.askPriceIndicator, keltnerRules.keltnerLow).or(new OverIndicatorRule(keltnerRules.askPriceIndicator, keltnerRules.keltnerHigh));

                    SellIndicator breakEvenIndicator = SellIndicator.createBreakEvenIndicator(series, buyFee, sellFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.bidPriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkKeltnerDefaultUpAndDownTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerUpDown",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createDefaultKeltner(i, j, series);

                    Rule entryRule = new UnderIndicatorRule(keltnerRules.askPriceIndicator, keltnerRules.keltnerLow).or(new OverIndicatorRule(keltnerRules.askPriceIndicator, keltnerRules.keltnerHigh));

                    SellIndicator breakEvenIndicator = SellIndicator.createBreakEvenIndicator(series, buyFee, sellFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.bidPriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void keltnerRulesKeltnerTripleDownAndTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerTripleDown",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createTripleKeltner(i,j,series);

                    Rule entryRule = new UnderIndicatorRule(keltnerRules.askPriceIndicator, keltnerRules.keltnerLow);

                    SellIndicator breakEvenIndicator = SellIndicator.createBreakEvenIndicator(series, buyFee, sellFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    Rule exitRule = new UnderIndicatorRule(keltnerRules.bidPriceIndicator, intelligentTrailIndicator);

                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkKeltnerDefaultDownTrailingTa4j() {
        runBenchmarkForTwoVariables("Ta4jKeltnerDown",
                i -> j -> series -> {
                    KeltnerRules keltnerRules = createDefaultKeltner(i,j,series);

                    Rule entryRule = new UnderIndicatorRule(keltnerRules.askPriceIndicator, keltnerRules.keltnerLow);

                    SellIndicator breakEvenIndicator = SellIndicator.createBreakEvenIndicator(series, buyFee, sellFee);
                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.07"), breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, new BigDecimal("0.02"), breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, new BigDecimal("0.01"), breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(keltnerRules.bidPriceIndicator, intelligentTrailIndicator);
                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                }
        );
    }

    @Test
    public void benchmarkEmaTa4j() {
        runBenchmarkForFourVariables("Ta4jMacd",
                i -> j -> k -> l -> series -> {
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
                    LowPriceIndicator bidPriceIndicator = new LowPriceIndicator(series);
                    HighPriceIndicator askPriceIndicator = new HighPriceIndicator(series);

                    //StochasticOscillatorKIndicator stochasticOscillaltorK = new StochasticOscillatorKIndicator(series, 140);
                    //MACDIndicator macd = new MACDIndicator(closePriceIndicator, 90, 260);
                    //EMAIndicator emaMacd = new EMAIndicator(macd, 180);


                    EMAIndicator buyIndicatorLong = new EMAIndicator(bidPriceIndicator, Math.toIntExact(i));
                    TransformIndicator buyIndicatorShort = TransformIndicator.multiply(new EMAIndicator(bidPriceIndicator, Math.toIntExact(j)), sellFeeFactor);

                    EMAIndicator sellIndicatorLong = new EMAIndicator(askPriceIndicator, Math.toIntExact(k));
                    TransformIndicator sellIndicatorShort = TransformIndicator.multiply(new EMAIndicator(askPriceIndicator, Math.toIntExact(l)), buyFeeFactor);

                    Rule entryRule = new CrossedUpIndicatorRule(buyIndicatorShort, buyIndicatorLong) // Trend
                            //.and(new UnderIndicatorRule(stochasticOscillaltorK, 20)) // Signal 1
                            //.and(new OverIndicatorRule(macd, emaMacd)); // Signal 2
                            ;

                    Rule exitRule = new CrossedDownIndicatorRule(sellIndicatorShort, sellIndicatorLong) // Trend
                            //.and(new OverIndicatorRule(stochasticOscillaltorK, 80)) // Signal 1
                            //.and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2
                            ;
                    return new StrategyCreationResult(entryRule, exitRule, null);
                });
    }

    @Test
    public void benchmarkTa4jTrailing() {
        runBenchmarkForSixVariablesPercentage("Ta4jTrailing",
                i -> j -> k -> below -> above -> minAbove -> series -> {
                    ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
                    LowPriceIndicator bidPriceIndicator = new LowPriceIndicator(series);
                    HighPriceIndicator askPriceIndicator = new HighPriceIndicator(series);
                    LowestValueIndicator buyLongIndicator = new LowestValueIndicator(askPriceIndicator, Math.toIntExact(i));
                    LowestValueIndicator buyShortIndicator = new LowestValueIndicator(askPriceIndicator, Math.toIntExact(j));
                    TransformIndicator buyGainLine = TransformIndicator.multiply(buyLongIndicator, BigDecimal.ONE.add(k));

                    SellIndicator breakEvenIndicator = SellIndicator.createBreakEvenIndicator(series, buyFee, sellFee);

                    OverIndicatorRule entryRule = new OverIndicatorRule(buyShortIndicator, buyGainLine);


                    Indicator<Num> belowBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, below, breakEvenIndicator);
                    Indicator<Num> aboveBreakEvenIndicator = SellIndicator.createSellLimitIndicator(series, above, breakEvenIndicator);
                    Indicator<Num> minAboveBreakEvenIndicator = createMinAboveBreakEvenIndicator(series, minAbove, breakEvenIndicator);

                    IntelligentTrailIndicator intelligentTrailIndicator = new IntelligentTrailIndicator(belowBreakEvenIndicator, aboveBreakEvenIndicator, minAboveBreakEvenIndicator, breakEvenIndicator);
                    UnderIndicatorRule exitRule = new UnderIndicatorRule(bidPriceIndicator, intelligentTrailIndicator);

                    return new StrategyCreationResult(entryRule, exitRule, breakEvenIndicator);
                });
    }

    private void runBenchmarkForTwoVariables(
            String benchmarkName,
            Function<Integer, Function<Integer, Function<BarSeries, StrategyCreationResult>>> strategyCreator
    ) {
        Queue<StrategyBenchmarkConfiguration> strategies = new LinkedList<>();

        for (long i = 1; i < lookback_max; i = Math.round(Math.ceil(i * upPercentage))) {
            for (long j = 1; j < lookback_max; j = Math.round(Math.ceil(j * upPercentage))) {
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

    private KeltnerRules createTripleKeltner(Integer keltnerBarCount, Integer keltnerRatio, BarSeries series) {
        KeltnerRules result = new KeltnerRules(series);
        result.midAskIndicator = new TripleKeltnerChannelMiddleIndicator(result.askPriceIndicator, keltnerBarCount);
        result.midBidIndicator = new TripleKeltnerChannelMiddleIndicator(result.bidPriceIndicator, keltnerBarCount);
        result.createOuterRules(keltnerBarCount, keltnerRatio);
        return result;
    }

    private KeltnerRules createDefaultKeltner(Integer keltnerBarCount, Integer keltnerRatio, BarSeries series) {
        KeltnerRules result = new KeltnerRules(series);
        result.midAskIndicator = new KeltnerChannelMiddleIndicator(result.askPriceIndicator, keltnerBarCount);
        result.midBidIndicator = new KeltnerChannelMiddleIndicator(result.bidPriceIndicator, keltnerBarCount);
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
                for (long k = 26; k < lookback_max; k = Math.round(Math.ceil(k * upPercentage))) {
                    for (long l = 9; l < k; l = Math.round(Math.ceil(l * upPercentage))) {
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

                BacktestExecutor bte = new BacktestExecutor(entry.series, new LinearTransactionCostModel(0.0026), new ZeroCostModel());
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
            if (counter % 2 == 0) {
                System.gc();
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

        Num currentWeight = currentTickSum.dividedBy (sumOfAllTicks);
        Num newWeight = newTickSum.dividedBy(sumOfAllTicks);

        Num weightedNewValue = totalProfitLossPercentageNew.multipliedBy(newWeight);
        Num weightedOldValue = totalProfitLossPercentage.multipliedBy(currentWeight);
        return weightedOldValue.plus(weightedNewValue);
    }

    private Set<BarSeries> loadSeries() {
        List<String> folders = new LinkedList<>();
        folders.add("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\recordedMarketData\\");
        folders.add("D:\\Documents\\Programmierung\\bxbot\\recordedMarketData\\");

        Set<BarSeries> result = new HashSet<>();
        for (String folder : folders) {
            File f = new File(folder);

            FilenameFilter filter = (f1, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json");

            String[] pathnames = f.list(filter);
            for (String path : pathnames) {
                result.add(JsonBarsSerializer.loadSeries(folder + File.separator + path));
            }
        }
        return result;

    }


    private Indicator<Num> createMinAboveBreakEvenIndicator(BarSeries series, BigDecimal minAbove, SellIndicator breakEvenIndicator) {
        SellIndicator limitIndicator = SellIndicator.createSellLimitIndicator(series, minAbove, breakEvenIndicator);
        BigDecimal minimumAboveBreakEvenAsFactor = BigDecimal.ONE.subtract(minAbove);
        TransformIndicator minimalDistanceNeededToBreakEven = TransformIndicator.divide(breakEvenIndicator, minimumAboveBreakEvenAsFactor);
        return CombineIndicator.min(limitIndicator, minimalDistanceNeededToBreakEven);
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
        FileWriter writer = null;
        try {

            //if (false) throw new IOException("never thrown");
            //else LOG.debug("Gson writing disabled");
            writer = new FileWriter("Benchmarks_" + suffix + ".json");
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
        private final LowPriceIndicator bidPriceIndicator;
        private final HighPriceIndicator askPriceIndicator;
        public KeltnerChannelMiddleIndicator midAskIndicator;
        public KeltnerChannelMiddleIndicator midBidIndicator;
        private Indicator<Num> keltnerLow;
        private Indicator<Num> keltnerHigh;

        private KeltnerRules(BarSeries series) {
            closePriceIndicator = new ClosePriceIndicator(series);
            bidPriceIndicator = new LowPriceIndicator(series);
            askPriceIndicator = new HighPriceIndicator(series);
        }

        public void createOuterRules(int keltnerBarCount, double keltnerRatio) {
            keltnerLow =  new UnstableIndicator(new KeltnerChannelLowerIndicator(midAskIndicator, keltnerRatio, keltnerBarCount), keltnerBarCount);
            keltnerHigh = new UnstableIndicator(new KeltnerChannelUpperIndicator(midBidIndicator, keltnerRatio, keltnerBarCount), keltnerBarCount);
        }
    }
}