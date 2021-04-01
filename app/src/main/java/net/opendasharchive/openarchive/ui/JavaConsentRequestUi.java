package net.opendasharchive.openarchive.ui;

import android.app.Activity;
import android.app.AlertDialog;

import net.opendasharchive.openarchive.R;

import org.cleaninsights.sdk.Campaign;
import org.cleaninsights.sdk.ConsentRequestUi;
import org.cleaninsights.sdk.ConsentRequestUiCompletionHandler;
import org.cleaninsights.sdk.Feature;
import org.cleaninsights.sdk.Period;
import org.jetbrains.annotations.NotNull;

public class JavaConsentRequestUi implements org.cleaninsights.sdk.JavaConsentRequestUi {

    private final Activity activity;

    public JavaConsentRequestUi(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void show(@NotNull String campaignId, @NotNull Campaign campaign, @NotNull ConsentRequestUiCompletionHandler handler) {
        Period period = campaign.getNextTotalMeasurementPeriod();

        if (period == null) return;

        String msg = activity.getString(R.string._measurement_consent_explanation_,
                (period.getStartDate().toLocaleString()),
                (period.getEndDate()).toLocaleString());

        new AlertDialog.Builder(activity)
                .setTitle(R.string.Your_Consent)
                .setMessage(msg)
                .setNegativeButton(R.string.No__sorry_, (dialog, which) -> handler.completed(false))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> handler.completed(true))
                .create()
                .show();
    }

    @Override
    public void show(@NotNull Feature feature, @NotNull ConsentRequestUiCompletionHandler handler) {
        String msg = activity.getString(R.string._feature_consent_explanation_, localize(feature, activity));

        new AlertDialog.Builder(activity)
                .setTitle(R.string.Your_Consent)
                .setMessage(msg)
                .setNegativeButton(R.string.No__sorry_, (dialog, which) -> handler.completed(false))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> handler.completed(true))
                .create()
                .show();
    }

    private String localize(Feature feature, Activity activity) {
        if (feature == Feature.Lang) {
            return activity.getString(R.string.Your_locale);
        }

        return activity.getString(R.string.Your_device_type);
    }
}

