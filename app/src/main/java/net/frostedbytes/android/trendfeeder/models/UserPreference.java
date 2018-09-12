package net.frostedbytes.android.trendfeeder.models;

import static net.frostedbytes.android.trendfeeder.BaseActivity.BASE_TAG;

import com.google.firebase.database.Exclude;
import java.io.Serializable;
import java.util.Calendar;
import net.frostedbytes.android.trendfeeder.BaseActivity;
import net.frostedbytes.android.trendfeeder.utils.LogUtils;

public class UserPreference implements Serializable {

  private static final String TAG = BASE_TAG + UserPreference.class.getSimpleName();

  /**
   * Year to compare Year results against.
   */
  public int Compare;

  /**
   * Unique identifier for team.
   */
  public String TeamId;

  /**
   * Unique identifier of user.
   */
  @Exclude
  public String UserId;

  /**
   * Year of results.
   */
  public int Season;

  /**
   * Constructs a new UserPreference object with default values.
   */
  @SuppressWarnings("unused")
  public UserPreference() {

    // Default constructor required for calls to DataSnapshot.getValue(Settings.class)
    this.Compare = 0;
    this.TeamId = BaseActivity.DEFAULT_ID;
    this.UserId = BaseActivity.DEFAULT_ID;
    this.Season = Calendar.getInstance().get(Calendar.YEAR);
  }

  /**
   * Compares this UserPreference with another UserPreference.
   * @param compareTo UserPreference to compare this UserPreference against
   * @return TRUE if this UserPreference equals the other UserPreference, otherwise FALSE
   * @throws ClassCastException if object parameter cannot be cast into UserPreference object
   */
  @Override
  public boolean equals(Object compareTo)  throws ClassCastException {

    if (compareTo == null) {
      return false;
    }

    if (this == compareTo) {
      return true;
    }

    //cast to native object is now safe
    if ((compareTo instanceof UserPreference)) {
      try {
        UserPreference compareToSettings = (UserPreference) compareTo;
        if (this.UserId.equals(compareToSettings.UserId) &&
          this.Season == compareToSettings.Season &&
          this.TeamId.equals(compareToSettings.TeamId) &&
          this.Compare == ((UserPreference) compareTo).Compare) {
          return true;
        }
      } catch (ClassCastException cce) {
        LogUtils.error(TAG, "Could not cast object to UserSetting class: %s", cce.getMessage());
      }
    }

    return false;
  }
}
