package ta4jexamples.strategies.intelligenthelper;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.Num;

public class HighestPivotPointIndicator extends MovingPivotPointIndicator {
    private final HighPriceIndicator highPriceIndicator;
    private final Indicator<Num> valueIndicator;
    private final LowPriceIndicator lowPriceIndicator;

    public HighestPivotPointIndicator(BarSeries series, String uuid, int amountConfirmationsNeeded) {
        this(series, null, uuid, amountConfirmationsNeeded);
    }

    public HighestPivotPointIndicator(BarSeries series, Indicator<Num> valueIndicator, String uuid, int amountConfirmationsNeeded) {
        super(series, uuid, amountConfirmationsNeeded);
        this.highPriceIndicator = new HighPriceIndicator(series);
        this.lowPriceIndicator = new LowPriceIndicator(series);
        this.valueIndicator = valueIndicator == null ? highPriceIndicator : valueIndicator;
    }

    @Override
    protected Num getValueAt(int index, int currentCalculationTick) {
        /*if (valueIndicator instanceof RSIIndicator) {
            int startIndex = Math.max(index - frameSize, getBarSeries().getBeginIndex());
            int endIndex = Math.min(index + frameSize, currentCalculationTick);
            int futureDelay = (endIndex - index) * -1;
            int restrictedFrameSize = endIndex - startIndex;
            HighestValueIndicator indicator = new HighestValueIndicator(new DelayIndicator(valueIndicator, futureDelay), restrictedFrameSize + 1);
            return indicator.getValue(index);
        }*/
        return valueIndicator.getValue(index);
    }

    @Override
    protected Indicator<Num> getPivotIndicator() {
        return highPriceIndicator;
    }

    @Override
    protected boolean contradictsPivot(Num valueToCheck, Num otherValue) {
        return valueToCheck.isLessThan(otherValue);
    }

    @Override
    protected Indicator<Num> getConfirmationIndicator() {
        return lowPriceIndicator;
    }
}
