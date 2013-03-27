package de.cketti.dashclock.k9;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;


public class K9Extension extends DashClockExtension {

    @Override
    protected void onUpdateData(int reason) {
        ExtensionData data = new ExtensionData()
                .visible(true)
                .status("42")
                .expandedTitle("Title here")
                .expandedBody("More text here");

        publishUpdate(data);
    }

}
