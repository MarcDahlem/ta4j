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

import com.google.gson.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;
import org.ta4j.core.cost.LinearTransactionCostModel;
import org.ta4j.core.cost.ZeroCostModel;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.PerformanceReport;
import org.ta4j.core.reports.PositionStatsReport;
import org.ta4j.core.reports.TradingStatement;
import org.ta4j.core.rules.*;
import ta4jexamples.loaders.JsonBarsSerializer;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

public class MovingMomentumStrategyTest {

    private static final Logger LOG = LoggerFactory.getLogger(MovingMomentumStrategyTest.class);


    @Test
    public void test() {
        MovingMomentumStrategy.main(null);
    }

    @Test
    public void testMineTa4j() {
        Set<BarSeries> allSeries = loadSeries();

        BigDecimal buyFee = new BigDecimal("0.0026");
        BigDecimal sellFee = new BigDecimal("0.0026");
        BigDecimal buyFeeFactor = BigDecimal.ONE.add(buyFee);
        BigDecimal sellFeeFactor = BigDecimal.ONE.subtract(sellFee);

        double upPercentage = 1.9;
        int lookback_max = 100;

        Queue<Map.Entry<List<Map.Entry<Strategy, BarSeries>>, String>> strategies = new LinkedList<>();


        for (long i = 26; i < lookback_max; i = Math.round(Math.ceil(i * upPercentage))) {
            for (long j = 9; j < i; j = Math.round(Math.ceil(j * upPercentage))) {
                for (long k = 26; k < lookback_max; k = Math.round(Math.ceil(k * upPercentage))) {
                    for (long l = 9; l < k; l = Math.round(Math.ceil(l * upPercentage))) {
                        String currentStrategyName = "i(" + i + "), j(" + j + "), k(" + k + "),l(" + l + ")";
                        LOG.info(currentStrategyName);
                        List<Map.Entry<Strategy, BarSeries>> strategiesForTheSeries = new LinkedList<>();
                        for (BarSeries series : allSeries) {
                            ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
                            LowPriceIndicator bidPriceIndicator = new LowPriceIndicator(series);
                            HighPriceIndicator askPriceIndicator = new HighPriceIndicator(series);

                            StochasticOscillatorKIndicator stochasticOscillaltorK = new StochasticOscillatorKIndicator(series, 140);
                            MACDIndicator macd = new MACDIndicator(closePriceIndicator, 90, 260);
                            EMAIndicator emaMacd = new EMAIndicator(macd, 180);


                            EMAIndicator buyIndicatorLong = new EMAIndicator(bidPriceIndicator, Math.toIntExact(i));
                            TransformIndicator buyIndicatorShort = TransformIndicator.multiply(new EMAIndicator(bidPriceIndicator, Math.toIntExact(j)), sellFeeFactor);

                            EMAIndicator sellIndicatorLong = new EMAIndicator(askPriceIndicator, Math.toIntExact(k));
                            TransformIndicator sellIndicatorShort = TransformIndicator.multiply(new EMAIndicator(askPriceIndicator, Math.toIntExact(l)), buyFeeFactor);

                            Rule entryRule = new CrossedUpIndicatorRule(buyIndicatorShort, buyIndicatorLong) // Trend
                                    /*.and(new UnderIndicatorRule(stochasticOscillaltorK, 20)) // Signal 1*/;/*.and(new OverIndicatorRule(macd, emaMacd)); // Signal 2*/

                            Rule exitRule = new CrossedDownIndicatorRule(sellIndicatorShort, sellIndicatorLong) // Trend
                                    /*.and(new OverIndicatorRule(stochasticOscillaltorK, 80)) // Signal 1*/;/*.and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2*/
                            strategiesForTheSeries.add(new AbstractMap.SimpleEntry<>(new BaseStrategy(currentStrategyName, entryRule, exitRule), series));
                        }
                        strategies.offer(new AbstractMap.SimpleEntry<>(strategiesForTheSeries, currentStrategyName));
                    }
                }
            }

        }

        List<TradingStatement> result = new LinkedList<>();
        int counter = 0;
        int originalSize = strategies.size();
        while (strategies.size() > 0) {
            counter++;
            LOG.info("Executing ta4j strategies " + counter + "/" + originalSize);

            Map.Entry<List<Map.Entry<Strategy, BarSeries>>, String> strats = strategies.poll();
            List<TradingStatement> currentSeriesResult = new LinkedList<>();
            for (Map.Entry<Strategy, BarSeries> entry : strats.getKey()) {
                BacktestExecutor bte = new BacktestExecutor(entry.getValue(), new LinearTransactionCostModel(0.0026), new ZeroCostModel());
                List<Strategy> toBeExecuted = new LinkedList<>();
                toBeExecuted.add(entry.getKey());
                currentSeriesResult.addAll(bte.execute(toBeExecuted, entry.getValue().numOf(25), Trade.TradeType.BUY));

                entry.getKey().destroy();
            }
            result.add(combineTradingStatements(currentSeriesResult, strats.getValue()));
            if (counter % 1000 == 0) {
                System.gc();
            }
        }

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
        LOG.info("---Worst result:--- \n" + printReport(result.subList(0, 1)) + "\n-------------");
        LOG.info("---best results:--- \n" + printReport(result.subList(result.size() - 10, result.size())) + "\n-------------");
        store(result, "_Ta4jMacd_" + System.currentTimeMillis() + "_steps_" + upPercentage + "_maxLookback_" + lookback_max);
    }

