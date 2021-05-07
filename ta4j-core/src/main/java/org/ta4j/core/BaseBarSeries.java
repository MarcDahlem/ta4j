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
package org.ta4j.core;

import static org.ta4j.core.num.NaN.NaN;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

/**
 * Base implementation of a {@link BarSeries}.
 * </p>
 */
public class BaseBarSeries implements BarSeries {

    private static final long serialVersionUID = -1878027009398790126L;
    /**
     * Name for unnamed series
     */
    private static final String UNNAMED_SERIES_NAME = "unnamed_series";
    /**
     * Num type function
     **/
    protected final transient Function<Number, Num> numFunction;
    /**
     * The logger
     */
    private final transient Logger log = LoggerFactory.getLogger(getClass());
    /**
     * Name of the series
     */
    private final String name;
    /**
     * The actual bars. To old bars are automatically removed by the cache
     */
    private final Ta4jCache<Bar> barCache;

    /**
     * Maximum number of bars for the bar series
     */
    private int maximumBarCount = Integer.MAX_VALUE;
    /**
     * True if the current series is constrained (i.e. its indexes cannot change),
     * false otherwise
     */
    private boolean constrained;

    /**
     * Constructor of an unnamed series.
     */
    public BaseBarSeries() {
        this(UNNAMED_SERIES_NAME);
    }

    /**
     * Constructor.
     *
     * @param name the name of the series
     */
    public BaseBarSeries(String name) {
        this(name, new ArrayList<>());
    }

    /**
     * Constructor of an unnamed series.
     *
     * @param bars the list of bars of the series
     */
    public BaseBarSeries(List<Bar> bars) {
        this(UNNAMED_SERIES_NAME, bars);
    }

    /**
     * Constructor.
     *
     * @param name the name of the series
     * @param bars the list of bars of the series
     */
    public BaseBarSeries(String name, List<Bar> bars) {
        this(name, bars, 0, bars.size() - 1, false);
    }

    /**
     * Constructor.
     * 
     * @param name        the name of the series
     * @param numFunction a {@link Function} to convert a {@link Number} to a
     *                    {@link Num Num implementation}
     */
    public BaseBarSeries(String name, Function<Number, Num> numFunction) {
        this(name, new ArrayList<>(), numFunction);
    }

    /**
     * Constructor.
     *
     * @param name the name of the series
     * @param bars the list of bars of the series
     */
    public BaseBarSeries(String name, List<Bar> bars, Function<Number, Num> numFunction) {
        this(name, bars, 0, bars.size() - 1, false, numFunction);
    }

    /**
     * Constructor.
     * <p/>
     * Creates a BaseBarSeries with default {@link DecimalNum} as type for the data
     * and all operations on it
     *
     * @param name             the name of the series
     * @param bars             the list of bars of the series
     * @param seriesBeginIndex the begin index (inclusive) of the bar series
     * @param seriesEndIndex   the end index (inclusive) of the bar series
     * @param constrained      true to constrain the bar series (i.e. indexes cannot
     *                         change), false otherwise
     */
    private BaseBarSeries(String name, List<Bar> bars, int seriesBeginIndex, int seriesEndIndex, boolean constrained) {
        this(name, bars, seriesBeginIndex, seriesEndIndex, constrained, DecimalNum::valueOf);
    }

    /**
     * Constructor.
     *
     * @param name             the name of the series
     * @param bars             the list of bars of the series
     * @param seriesBeginIndex the begin index (inclusive) of the bar series
     * @param seriesEndIndex   the end index (inclusive) of the bar series
     * @param constrained      true to constrain the bar series (i.e. indexes cannot
     *                         change), false otherwise
     * @param numFunction      a {@link Function} to convert a {@link Number} to a
     *                         {@link Num Num implementation}
     */
    BaseBarSeries(String name, List<Bar> bars, int seriesBeginIndex, int seriesEndIndex, boolean constrained,
            Function<Number, Num> numFunction) {
        this.name = name;
        this.barCache = new Ta4jCache<>(this);

        if (bars.isEmpty()) {
            // Bar list empty
            this.constrained = false;
            this.numFunction = numFunction;
            return;
        }
        // Bar list not empty: take Function of first bar
        this.numFunction = bars.get(0).getClosePrice().function();
        // Bar list not empty: checking num types
        if (!checkBars(bars)) {
            throw new IllegalArgumentException(String.format(
                    "Num implementation of bars: %s" + " does not match to Num implementation of bar series: %s",
                    bars.get(0).getClosePrice().getClass(), numFunction));
        }
        // Bar list not empty: checking indexes
        if (seriesEndIndex < seriesBeginIndex - 1) {
            throw new IllegalArgumentException("End index must be >= to begin index - 1");
        }
        if (seriesEndIndex >= bars.size()) {
            throw new IllegalArgumentException("End index must be < to the bar list size");
        }

        int currentBarIndex = seriesBeginIndex;
        for(Bar bar: bars) {
            barCache.add(currentBarIndex, bar);
            currentBarIndex++;
        }
        this.constrained = constrained;
    }

