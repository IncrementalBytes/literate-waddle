package net.frostedbytes.android.trendfeeder.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.frostedbytes.android.trendfeeder.BaseActivity;
import net.frostedbytes.android.trendfeeder.R;
import net.frostedbytes.android.trendfeeder.models.MatchSummary;
import net.frostedbytes.android.trendfeeder.models.UserPreference;
import net.frostedbytes.android.trendfeeder.utils.LogUtils;
import net.frostedbytes.android.trendfeeder.utils.PathUtils;

public class MatchDetailsFragment  extends Fragment {

  private static final String TAG = MatchDetailsFragment.class.getSimpleName();

  public interface OnMatchDetailsListener {

    void onMatchUpdated(MatchSummary matchSummary);
    void onMatchUpdateFailed();
  }

  private OnMatchDetailsListener mCallback;

  private EditText mAwayScoreText;
  private EditText mHomeScoreText;
  private CheckBox mIsFinalCheck;
  private TextView mErrorMessageText;

  private MatchSummary mMatchSummary;
  private UserPreference mUserPreference;

  public static MatchDetailsFragment newInstance(UserPreference userPreference, MatchSummary matchSummary) {

    LogUtils.debug(TAG, "++newInstance(UserPreference)");
    MatchDetailsFragment fragment = new MatchDetailsFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_USER_PREFERENCE, userPreference);
    args.putSerializable(BaseActivity.ARG_MATCH_SUMMARY, matchSummary);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_match_details, container, false);

    Bundle arguments = getArguments();
    if (arguments != null) {
      mUserPreference = (UserPreference) arguments.getSerializable(BaseActivity.ARG_USER_PREFERENCE);
      mMatchSummary = (MatchSummary) arguments.getSerializable(BaseActivity.ARG_MATCH_SUMMARY);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }

    TextView awayText = view.findViewById(R.id.details_text_away);
    mAwayScoreText = view.findViewById(R.id.details_edit_away_score);
    TextView homeText = view.findViewById(R.id.details_text_home);
    mHomeScoreText = view.findViewById(R.id.details_edit_home_score);
    mIsFinalCheck = view.findViewById(R.id.details_check_final);
    mErrorMessageText = view.findViewById(R.id.details_text_error);

    awayText.setText(mMatchSummary.AwayTeamName);
    mAwayScoreText.setText(String.valueOf(mMatchSummary.AwayScore));
    homeText.setText(mMatchSummary.HomeTeamName);
    mHomeScoreText.setText(String.valueOf(mMatchSummary.HomeScore));
    mIsFinalCheck.setChecked(mMatchSummary.IsFinal);

    Button updateButton = view.findViewById(R.id.details_button_update);
    updateButton.setOnClickListener(buttonView -> {

      if (mAwayScoreText.getText().toString().isEmpty()) {
        mErrorMessageText.setText(R.string.err_no_away_score);
      } else if (mHomeScoreText.getText().toString().isEmpty()) {
        mErrorMessageText.setText(R.string.err_no_home_score);
      } else {
        MatchSummary updatedSummary = new MatchSummary();
        updatedSummary.AwayTeamName = mMatchSummary.AwayTeamName;
        updatedSummary.AwayScore = Integer.parseInt(mAwayScoreText.getText().toString());
        updatedSummary.HomeTeamName = mMatchSummary.HomeTeamName;
        updatedSummary.HomeScore = Integer.parseInt(mHomeScoreText.getText().toString());
        updatedSummary.IsFinal = mIsFinalCheck.isChecked();
        updatedSummary.MatchDate = mMatchSummary.MatchDate;
        updatedSummary.MatchDay = mMatchSummary.MatchDay;
        updatedSummary.MatchId = mMatchSummary.MatchId;

        if (mUserPreference != null && !mUserPreference.TeamShortName.isEmpty()) {
          String queryPath = PathUtils.combine(MatchSummary.ROOT, mUserPreference.Season, mUserPreference.TeamShortName, updatedSummary.MatchId);
          Map<String, Object> childUpdates = new HashMap<>();
          childUpdates.put(queryPath, updatedSummary.toMap());
          FirebaseDatabase.getInstance().getReference().updateChildren(
            childUpdates,
            (databaseError, databaseReference) -> {

              if (databaseError != null && databaseError.getCode() < 0) {
                LogUtils.error(TAG, "Could not updated match: %s", databaseError.getMessage());
                mCallback.onMatchUpdateFailed();
              } else if (databaseError == null){
                mCallback.onMatchUpdated(updatedSummary);
              } else {
                LogUtils.error(TAG, "Update failed with unexpected error: %d", databaseError.getCode());
                mCallback.onMatchUpdateFailed();
              }
            });
        }
      }
    });

    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    LogUtils.debug(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnMatchDetailsListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.ENGLISH, "%s must implement onPopulated(int) and onSelected(String).", context.toString()));
    }
  }
}
