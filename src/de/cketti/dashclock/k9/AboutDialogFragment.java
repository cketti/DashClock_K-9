package de.cketti.dashclock.k9;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
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
        about.setText(Html.fromHtml(getString(R.string.about_text)));
        about.setMovementMethod(LinkMovementMethod.getInstance());

        builder.setView(view);

        return builder.create();
    }
}
