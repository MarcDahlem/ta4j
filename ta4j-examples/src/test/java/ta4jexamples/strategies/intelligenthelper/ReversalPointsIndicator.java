package ta4jexamples.strategies.intelligenthelper;

import static org.ta4j.core.num.NaN.NaN;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.Num;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ReversalPointsIndicator extends AbstractIndicator<Num> {

    private final String uuid;

    public enum ReversalType {
        LOWS, HIGHS;
    }

    private final HighPriceIndicator highPriceIndicator;
    private final LowPriceIndicator lowPriceIndicator;
    private final int amountConfirmationsNeeded;

    private final ReversalType type;
    private final Indicator<Num> valueIndicator;
    private final ConcurrentSkipListSet<Integer> lows = new ConcurrentSkipListSet<>();
    private final ConcurrentSkipListSet<Integer> highs = new ConcurrentSkipListSet<>();
    private static final Map<String, Integer> latestSeenIndex = new HashMap<>();
    private static final Map<String, Cache<Bar, ReversalComputationState>> strategyCaches = new HashMap<>();

    public ReversalPointsIndicator(BarSeries series, ReversalType type, String uuid,  int amountConfirmationsNeeded) {
        this(series, type, null, uuid,  amountConfirmationsNeeded);
    }

    public ReversalPointsIndicator(BarSeries series, ReversalType type, Indicator<Num> valueIndicator, String uuid,  int amountConfirmationsNeeded) {
        super(series);
        this.type = type;
        this.valueIndicator = valueIndicator;
        this.highPriceIndicator = new HighPriceIndicator(series);
        this.lowPriceIndicator = new LowPriceIndicator(series);
        this.amountConfirmationsNeeded = amountConfirmationsNeeded;
        if (!strategyCaches.containsKey(uuid)) {
            strategyCaches.put(uuid, CacheBuilder.newBuilder().build());
        }
        this.uuid = uuid;
    }

    @Override
    public Num getValue(int index) {
        updateLatestSeenIndex(index);
        Integer currentCalculationTick = latestSeenIndex.get(uuid);
        updateReversals(currentCalculationTick);
        switch (type) {
            case LOWS:
                Integer lastLowIndex = lows.floor(index);
                if (lastLowIndex == null) {
                    return NaN;
                }
                return this.valueIndicator == null ? this.lowPriceIndicator.getValue(lastLowIndex) : this.valueIndicator.getValue(lastLowIndex);
            case HIGHS:
                Integer lastHighIndex = highs.floor(index);
                if (lastHighIndex == null) {
                    return NaN;
                }
                return this.valueIndicator == null ? this.highPriceIndicator.getValue(lastHighIndex) : this.valueIndicator.getValue(lastHighIndex);
            default:
                throw new IllegalStateException("Unknown type encountered " + type);
        }
    }

    private void updateLatestSeenIndex(int index) {
        if (latestSeenIndex.containsKey(uuid)) {
            Integer storedIndex = latestSeenIndex.get(uuid);
            latestSeenIndex.put(uuid, Math.max(storedIndex, index));
        } else {
            latestSeenIndex.put(uuid, index);
        }
    }

    private void updateReversals(Integer currentCalculationTick) {
        lows.clear();
        highs.clear();

        Bar currentBar = getBarSeries().getBar(currentCalculationTick);
        ReversalComputationState state = null;
        Cache<Bar, ReversalComputationState> savedStates = strategyCaches.get(uuid);
        try {
            state = savedStates.get(currentBar, () -> {
                        ReversalComputationState result = new ReversalComputationState(highPriceIndicator, lowPriceIndicator, amountConfirmationsNeeded);
                        for (int index = getBarSeries().getBeginIndex(); index <= currentCalculationTick; index++) {
                            result.update(index);
                        }
                        return result;
                    }
            );
        } catch (ExecutionException e) {
            throw new IllegalStateException("Somehint stupid happened: ", e);
        }
        savedStates.put(currentBar, state);

        lows.addAll(state.getLows());
        highs.addAll(state.getHighs());
    }
}
