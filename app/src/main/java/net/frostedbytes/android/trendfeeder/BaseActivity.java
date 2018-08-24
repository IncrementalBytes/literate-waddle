package net.frostedbytes.android.trendfeeder;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import net.frostedbytes.android.trendfeeder.utils.LogUtils;

public class BaseActivity extends AppCompatActivity {

  public static final String ARG_EMAIL = "email";
  public static final String ARG_EMPTY_MESSAGE = "empty_message";
  public static final String ARG_MATCH_SUMMARIES = "match_summaries";
  public static final String ARG_MATCH_SUMMARY = "match_summary";
  public static final String ARG_TEAMS = "teams";
  public static final String ARG_USER_ID = "user_id";
  public static final String ARG_USER_NAME = "user_name";
  public static final String ARG_USER_PREFERENCE = "user_preference";
  public static final String DEFAULT_DATE = "0000-01-01";
  public static final String DEFAULT_ID = "000000000-0000-0000-0000-000000000000";

  private static final String TAG = BaseActivity.class.getSimpleName();

  private ProgressDialog mProgressDialog;

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);

    LogUtils.debug(TAG, "++onCreate(Bundle)");
  }

  void showProgressDialog(String message) {

    LogUtils.debug(TAG, "++showProgressDialog()");
    if (mProgressDialog == null) {
      mProgressDialog = new ProgressDialog(this);
      mProgressDialog.setCancelable(false);
      mProgressDialog.setMessage(message);
    }

    mProgressDialog.show();
  }

  void hideProgressDialog() {

    LogUtils.debug(TAG, "++hideProgressDialog()");
    if (mProgressDialog != null && mProgressDialog.isShowing()) {
      mProgressDialog.dismiss();
    }
  }
}
