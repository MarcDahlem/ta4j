package ta4jexamples.strategies.intelligenthelper;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.TradeBasedIndicator;

public class TrueInBuyPhaseIndicator extends TradeBasedIndicator<Boolean> {
    public TrueInBuyPhaseIndicator(BarSeries series, TradeBasedIndicator<?> tradeKnowingIndicator) {
        super(series, tradeKnowingIndicator);
    }

    @Override
    protected Boolean calculateNoLastTradeAvailable(int index) {
        return false;
    }

    @Override
    protected Boolean calculateLastTradeWasBuy(int buyIndex, int index) {
        return true;
    }

    @Override
    protected Boolean calculateLastTradeWasSell(int sellIndex, int index) {
        return false;
    }
}
