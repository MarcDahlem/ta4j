package ta4jexamples.strategies.intelligenthelper;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class IntelligentTrailIndicator extends CachedIndicator<Num> {
    private final Indicator<Num> aboveBreakEvenIndicator;
    private final Indicator<Num> minAboveBreakEvenIndicator;
    private final Indicator<Num> breakEvenIndicator;
    private final Indicator<Num> belowBreakEvenIndicator;

    public IntelligentTrailIndicator(Indicator<Num> belowBreakEvenIndicator, Indicator<Num> aboveBreakEvenIndicator, Indicator<Num> minAboveBreakEvenIndicator, Indicator<Num> breakEvenIndicator) {
        super(belowBreakEvenIndicator);

        this.belowBreakEvenIndicator = belowBreakEvenIndicator;
        this.aboveBreakEvenIndicator = aboveBreakEvenIndicator;
        this.minAboveBreakEvenIndicator = minAboveBreakEvenIndicator;
        this.breakEvenIndicator = breakEvenIndicator;
    }


    @Override
    protected Num calculate(int i) {
        Num breakEven = breakEvenIndicator.getValue(i);
        if(minAboveBreakEvenIndicator.getValue(i).isGreaterThanOrEqual(breakEven)) {
            return minAboveBreakEvenIndicator.getValue(i).max(aboveBreakEvenIndicator.getValue(i));
        }
        return belowBreakEvenIndicator.getValue(i);
    }
}
