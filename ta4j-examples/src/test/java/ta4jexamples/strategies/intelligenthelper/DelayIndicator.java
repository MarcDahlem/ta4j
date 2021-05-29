package ta4jexamples.strategies.intelligenthelper;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

import static org.ta4j.core.num.NaN.NaN;

public class DelayIndicator extends AbstractIndicator<Num> {
    private final int delay;
    private final Indicator<Num> indicator;

    public DelayIndicator(Indicator<Num> indicator, Integer delay) {
        super(indicator.getBarSeries());
        this.indicator = indicator;
        this.delay = delay;
    }

    @Override
    public Num getValue(int index) {
        int delayedIndex = index - delay;
        if (delayedIndex >= getBarSeries().getBeginIndex() && delayedIndex <= getBarSeries().getEndIndex()) {
            return indicator.getValue(delayedIndex);
        }
        return NaN;
    }
}
