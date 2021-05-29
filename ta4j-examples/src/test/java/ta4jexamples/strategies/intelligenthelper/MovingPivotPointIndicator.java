package ta4jexamples.strategies.intelligenthelper;

import static org.ta4j.core.num.NaN.NaN;

import java.util.Optional;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public abstract class MovingPivotPointIndicator extends CachedIndicator<Num> {

    private final int frameSize;

    protected MovingPivotPointIndicator(BarSeries series, int frameSize) {
        super(series);
        this.frameSize = frameSize;
    }

    @Override
    public Num getValue(int index) {
        if(index-frameSize >= getBarSeries().getBeginIndex() && index+frameSize < getBarSeries().getEndIndex()) {
            return super.getValue(index);
        }
        return calculate(index);
    }

    @Override
    protected Num calculate(int index) {
        Optional<Integer> latestPivotIndex = getLatestPivotIndex(index);
        if (latestPivotIndex.isPresent()) {
            return this.getValueIndicator().getValue(latestPivotIndex.get());
        }
        return NaN;
    }

    private Optional<Integer> getLatestPivotIndex(int index) {
        while (index >= getBarSeries().getBeginIndex()) {
            if (isPivotIndex(index)) {
                return Optional.of(index);
            }
            index--;
        }
        return Optional.empty();
    }

    private boolean isPivotIndex(int index) {
        Num valueToCheck = getPivotIndicator().getValue(index);
        int startIndex = Math.max(index - frameSize, getBarSeries().getBeginIndex());
        int endIndex = Math.min(index + frameSize, getBarSeries().getEndIndex());

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
