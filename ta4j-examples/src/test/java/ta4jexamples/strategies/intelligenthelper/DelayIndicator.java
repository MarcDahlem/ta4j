package ta4jexamples.strategies.intelligenthelper;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import static org.ta4j.core.num.NaN.NaN;

public class DelayIndicator extends CachedIndicator<Num> {
    private final int delay;
    private final Indicator<Num> indicator;

    public DelayIndicator(Indicator<Num> indicator, Integer delay) {
        super(indicator);
        this.indicator = indicator;
        this.delay = delay;
    }

    @Override
    protected Num calculate(int index) {
        int delayedIndex = index - delay;
        if (delayedIndex >= getBarSeries().getBeginIndex() && delayedIndex <= getBarSeries().getEndIndex()) {
            return indicator.getValue(delayedIndex);
        }
        return NaN;
    }
}
