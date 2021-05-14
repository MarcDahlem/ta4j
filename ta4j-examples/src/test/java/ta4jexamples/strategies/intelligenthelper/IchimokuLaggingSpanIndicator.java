package ta4jexamples.strategies.intelligenthelper;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class IchimokuLaggingSpanIndicator extends CachedIndicator<Num> {


    private final Indicator<Num> currentPriceIndicator;

    public IchimokuLaggingSpanIndicator(Indicator<Num> currentPriceIndicator) {
        super(currentPriceIndicator);
        this.currentPriceIndicator = currentPriceIndicator;
    }


    @Override
    protected Num calculate(int i) {
        return currentPriceIndicator.getValue(i);
    }
}
