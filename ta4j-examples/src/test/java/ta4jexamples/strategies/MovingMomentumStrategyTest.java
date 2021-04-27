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
package ta4jexamples.strategies;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;
import org.ta4j.core.cost.LinearTransactionCostModel;
import org.ta4j.core.cost.ZeroCostModel;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.reports.PerformanceReport;
import org.ta4j.core.reports.PositionStatsReport;
import org.ta4j.core.reports.TradingStatement;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import ta4jexamples.backtesting.SimpleMovingAverageRangeBacktest;
import ta4jexamples.loaders.CsvBarsLoader;
import ta4jexamples.loaders.CsvTradesLoader;
import ta4jexamples.loaders.JsonBarsSerializer;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class MovingMomentumStrategyTest {

    private static final Logger LOG = LoggerFactory.getLogger(MovingMomentumStrategyTest.class);


    @Test
    public void test() {
        MovingMomentumStrategy.main(null);
    }

    @Test
    public void testMine() {
        BarSeries series = JsonBarsSerializer.loadSeries("C:\\Users\\Marc\\Documents\\Programmierung\\bxbot-working\\barData_1618580976288.json");
        //BarSeries series = CsvTradesLoader.loadBitstampSeries();
        //BarSeries series = CsvBarsLoader.loadAppleIncSeries();
        BigDecimal buyFee = new BigDecimal("0.0026");
        BigDecimal sellFee = new BigDecimal("0.0026");

        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        LowPriceIndicator bidPriceIndicator = new LowPriceIndicator(series);
        HighPriceIndicator askPriceIndicator = new HighPriceIndicator(series);

        List<Strategy> strategies = new LinkedList<>();


        StochasticOscillatorKIndicator stochasticOscillaltorK = new StochasticOscillatorKIndicator(series, 140);
        MACDIndicator macd = new MACDIndicator(closePriceIndicator, 90, 260);
        EMAIndicator emaMacd = new EMAIndicator(macd, 180);

        BigDecimal buyFeeFactor = BigDecimal.ONE.add(buyFee);
        BigDecimal sellFeeFactor = BigDecimal.ONE.subtract(sellFee);

        double upPercentage = 1.618;

        for (long i = 6; i<500; i=Math.round(Math.ceil(i * upPercentage))) {
            for (long j = 6; j<i; j=Math.round(Math.ceil(j * upPercentage))) {
                for (long k = 6; k < 500; k=Math.round(Math.ceil(k * upPercentage))) {
                    for (long l = 6; l < k; l=Math.round(Math.ceil(l * upPercentage))) {
                        LOG.info("i(" + i + "), j(" + j + "), k(" +k + "),l("+l+")");
                        EMAIndicator buyIndicatorLong = new EMAIndicator(bidPriceIndicator,  Math.toIntExact(i));
                        TransformIndicator buyIndicatorShort = TransformIndicator.multiply(new EMAIndicator(bidPriceIndicator,  Math.toIntExact(j)), sellFeeFactor);

                        EMAIndicator sellIndicatorLong = new EMAIndicator(askPriceIndicator,  Math.toIntExact(k));
                        TransformIndicator sellIndicatorShort = TransformIndicator.multiply(new EMAIndicator(askPriceIndicator,  Math.toIntExact(l)), buyFeeFactor);

                        Rule entryRule = new CrossedUpIndicatorRule(buyIndicatorShort, buyIndicatorLong) // Trend
                                /*.and(new UnderIndicatorRule(stochasticOscillaltorK, 20)) // Signal 1*/;/*.and(new OverIndicatorRule(macd, emaMacd)); // Signal 2*/

                        Rule exitRule = new CrossedDownIndicatorRule(sellIndicatorShort, sellIndicatorLong) // Trend
                                /*.and(new OverIndicatorRule(stochasticOscillaltorK, 80)) // Signal 1*/;/*.and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2*/
                        strategies.add(new BaseStrategy("i(" + i + "), j(" + j + "), k(" +k + "),l("+l+")" , entryRule, exitRule));
                    }
                }
            }
        }


        BacktestExecutor bte = new BacktestExecutor(series, new LinearTransactionCostModel(0.0026), new ZeroCostModel());
        List<TradingStatement> result = bte.execute(strategies, series.numOf(25), Trade.TradeType.BUY);

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
        LOG.info(printReport(result.subList(0,1)));
        LOG.info(printReport(result.subList(result.size()-10,result.size())));
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
}
