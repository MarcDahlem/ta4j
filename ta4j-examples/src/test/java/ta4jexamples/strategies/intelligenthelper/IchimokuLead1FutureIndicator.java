package ta4jexamples.strategies.intelligenthelper;

import static ta4jexamples.strategies.intelligenthelper.CombineIndicator.plus;
import static org.ta4j.core.indicators.helpers.TransformIndicator.divide;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.num.Num;

public class IchimokuLead1FutureIndicator extends CachedIndicator<Num> {

    private final TransformIndicator lead1Future;

    public IchimokuLead1FutureIndicator(Indicator<Num> conversionLine, Indicator<Num> baseLine) {
        super(conversionLine);
        lead1Future = divide(plus(conversionLine, baseLine), 2);
    }


    @Override
    protected Num calculate(int i) {
        return lead1Future.getValue(i);
    }
}