    private TradingStatement combineTradingStatements(List<TradingStatement> statements, String strategyName) {
        Num totalProfitLoss = DecimalNum.valueOf(0);
        Num totalProfitLossPercentage = DecimalNum.valueOf(0);
        Num totalProfit = DecimalNum.valueOf(0);
        Num totalLoss = DecimalNum.valueOf(0);

        Num profitCount = DecimalNum.valueOf(0);
        Num lossCount = DecimalNum.valueOf(0);
        Num breakEvenCount = DecimalNum.valueOf(0);

        for (TradingStatement statement : statements) {
            totalProfitLoss = totalProfitLoss.plus(statement.getPerformanceReport().getTotalProfitLoss());
            totalProfit = totalProfit.plus(statement.getPerformanceReport().getTotalProfit());
            totalLoss = totalLoss.plus(statement.getPerformanceReport().getTotalLoss());

            profitCount = profitCount.plus(statement.getPositionStatsReport().getProfitCount());
            lossCount = lossCount.plus(statement.getPositionStatsReport().getLossCount());
            breakEvenCount = breakEvenCount.plus(statement.getPositionStatsReport().getBreakEvenCount());
        }

        PerformanceReport combinedPerformceReport = new PerformanceReport(totalProfitLoss, totalProfitLossPercentage, totalProfit, totalLoss);
        PositionStatsReport combinedPositionReport = new PositionStatsReport(profitCount, lossCount, breakEvenCount);

        return new TradingStatement(new BaseStrategy(strategyName, new FixedRule(), new FixedRule()), combinedPositionReport, combinedPerformceReport);
    }

    private Set<BarSeries> loadSeries() {
        Set<BarSeries> result = new HashSet<>();
        result.add(JsonBarsSerializer.loadSeries("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\barData_1618580976288.json"));
        result.add(JsonBarsSerializer.loadSeries("D:\\Documents\\Programmierung\\bxbot\\barData_1618533095982.json"));
        result.add(JsonBarsSerializer.loadSeries("D:\\Documents\\Programmierung\\bxbot\\barData_1618527900760.json"));
        result.add(JsonBarsSerializer.loadSeries("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\XRPEUR1619755007893.json"));
        result.add(JsonBarsSerializer.loadSeries("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\XRPEUR1619776350101.json"));
        result.add(JsonBarsSerializer.loadSeries("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\XRPEUR1619822902408.json"));
        result.add(JsonBarsSerializer.loadSeries("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\XRPEUR1619898650211.json"));
        result.add(JsonBarsSerializer.loadSeries("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\XRPEUR1619943515150.json"));
        return result;

    }

