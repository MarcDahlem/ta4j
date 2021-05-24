package ta4jexamples.strategies.intelligenthelper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import ta4jexamples.loaders.JsonBarsSerializer;

public class IntelligentJsonSeriesLoader {
    private final Collection<String> paths;

    public IntelligentJsonSeriesLoader(Collection<String> paths) {
        this.paths = paths;
    }


    public Set<BarSeries> loadRecordingsIntoSeries(JsonRecordingTimeInterval interval) {
        Set<BarSeries> result = new HashSet<>();
        for (String folder : paths) {
            File f = new File(folder);

            FilenameFilter filter = getFilenameFilter(interval);

            String[] pathnames = f.list(filter);
            BarSeries lastSeries = null;
            for (String path : pathnames) {
                BarSeries newSeries = JsonBarsSerializer.loadSeries(folder + File.separator + path);
                Optional<Integer> maybeMergeIndex = findMergeIndex(lastSeries, newSeries);
                if (maybeMergeIndex.isPresent()) {
                    lastSeries = mergeSeries(lastSeries, newSeries, maybeMergeIndex.get());
                } else {
                    if(lastSeries != null) {
                        result.add(lastSeries);
                    }
                    lastSeries = newSeries;
                }
            }
            if(Objects.nonNull(lastSeries)) {
                result.add(lastSeries);
            }
        }
        return result;
    }

    private BarSeries mergeSeries(BarSeries lastSeries, BarSeries newSeries, Integer mergeIndex) {
        Bar firstNewBar = newSeries.getBar(mergeIndex);
        lastSeries.addBar(firstNewBar, true);

        for (int index = mergeIndex+1; index<= newSeries.getEndIndex(); index++) {
            lastSeries.addBar(newSeries.getBar(index));
        }
        return lastSeries;
    }

    private Optional<Integer> findMergeIndex(BarSeries lastSeries, BarSeries newSeries) {
        if (Objects.isNull(lastSeries)) {
            return Optional.empty();
        }
        if (!getNamePrefix(lastSeries).equalsIgnoreCase(getNamePrefix(newSeries))) {
            return Optional.empty();
        }

        Bar lastSeriesLastBar = lastSeries.getLastBar();
        for(int index = newSeries.getBeginIndex(); index<= newSeries.getEndIndex(); index++) {
            Bar currentBarInNewSeries = newSeries.getBar(index);
            if (currentBarInNewSeries.getBeginTime().equals(lastSeriesLastBar.getBeginTime())) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    private String getNamePrefix(BarSeries series) {
        String[] split = series.getName().split("_");
        if (split.length != 2) {
            throw new IllegalArgumentException("No valid recorded series. Needs <name>_<timestamp> as series name");
        }
        return split[0];
    }

    private FilenameFilter getFilenameFilter(JsonRecordingTimeInterval interval) {
        return (f1, name) -> {
            String fileName = name.toLowerCase(Locale.ROOT);
            if (fileName.endsWith(".json")) {
                switch (interval) {
                    case All:
                        return true;
                    case OneMinute:
                        return !(fileName.startsWith("5min") || fileName.startsWith("15min"));
                    case FiveMinutes:
                        return fileName.startsWith("5min");
                    case FifteenMinutes:
                        return fileName.startsWith("15min");
                }
            }
            return false;
        };
    }
}
