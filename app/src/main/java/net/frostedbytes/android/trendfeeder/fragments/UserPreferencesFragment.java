package net.frostedbytes.android.trendfeeder.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import java.util.Locale;
import net.frostedbytes.android.trendfeeder.R;
import net.frostedbytes.android.trendfeeder.utils.LogUtils;

public class UserPreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = UserPreferencesFragment.class.getSimpleName();

  public static final String KEY_TEAM_PREFERENCE = "preference_list_team";
  public static final String KEY_SEASON_PREFERENCE = "preference_list_season";

  public interface OnPreferencesListener {

    void onPreferenceChanged();
  }

  private OnPreferencesListener mCallback;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    LogUtils.debug(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnPreferencesListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.ENGLISH, "%s must implement onPreferenceChanged().", context.toString()));
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    LogUtils.debug(TAG, "++onCreatePreferences(Bundle, String)");
    addPreferencesFromResource(R.xml.app_preferences);
  }

  @Override
  public void onPause() {
    super.onPause();

    LogUtils.debug(TAG, "++onPause()");
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();

    LogUtils.debug(TAG, "++onResume()");
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyName) {

    LogUtils.debug(TAG, "++onSharedPreferenceChanged(SharedPreferences, String)");
    getPreferenceScreen().getSharedPreferences().edit().apply();
    if (keyName.equals(KEY_TEAM_PREFERENCE) || keyName.equals(KEY_SEASON_PREFERENCE)) {
      mCallback.onPreferenceChanged();
    } else {
      LogUtils.error(TAG, "Unknown key: ", keyName);
    }
  }
}