    /**
     * Cuts a list of bars into a new list of bars that is a subset of it
     *
     * @param bars       the list of {@link Bar bars}
     * @param startIndex start index of the subset
     * @param endIndex   end index of the subset
     * @return a new list of bars with tick from startIndex (inclusive) to endIndex
     *         (exclusive)
     */
    private static List<Bar> cut(List<Bar> bars, final int startIndex, final int endIndex) {
        return new ArrayList<>(bars.subList(startIndex, endIndex));
    }

    /**
     * @param series a bar series
     * @param index  an out of bounds bar index
     * @return a message for an OutOfBoundsException
     */
    private static String buildOutOfBoundsMessage(BaseBarSeries series, int index) {
        return String.format("Size of series: %s bars, %s bars removed, index = %s", series.barCache.size(),
                series.barCache.firstAvailableIndex(), index);
    }

    /**
     * Returns a new BaseBarSeries that is a subset of this BaseBarSeries. The new
     * series holds a copy of all {@link Bar bars} between <tt>startIndex</tt>
     * (inclusive) and <tt>endIndex</tt> (exclusive) of this BaseBarSeries. The
     * indices of this BaseBarSeries and the new subset BaseBarSeries can be
     * different. I. e. index 0 of the new BaseBarSeries will be index
     * <tt>startIndex</tt> of this BaseBarSeries. If <tt>startIndex</tt> <
     * this.seriesBeginIndex the new BaseBarSeries will start with the first
     * available Bar of this BaseBarSeries. If <tt>endIndex</tt> >
     * this.seriesEndIndex+1 the new BaseBarSeries will end at the last available
     * Bar of this BaseBarSeries
     *
     * @param startIndex the startIndex (inclusive)
     * @param endIndex   the endIndex (exclusive)
     * @return a new BarSeries with Bars from startIndex to endIndex-1
     * @throws IllegalArgumentException if endIndex <= startIndex or startIndex < 0
     */
    @Override
    public BaseBarSeries getSubSeries(int startIndex, int endIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException(String.format("the startIndex: %s must not be negative", startIndex));
        }
        if (startIndex >= endIndex) {
            throw new IllegalArgumentException(
                    String.format("the endIndex: %s must be greater than startIndex: %s", endIndex, startIndex));
        }
        if (!barCache.isEmpty()) {
            int start = Math.max(startIndex - barCache.firstAvailableIndex(), this.getBeginIndex());
            int end = Math.min(endIndex - barCache.firstAvailableIndex(), this.getEndIndex() + 1);
            return new BaseBarSeries(getName(), cut(getBarData(), start, end), numFunction);
        }
        return new BaseBarSeries(name, numFunction);

    }

    @Override
    public Num numOf(Number number) {
        return this.numFunction.apply(number);
    }

    @Override
    public Function<Number, Num> function() {
        return numFunction;
    }

    /**
     * Checks if all {@link Bar bars} of a list fits to the {@link Num NumFunction}
     * used by this bar series.
     *
     * @param bars a List of Bar objects.
     * @return false if a Num implementation of at least one Bar does not fit.
     */
    private boolean checkBars(List<Bar> bars) {
        for (Bar bar : bars) {
            if (!checkBar(bar)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the {@link Num} implementation of a {@link Bar} fits to the
     * NumFunction used by bar series.
     *
     * @param bar a Bar object.
     * @return false if another Num implementation is used than by this bar series.
     * @see Num
     * @see Bar
     * @see #addBar(Duration, ZonedDateTime)
     */
    private boolean checkBar(Bar bar) {
        if (bar.getClosePrice() == null) {
            return true; // bar has not been initialized with data (uses deprecated constructor)
        }
        // all other constructors initialize at least the close price, check if Num
        // implementation fits to numFunction
        Class<? extends Num> f = numOf(1).getClass();
        return f == bar.getClosePrice().getClass() || bar.getClosePrice().equals(NaN);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Bar getBar(int i) {
        if(barCache.isEmpty()) {
            throw new IndexOutOfBoundsException(buildOutOfBoundsMessage(this, i));
        }
        if (i<barCache.firstAvailableIndex()) {
            log.trace("Bar series `{}` ({} bars): bar {} already removed, use {}-th instead", name, barCache.size(), i,
                    barCache.firstAvailableIndex());
            return barCache.get(barCache.firstAvailableIndex());
        }
        if(!barCache.contains(i)) {
            throw new IndexOutOfBoundsException(buildOutOfBoundsMessage(this, i));
        }
        return barCache.get(i);
    }

    @Override
    public int getBarCount() {
        return barCache.size();
    }

    @Override
    public List<Bar> getBarData() {
        return barCache.values();
    }

    @Override
    public int getBeginIndex() {
        return barCache.firstAvailableIndex();
    }

    @Override
    public int getEndIndex() {
        return barCache.lastAvailableIndex();
    }

    @Override
    public int getMaximumBarCount() {
        return maximumBarCount;
    }

    @Override
    public void setMaximumBarCount(int maximumBarCount) {
        if (constrained) {
            throw new IllegalStateException("Cannot set a maximum bar count on a constrained bar series");
        }
        if (maximumBarCount <= 0) {
            throw new IllegalArgumentException("Maximum bar count must be strictly positive");
        }
        this.maximumBarCount = maximumBarCount;
        barCache.removeFirstEntriesIfNeeded();
    }

    /**
     * @param bar the <code>Bar</code> to be added
     * @apiNote to add bar data directly use #addBar(Duration, ZonedDateTime, Num,
     *          Num, Num, Num, Num)
     */
    @Override
    public void addBar(Bar bar, boolean replace) {
        Objects.requireNonNull(bar);
        if (!checkBar(bar)) {
            throw new IllegalArgumentException(
                    String.format("Cannot add Bar with data type: %s to series with data" + "type: %s",
                            bar.getClosePrice().getClass(), numOf(1).getClass()));
        }
        if (!barCache.isEmpty()) {
            if (replace) {
                barCache.add(barCache.lastAvailableIndex(), bar);
                return;
            }
            final int lastBarIndex = barCache.lastAvailableIndex();
            ZonedDateTime seriesEndTime = barCache.get(lastBarIndex).getEndTime();
            if (!bar.getEndTime().isAfter(seriesEndTime)) {
                throw new IllegalArgumentException(
                        String.format("Cannot add a bar with end time:%s that is <= to series end time: %s",
                                bar.getEndTime(), seriesEndTime));
            }
        }

        barCache.add(barCache.lastAvailableIndex()+1, bar);
    }

    @Override
    public void addBar(Duration timePeriod, ZonedDateTime endTime) {
        this.addBar(new BaseBar(timePeriod, endTime, function()));
    }

    @Override
    public void addBar(ZonedDateTime endTime, Num openPrice, Num highPrice, Num lowPrice, Num closePrice, Num volume) {
        this.addBar(
                new BaseBar(Duration.ofDays(1), endTime, openPrice, highPrice, lowPrice, closePrice, volume, numOf(0)));
    }

    @Override
    public void addBar(ZonedDateTime endTime, Num openPrice, Num highPrice, Num lowPrice, Num closePrice, Num volume,
            Num amount) {
        this.addBar(
                new BaseBar(Duration.ofDays(1), endTime, openPrice, highPrice, lowPrice, closePrice, volume, amount));
    }

    @Override
    public void addBar(Duration timePeriod, ZonedDateTime endTime, Num openPrice, Num highPrice, Num lowPrice,
            Num closePrice, Num volume) {
        this.addBar(new BaseBar(timePeriod, endTime, openPrice, highPrice, lowPrice, closePrice, volume, numOf(0)));
    }

    @Override
    public void addBar(Duration timePeriod, ZonedDateTime endTime, Num openPrice, Num highPrice, Num lowPrice,
            Num closePrice, Num volume, Num amount) {
        this.addBar(new BaseBar(timePeriod, endTime, openPrice, highPrice, lowPrice, closePrice, volume, amount));
    }

    @Override
    public void addTrade(Number price, Number amount) {
        addTrade(numOf(price), numOf(amount));
    }

    @Override
    public void addTrade(String price, String amount) {
        addTrade(numOf(new BigDecimal(price)), numOf(new BigDecimal(amount)));
    }

    @Override
    public void addTrade(Num tradeVolume, Num tradePrice) {
        getLastBar().addTrade(tradeVolume, tradePrice);
    }

    @Override
    public void addPrice(Num price) {
        getLastBar().addPrice(price);
    }

}
