package com.pocket.sdk.help;

import com.pocket.util.java.StringBuilders;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Allows components of the app to be included in bug logs attached to support emails.
 * Register a component with {@link #registerTroublemaker(com.pocket.sdk.help.Troubleshooter.Troublemaker)}
 * and if a user files a report, your component will be queried for data to be included in the report.
 */
@Singleton
public class Troubleshooter {

    private final Set<Troublemaker> mRecords = new HashSet<>();

    @Inject
    public Troubleshooter() {}

    /**
     * Register to be included in bug reports that a user may file in the future during this app instance.
     *
     * @param maker
     */
    public void registerTroublemaker(Troublemaker maker) {
        mRecords.add(maker);
    }

    /**
     * @return A report including all {@link com.pocket.sdk.help.Troubleshooter.Troublemaker} reports in the app.
     */
    public String getFormattedReport() {
        StringBuilder builder = StringBuilders.get();

        for (Troublemaker report : mRecords) {
            if (!report.hasTroublemakerReport()) {
                continue;
            }
            builder.append(report.getTroublemakerName());
            builder.append("\n");
            builder.append(report.getTroublemakerReport());
            builder.append("\n\n");
        }

        String result = builder.toString();
        if (result.length() == 0) {
            result = null;
        }
        StringBuilders.recycle(builder);
        return result;
    }

    /**
     * Something that can file a bug report.
     */
    public interface Troublemaker {
        /**
         * @return The name to be displayed in the report. Note: This is user facing.
         */
        public String getTroublemakerName();

        /**
         * @return true if there is something to include in the report.
         */
        public boolean hasTroublemakerReport();

        /**
         * @return The content you want to include in the report. Note: This is user facing.
         */
        public String getTroublemakerReport();
    }


}
