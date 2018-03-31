package net.frostedbytes.android.trendfeeder.utils;

import android.util.Log;
import com.google.firebase.crash.FirebaseCrash;
import java.util.Locale;
import net.frostedbytes.android.trendfeeder.BuildConfig;

/**
 * Wrapper class for logging to help remove from non-debug builds.
 */
public class LogUtils {

  public static void debug(final String tag, String message) {

    debug(tag, "%s", message);
  }

  public static void debug(final String tag, String messageFormat, Object... args) {

    if (BuildConfig.DEBUG) {
      Log.d(tag, String.format(Locale.ENGLISH, messageFormat, args));
    }
  }

  public static void error(final String tag, String message) {

    error(tag, "%s", message);
  }

  public static void error(final String tag, String messageFormat, Object... args) {

    if (BuildConfig.DEBUG) {
      Log.e(tag, String.format(Locale.ENGLISH, messageFormat, args));
    } else {
      FirebaseCrash.log(String.format(Locale.ENGLISH, messageFormat, args));
    }
  }

  public static void warn(final String tag, String message) {

    warn(tag, "%s", message);
  }

  public static void warn(final String tag, String messageFormat, Object... args) {

    if (BuildConfig.DEBUG) {
      Log.w(tag, String.format(Locale.ENGLISH, messageFormat, args));
    }
  }
}
