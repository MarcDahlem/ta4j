package ta4jexamples.strategies.intelligenthelper;

import static org.ta4j.core.num.NaN.NaN;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public abstract class MovingPivotPointIndicator extends CachedIndicator<Num> {

    private final int frameSize;
    private final String uuid;
    private static final Map<String, Integer> latestSeenIndex = new HashMap<>();

    protected MovingPivotPointIndicator(BarSeries series, int frameSize, String uuid) {
        super(series);
        this.frameSize = frameSize;
        this.uuid = uuid;
    }

    @Override
    public Num getValue(int index) {
        updateLatestSeenIndex(index);
        return calculate(index, latestSeenIndex.get(uuid));
    }

    private void updateLatestSeenIndex(int index) {
        if (latestSeenIndex.containsKey(uuid)) {
            Integer storedIndex = latestSeenIndex.get(uuid);
            latestSeenIndex.put(uuid, Math.max(storedIndex, index));
        } else {
            latestSeenIndex.put(uuid, index);
        }

    }

    @Override
    protected Num calculate(int index) {
        throw new IllegalArgumentException("Internal calculation for fixed entry index needed");
    }

    protected Num calculate(int index, int currentCalculationTick) {
        Optional<Integer> latestPivotIndex = getLatestPivotIndex(index, currentCalculationTick);
        if (latestPivotIndex.isPresent()) {
            return this.getValueIndicator().getValue(latestPivotIndex.get());
        }
        return NaN;
    }

    private Optional<Integer> getLatestPivotIndex(int index, int currentCalculationTick) {
        while (index >= getBarSeries().getBeginIndex()) {
            if (isPivotIndex(index, currentCalculationTick)) {
                return Optional.of(index);
            }
            index--;
        }
        return Optional.empty();
    }

    private boolean isPivotIndex(int index, int currentCalculationTick) {
        Num valueToCheck = getPivotIndicator().getValue(index);
        int startIndex = Math.max(index - frameSize, getBarSeries().getBeginIndex());
        int currentCalculationEndIndex = Math.min(getBarSeries().getEndIndex(), currentCalculationTick);
        int endIndex = index + frameSize;
        if (endIndex > currentCalculationEndIndex) {
            //return false;
            endIndex = currentCalculationEndIndex;
        }

        for (int inFrameIndex = startIndex; inFrameIndex <= endIndex; inFrameIndex++) {
            if (index != inFrameIndex) {
                Num otherValue = getPivotIndicator().getValue(inFrameIndex);
                if (contradictsPivot(valueToCheck, otherValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected abstract Indicator<Num> getPivotIndicator();

    protected abstract Indicator<Num> getValueIndicator();

    protected abstract boolean contradictsPivot(Num valueToCheck, Num otherValue);
}
