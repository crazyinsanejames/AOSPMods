package sh.siava.AOSPMods;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "settingsActivityTitle";

    //Gets string for running shell commands, using stericson RootTools lib
    private void runCommandAction(String command) {
        Command c = new Command(0, command);
        try {
            RootTools.getShell(true).add(c);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }
    }

    public void RestartSysUI(View view) {
        runCommandAction("killall com.android.systemui");

    }

    public void backButtonEnabled(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void backButtonDisabled(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backButtonDisabled();

        try {
            getApplicationContext().getSharedPreferences("sh.siava.AOSPMods_preferences", Context.MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {
            Log.d("SIAPOSED", "onCreate: failed to world read");
            // The new XSharedPreferences is not enabled or module's not loading
            // other fallback, if any
        }

        //update settings from previous config file
        try {
            if(PreferenceManager.getDefaultSharedPreferences(this).contains("Show4GIcon"))
            {
                boolean fourGEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("Show4GIcon", false);
                if(fourGEnabled)
                {
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("LTE4GIconMod", 2).apply();
                }
                PreferenceManager.getDefaultSharedPreferences(this).edit().remove("Show4GIcon").commit();
            }
        }
        catch(Exception e){}


        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }

        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                            backButtonDisabled();
                        }
                    }
                });
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (getTitle() == getString(R.string.title_activity_settings)){
        backButtonDisabled();
        } else {
            backButtonEnabled();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        backButtonEnabled();
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public static class NavFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.nav_prefs, rootKey);
        }
    }

    public static class LockScreenFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.lock_screen_prefs, rootKey);
        }
    }

    public static class MiscFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.misc_prefs, rootKey);
        }
    }

    public static class SBCFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.statusbar_clock_prefs, rootKey);
        }
    }

    public static class ThreeButtonNavFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.three_button_prefs, rootKey);
        }
    }

    public static class StatusbarFragment extends PreferenceFragmentCompat {

        Preference QSPulldownPercent;
        private Context mContext;

        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("QSPulldownPercent")) {
                    updateQSPulldownPercent();
                } else if (key.equals("QSPullodwnEnabled")) {
                    updateQSPulldownEnabld();
                } else if (key.equals("QSFooterMod")) {
                    updateQSFooterMod();
                } else if (key.equals("BatteryStyle")) {
                    updateBatteryMod();
                }
            }

        };

        private void updateBatteryMod() {
            boolean enabled = !(PreferenceManager.getDefaultSharedPreferences(mContext).getString("BatteryStyle", "0").equals(("0")));
            findPreference("BatteryShowPercent").setEnabled(enabled);
        }

        private void updateQSFooterMod() {
            boolean enabled = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("QSFooterMod", false);

            findPreference("QSFooterText").setEnabled(enabled);
        }

        private void updateQSPulldownEnabld() {
            boolean enabled = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("QSPullodwnEnabled", false);

            findPreference("QSPulldownPercent").setEnabled(enabled);
            findPreference("QSPulldownSide").setEnabled(enabled);
        }

        private void updateQSPulldownPercent() {
            int value = PreferenceManager.getDefaultSharedPreferences(mContext).getInt("QSPulldownPercent", 25);
            QSPulldownPercent.setSummary(value + "%");
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            mContext = getContext();
            setPreferencesFromResource(R.xml.statusbar_settings, rootKey);

            QSPulldownPercent = findPreference("QSPulldownPercent");

            PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(listener);

            updateQSPulldownPercent();
            updateQSPulldownEnabld();
            updateQSFooterMod();
            updateBatteryMod();
        }
    }

    public static class GestureNavFragment extends PreferenceFragmentCompat {

        private Context mContext;

        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateBackGesture();
                updateNavPill();
            }

        };

        private void updateNavPill() {
            findPreference("GesPillWidthModPos").setEnabled(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("GesPillWidthMod", true));
            findPreference("GesPillWidthModPos").setSummary(PreferenceManager.getDefaultSharedPreferences(mContext).getInt("GesPillWidthModPos", 50)*2 + getString(R.string.pill_width_summary));
        }

        private void updateBackGesture() {
            findPreference("BackLeftHeight").setEnabled(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("BackFromLeft", true));
            findPreference("BackRightHeight").setEnabled(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("BackFromRight", true));

            findPreference("BackLeftHeight").setSummary(PreferenceManager.getDefaultSharedPreferences(mContext).getInt("BackLeftHeight", 100) + "%");
            findPreference("BackRightHeight").setSummary(PreferenceManager.getDefaultSharedPreferences(mContext).getInt("BackRightHeight", 100) + "%");
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            mContext =getContext();
            setPreferencesFromResource(R.xml.gesture_nav_perfs, rootKey);

            PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(listener);

            updateBackGesture();
            updateNavPill();
        }

    }
}