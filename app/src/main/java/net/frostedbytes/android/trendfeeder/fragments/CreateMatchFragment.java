package net.frostedbytes.android.trendfeeder.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.frostedbytes.android.trendfeeder.BaseActivity;
import net.frostedbytes.android.trendfeeder.R;
import net.frostedbytes.android.trendfeeder.models.MatchSummary;
import net.frostedbytes.android.trendfeeder.models.UserPreference;
import net.frostedbytes.android.trendfeeder.utils.LogUtils;
import net.frostedbytes.android.trendfeeder.utils.PathUtils;

public class CreateMatchFragment extends Fragment {

  private static final String TAG = CreateMatchFragment.class.getSimpleName();

  public interface OnCreateMatchListener {

    void onMatchCreated(MatchSummary matchSummary);
  }

  private OnCreateMatchListener mCallback;

  private Spinner mAwaySpinner;
  private Spinner mHomeSpinner;
  private EditText mMonthText;
  private EditText mDayText;
  private EditText mYearText;
  private TextView mErrorMessageText;

  private UserPreference mUserPreference;

  private Query mMatchSummaryQuery;
  private ValueEventListener mValueEventListener;

  public static CreateMatchFragment newInstance(UserPreference userPreference) {

    LogUtils.debug(TAG, "++newInstance(UserPreference)");
    CreateMatchFragment fragment = new CreateMatchFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_USER_PREFERENCE, userPreference);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_create_match, container, false);

    Bundle arguments = getArguments();
    if (arguments != null) {
      mUserPreference = (UserPreference) arguments.getSerializable(BaseActivity.ARG_USER_PREFERENCE);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }

    mAwaySpinner = view.findViewById(R.id.create_spinner_away);
    mDayText = view.findViewById(R.id.create_edit_day);
    mHomeSpinner = view.findViewById(R.id.create_spinner_home);
    mMonthText = view.findViewById(R.id.create_edit_month);
    mYearText = view.findViewById(R.id.create_edit_year);
    mErrorMessageText = view.findViewById(R.id.create_text_error);

    String[] teamItems = getResources().getStringArray(R.array.team_list_entries);
    List<String> teams = Arrays.asList(teamItems);
    Collections.sort(teams);

    // get a list of teams for the object adapter used by the spinner controls
    if (getActivity() != null) {
      ArrayAdapter<String> teamsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<>(teams));
      teamsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mHomeSpinner.setAdapter(teamsAdapter);
      mAwaySpinner.setAdapter(teamsAdapter);
    }

    Button createMatch = view.findViewById(R.id.create_button_create);
    createMatch.setOnClickListener(buttonView -> {

      if (mHomeSpinner.getSelectedItem().toString().isEmpty()) {
        mErrorMessageText.setText(R.string.err_missing_home_selection);
      } else if (mAwaySpinner.getSelectedItem().toString().isEmpty()) {
        mErrorMessageText.setText(R.string.err_missing_away_selection);
      } else if (mHomeSpinner.getSelectedItem().toString().equals(mAwaySpinner.getSelectedItem().toString())) {
        mErrorMessageText.setText(R.string.err_same_selection);
      } else {
        mErrorMessageText.setText("");
        if (mUserPreference != null && !mUserPreference.TeamShortName.isEmpty()) {
          String queryPath = PathUtils.combine(MatchSummary.ROOT, mUserPreference.Season, mUserPreference.TeamShortName);
          LogUtils.debug(TAG, "Query: %s", queryPath);
          mMatchSummaryQuery = FirebaseDatabase.getInstance().getReference().child(queryPath).orderByChild("MatchDay");
          mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

              int year = Integer.parseInt(mYearText.getText().toString());
              int month = Integer.parseInt(mMonthText.getText().toString());
              int day = Integer.parseInt(mDayText.getText().toString());
              int matchDay = 1;
              String matchDate = String.format(Locale.ENGLISH, "%04d%02d%02d", year, month, day);
              String home = mHomeSpinner.getSelectedItem().toString();
              String away = mAwaySpinner.getSelectedItem().toString();
              boolean found = false;
              for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                MatchSummary matchSummary = snapshot.getValue(MatchSummary.class);
                if (matchSummary != null) {
                  if (matchSummary.AwayTeamName.equals(away) &&
                    matchSummary.HomeTeamName.equals(home) &&
                    matchSummary.MatchDate.equals(matchDate)) {

                    LogUtils.debug(TAG, "Match already exists.");
                    mCallback.onMatchCreated(null);
                    found = true;
                    break;
                  } else {
                    matchDay++;
                  }
                }
              }

              if (!found) {
                MatchSummary summary = new MatchSummary();
                summary.MatchDate = matchDate;
                summary.AwayTeamName = away;
                summary.HomeTeamName = home;
                summary.MatchId = UUID.randomUUID().toString();
                summary.MatchDay = matchDay;
                mCallback.onMatchCreated(summary);
              }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
          };
          mMatchSummaryQuery.addValueEventListener(mValueEventListener);
        } else {
          LogUtils.warn(TAG, "User preferences were incomplete.");
        }
      }
    });

    updateUI();

    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    LogUtils.debug(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnCreateMatchListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.ENGLISH, "%s must implement onPopulated(int) and onSelected(String).", context.toString()));
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
    if (mMatchSummaryQuery != null && mValueEventListener != null) {
      mMatchSummaryQuery.removeEventListener(mValueEventListener);
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    LogUtils.debug(TAG, "++onResume()");
    updateUI();
  }

  private void updateUI() {

    if (mUserPreference != null) {
      mYearText.setText(String.format(Locale.ENGLISH, "%1d", mUserPreference.Season));
    }
  }
}
