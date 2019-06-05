package cityu.jasontang.comicviewer;

import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import java.util.regex.Pattern;

public class ReaderSettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_guidesettings, rootKey);

        /* Need to migrate to Android X for better native EditTextPreference support */

        //Set OnPreferenceChangeListener to filter input for now
        EditTextPreference threshold_value = (EditTextPreference) this.findPreference("image_threshold_value");
        threshold_value.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    if(withinLimit(newValue.toString(), 3)) {
                        int val = Integer.parseInt(newValue.toString());
                        if ((val >= 80) && (val <= 255)) {
                            preference.setSummary("" + val);
                            return true;
                        }
                    }
                }
                Toast.makeText(getActivity(), "Choose something between 80 and 255", Toast.LENGTH_LONG).show();
                return false;
            }
        });

        EditTextPreference area_threshold = (EditTextPreference) this.findPreference("panel_split_area");
        area_threshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    if(withinLimit(newValue.toString(), 2)) {
                        int val = Integer.parseInt(newValue.toString());
                        if ((val >= 10) && (val <= 80)) {
                            preference.setSummary("" + val);
                            return true;
                        }
                    }
                }
                Toast.makeText(getActivity(), "Choose something between 40 and 80", Toast.LENGTH_LONG).show();
                return false;
            }
        });


        EditTextPreference split_rows = (EditTextPreference) this.findPreference("panel_split_rows");
        split_rows.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    if(withinLimit(newValue.toString(), 1)) {
                        int val = Integer.parseInt(newValue.toString());
                        if ((val >= 1) && (val <= 3)) {
                            preference.setSummary("" + val);
                            return true;
                        }
                    }
                }
                Toast.makeText(getActivity(), "Choose something between 1 and 4", Toast.LENGTH_LONG).show();
                return false;
            }
        });

        EditTextPreference split_cols = (EditTextPreference) this.findPreference("panel_split_cols");
        split_cols.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    if(withinLimit(newValue.toString(), 1)) {
                        int val = Integer.parseInt(newValue.toString());
                        if ((val >= 1) && (val <= 3)) {
                            preference.setSummary("" + val);
                            return true;
                        }
                    }
                }
                Toast.makeText(getActivity(), "Choose something between 1 and 4", Toast.LENGTH_LONG).show();
                return false;
            }
        });
    }

    /* Check if input is number and within a set length */
    public static boolean withinLimit(String s, int limit) {
        if (s.length() <= limit) {
            return (Pattern.matches("[0-9]+", s));
        }
        return false;
    }
}
