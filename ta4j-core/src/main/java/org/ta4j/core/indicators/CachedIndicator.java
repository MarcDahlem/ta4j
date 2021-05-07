/**
 * The MIT License (MIT)
 *
 * <p>Copyright (c) 2014-2017 Marc de Verdelhan, 2017-2021 Ta4j Organization & respective authors
 * (see AUTHORS)
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ta4j.core.indicators;

import java.util.*;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;

/**
 * Cached {@link Indicator indicator}.
 *
 * <p>Caches the constructor of the indicator. Avoid to calculate the same index of the indicator
 * twice.
 */
public abstract class CachedIndicator<T> extends AbstractIndicator<T> {

  protected final SortedSet<Integer> cachedIndeces = new TreeSet<>();

  /** List of cached results */
  private final LinkedHashMap<Integer, T> cache =
      new LinkedHashMap<Integer, T>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, T> eldest) {
          boolean shouldRemove = size() > getBarSeries().getMaximumBarCount();
          if (shouldRemove) {
            cachedIndeces.remove(eldest.getKey());
          }
          return shouldRemove;
        }
      };

  /**
   * Constructor.
   *
   * @param series the related bar series
   */
  protected CachedIndicator(BarSeries series) {
    super(series);
  }

  /**
   * Constructor.
   *
   * @param indicator a related indicator (with a bar series)
   */
  protected CachedIndicator(Indicator<?> indicator) {
    this(indicator.getBarSeries());
  }

  /**
   * @param index the bar index
   * @return the value of the indicator
   */
  protected abstract T calculate(int index);

  @Override
  public T getValue(int index) {
    if (cache.containsKey(index)) {
      return cache.get(index);
    }
    final int removedBarsCount = getBarSeries().getRemovedBarsCount();

    if (index < removedBarsCount) {
      return calculate(0);
    }
    T result = calculate(index);
    if (this.getBarSeries() != null && index != this.getBarSeries().getEndIndex()) {
      cache.put(index, result);
      cachedIndeces.add(index);
    }
    return result;
  }
}
