package ta4jexamples.strategies.intelligenthelper;

import static org.ta4j.core.num.NaN.NaN;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public abstract class MovingPivotPointIndicator extends CachedIndicator<Num> {

    private final String uuid;
    private final int amountConfirmationsNeeded;
    private static final Map<String, Integer> latestSeenIndex = new HashMap<>();

    private MovingPivotPointIndicator oppositPivotIndicator;

    protected MovingPivotPointIndicator(BarSeries series, String uuid, int amountConfirmationsNeeded) {
        super(series);
        this.uuid = uuid;
        this.amountConfirmationsNeeded = amountConfirmationsNeeded;
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
            return this.getValueAt(latestPivotIndex.get(), currentCalculationTick);
        }
        return NaN;
    }

    private Optional<Integer> getLatestPivotIndex(int index, int currentCalculationTick) {
        ConfirmationMap confirmationMap = buildConfirmationMap(currentCalculationTick);
        TreeSet<Integer> reversalPoints = confirmationMap.computeReversalPoints(this::contradictsPivot);
        Integer lastReversalPoint = reversalPoints.floor(index);
        return lastReversalPoint==null ? Optional.empty() : Optional.of(lastReversalPoint);
    }

    private ConfirmationMap buildConfirmationMap(int currentCalculationTick) {
        ConfirmationMap map = new ConfirmationMap();
        int currentCalculationEndIndex = Math.min(getBarSeries().getEndIndex(), currentCalculationTick);
        Num maximaSinceLastOpposite = null;
        for(int i = getBarSeries().getBeginIndex(); i<= currentCalculationEndIndex; i++) {
            if (isConfirmed(i, currentCalculationTick)) {
                if (maximaSinceLastOpposite == null || contradictsPivot(maximaSinceLastOpposite, getPivotIndicator().getValue(i))) {
                    map.addConfirmation(i, getPivotIndicator().getValue(i));
                }
            }
            if (oppositPivotIndicator.isConfirmed(i, currentCalculationTick)) {
                map.addOppositeConfirmation(i);
                maximaSinceLastOpposite = getPivotIndicator().getValue(i);
            } else {
                if (maximaSinceLastOpposite == null || contradictsPivot(maximaSinceLastOpposite, getPivotIndicator().getValue(i))) {
                    maximaSinceLastOpposite = getPivotIndicator().getValue(i);
                }
            }
        }
        return map;
    }

    private boolean isConfirmed(int index, int currentCalculationTick) {
        Num valueToCheck = getPivotIndicator().getValue(index);
        int endIndex = Math.min(getBarSeries().getEndIndex(), currentCalculationTick);
        Num lastConfirmation = getConfirmationIndicator().getValue(index);

        int confirmations = 0;
        for (int inFrameIndex = index + 1; inFrameIndex <= endIndex; inFrameIndex++) {
            Num otherValue = getPivotIndicator().getValue(inFrameIndex);
            if (contradictsPivot(valueToCheck, otherValue)) {
                return false;
            }
            Num confirmationValue = getConfirmationIndicator().getValue(inFrameIndex);
            if (contradictsPivot(confirmationValue, lastConfirmation)) {
                confirmations++;
                if (confirmations>=amountConfirmationsNeeded) {
                    return true;
                }
                lastConfirmation = confirmationValue;
            }
        }
        return false;
    }

    public void setOppositPivotIndicator(MovingPivotPointIndicator oppositPivotIndicator) {
        this.oppositPivotIndicator = oppositPivotIndicator;
    }

    protected abstract Indicator<Num> getConfirmationIndicator();

    protected abstract Indicator<Num> getPivotIndicator();

    protected abstract Num getValueAt(int index, int currentCalculationTick);

    protected abstract boolean contradictsPivot(Num valueToCheck, Num otherValue);
}