    @Test
    public void testMineTa4jTrailing() {
        BarSeries series = JsonBarsSerializer.loadSeries("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\barData_1618580976288.json");
        BigDecimal buyFee = new BigDecimal("0.0026");
        BigDecimal sellFee = new BigDecimal("0.0026");

        DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.########");

        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        LowPriceIndicator bidPriceIndicator = new LowPriceIndicator(series);
        HighPriceIndicator askPriceIndicator = new HighPriceIndicator(series);

        Queue<Map.Entry<Strategy, SellIndicator>> strategies = new LinkedList<>();

        double upPercentage = 1.309;
        BigDecimal upPercentageBig = new BigDecimal(upPercentage);

        int lookback_max = 500;

        BigDecimal percentageUpperBound = new BigDecimal("0.1");

        for (long i = 1; i < lookback_max; i = Math.round(Math.ceil(i * upPercentage))) { // lookback long
            for (long j = 1; j < i; j = Math.round(Math.ceil(j * upPercentage))) { //lookback short
                for (BigDecimal k = new BigDecimal("0.001"); k.compareTo(percentageUpperBound) <= 0; k = k.multiply(upPercentageBig)) { // buy up percentage needed
                    for (BigDecimal below = new BigDecimal("0.001"); below.compareTo(percentageUpperBound) <= 0; below = below.multiply(upPercentageBig)) {
                        for (BigDecimal above = new BigDecimal("0.001"); above.compareTo(percentageUpperBound) <= 0; above = above.multiply(upPercentageBig)) {
                            for (BigDecimal minAbove = new BigDecimal("0.001"); minAbove.compareTo(above) <= 0; minAbove = minAbove.multiply(upPercentageBig)) {
                                String currentStrategyName = "i(" + i + "), j(" + j + "), k(" + DECIMAL_FORMAT.format(k) + "),below(" + DECIMAL_FORMAT.format(below) + "),above(" + DECIMAL_FORMAT.format(above) + "),minAbove(" + DECIMAL_FORMAT.format(minAbove) + ")";
                                LOG.info(currentStrategyName);
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

                                strategies.offer(new AbstractMap.SimpleEntry<>(new BaseStrategy(currentStrategyName, entryRule, exitRule), breakEvenIndicator));
                            }
                        }
                    }
                }
            }
        }


        BacktestExecutor bte = new BacktestExecutor(series, new LinearTransactionCostModel(0.0026), new ZeroCostModel());
        List<TradingStatement> result = new LinkedList<>();
        int counter = 0;
        int originalSize = strategies.size();
        while (strategies.size() > 0) {
            Map.Entry<Strategy, SellIndicator> strat = strategies.poll();
            counter++;
            LOG.info("Executing ta4j-trailing strategy " + counter + "/" + originalSize);
            Map<Strategy, SellIndicator> listToBeExecuted = new HashMap<>();
            listToBeExecuted.put(strat.getKey(), strat.getValue());
            result.addAll(bte.execute(listToBeExecuted, series.numOf(25), Trade.TradeType.BUY));
            strat.getKey().destroy();
            if (counter % 300 == 0) {
                System.gc();
            }
        }

        result.sort((o1, o2) -> {
            Num trades1 = o1.getPositionStatsReport().getLossCount().plus(o1.getPositionStatsReport().getProfitCount()).plus(o1.getPositionStatsReport().getBreakEvenCount());
            Num trades2 = o2.getPositionStatsReport().getLossCount().plus(o2.getPositionStatsReport().getProfitCount()).plus(o2.getPositionStatsReport().getBreakEvenCount());

            if (trades1.isLessThanOrEqual(series.numOf(1))) {
                if (trades2.isLessThanOrEqual(series.numOf(1))) {
                    return 0;
                } else {
                    return -1;
                }
            }
            if (trades2.isLessThanOrEqual(series.numOf(1))) {
                return 1;
            }

            return o1.getPerformanceReport().getTotalProfitLoss().compareTo(o2.getPerformanceReport().getTotalProfitLoss());
        });
        LOG.info(printReport(result.subList(0, 1)));
        LOG.info(printReport(result.subList(result.size() - 10, result.size())));
        store(result, "_Ta4jTrailing_" + System.currentTimeMillis() + "_steps_" + upPercentage + "_maxLookback_" + lookback_max + "_maxPercentage_" + DECIMAL_FORMAT.format(percentageUpperBound));
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

}
