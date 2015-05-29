package me.krrr.androidhelper;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;


public class ListPreferenceCompat extends ListPreference {
    private String originSummary;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ListPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        originSummary = getSummary().toString();
    }

    public ListPreferenceCompat(Context context) {
        super(context);
        originSummary = getSummary().toString();
    }

    @Override
    public void setValue(String value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            super.setValue(value);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // source: http://stackoverflow.com/questions/10119852
            // The framework forgot to call notifyChanged() in setValue() on previous versions of android.
            // This bug has been fixed in android-4.4_r0.7.
            String oldValue = getValue();
            super.setValue(value);
            if (!TextUtils.equals(value, oldValue)) {
                notifyChanged();
            }
        } else {
            // lower API version doesn't support formatting in summary
            // only confirmed on v2.3.4
            super.setValue(value);
            setSummary(String.format(originSummary, getEntry()));
        }
    }
}