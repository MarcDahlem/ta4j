package ta4jexamples.strategies.intelligenthelper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
            for (String path : pathnames) {
                result.add(JsonBarsSerializer.loadSeries(folder + File.separator + path));
            }
        }
        return result;
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
