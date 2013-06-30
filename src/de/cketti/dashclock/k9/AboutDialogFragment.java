/*
 * Copyright 2013 Christian Ketterer (cketti)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cketti.dashclock.k9;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


/**
 * Show an "About" dialog
 */
public class AboutDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.label_about);
        builder.setPositiveButton(android.R.string.ok, null);

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.about_dialog, null);

        TextView about = (TextView) view.findViewById(R.id.about_text);
        about.setText(Html.fromHtml(getString(R.string.about_text, getVersionName())));
        about.setMovementMethod(LinkMovementMethod.getInstance());

        builder.setView(view);

        return builder.create();
    }

    private String getVersionName() {
        String version = "?";
        try {
            Activity context = getActivity();
            String packageName = context.getPackageName();
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing
        }
        return version;
    }
}
