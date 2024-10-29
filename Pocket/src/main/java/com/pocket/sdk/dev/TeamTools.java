package com.pocket.sdk.dev;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.ideashower.readitlater.BuildConfig;
import com.ideashower.readitlater.R;
import com.pocket.app.settings.beta.TCActivity;
import com.pocket.sdk.util.AbsPocketActivity;

/**
 * TODO Documentation
 */
public class TeamTools {

    private final AbsPocketActivity mActivity;

    public TeamTools(AbsPocketActivity activity) {
        mActivity = activity;
    }

    public void show() {
        if (!mActivity.app().mode().isForInternalCompanyOnly()) {
            return;
        }

        final String versionInfo = "Version " + mActivity.app().build().getVersionName() + "\n" +
                            "Build " + BuildConfig.GIT_SHA;

        String message = versionInfo + "\n\n";

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle("Review/Testing Tool")
                .setMessage(message)
                .setNegativeButton(R.string.ac_ok, null)
                .setPositiveButton("Configure", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.startActivity(new Intent(mActivity, TCActivity.class));
                    }
                })
                .setNeutralButton(R.string.ac_share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.startActivity(
                                new Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, versionInfo));
                    }
                });

        builder.show();
    }

}
