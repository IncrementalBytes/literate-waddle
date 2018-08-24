package net.frostedbytes.android.trendfeeder.utils;

import android.util.Log;
import java.util.Locale;
import net.frostedbytes.android.trendfeeder.BuildConfig;

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
