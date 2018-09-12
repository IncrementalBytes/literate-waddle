package net.frostedbytes.android.trendfeeder.fragments;

import static net.frostedbytes.android.trendfeeder.BaseActivity.BASE_TAG;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.frostedbytes.android.trendfeeder.BaseActivity;
import net.frostedbytes.android.trendfeeder.R;
import net.frostedbytes.android.trendfeeder.models.MatchSummary;
import net.frostedbytes.android.trendfeeder.models.Team;
import net.frostedbytes.android.trendfeeder.utils.LogUtils;

public class CreateMatchFragment extends Fragment {

  private static final String TAG = BASE_TAG + CreateMatchFragment.class.getSimpleName();

  public interface OnCreateMatchListener {

    void onMatchCreated(MatchSummary matchSummary);
  }

  private OnCreateMatchListener mCallback;

  private Spinner mAwaySpinner;
  private Spinner mHomeSpinner;
  private TextView mErrorMessageText;

  private int mMatchDay;
  private int mMatchMonth;
  private ArrayList<MatchSummary> mMatchSummaries;
  private int mMatchYear;
  private ArrayList<Team> mTeams;

  public static CreateMatchFragment newInstance(ArrayList<Team> teams, ArrayList<MatchSummary> matchSummaries) {

    LogUtils.debug(TAG, "++newInstance(ArrayList<>, ArrayList<>)");
    CreateMatchFragment fragment = new CreateMatchFragment();
    Bundle args = new Bundle();
    args.putParcelableArrayList(BaseActivity.ARG_TEAMS, teams);
    args.putParcelableArrayList(BaseActivity.ARG_MATCH_SUMMARIES, matchSummaries);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_create_match, container, false);

    mAwaySpinner = view.findViewById(R.id.create_spinner_away);
    mHomeSpinner = view.findViewById(R.id.create_spinner_home);
    mErrorMessageText = view.findViewById(R.id.create_text_error);

    Calendar calendar = Calendar.getInstance(); // set default to current day
    mMatchDay = calendar.get(Calendar.DAY_OF_MONTH);
    mMatchMonth = calendar.get(Calendar.MONTH) + 1; // note: month value is based on 0-11
    mMatchYear = calendar.get(Calendar.YEAR);

    populateSpinners();
    CalendarView calendarView = view.findViewById(R.id.create_calendar);
    calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {

      mMatchDay = dayOfMonth;
      mMatchMonth = month + 1; // note: month value is based on 0-11
      mMatchYear = year;
    });

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
        String matchDate = String.format(Locale.ENGLISH, "%04d%02d%02d", mMatchYear, mMatchMonth, mMatchDay);
        String home = mHomeSpinner.getSelectedItem().toString();
        String away = mAwaySpinner.getSelectedItem().toString();
        boolean found = false;
        for (MatchSummary matchSummary : mMatchSummaries) {
          String awayName = getTeamName(matchSummary.AwayId);
          String homeName = getTeamName(matchSummary.HomeId);
          if (awayName.equals(away) && homeName.equals(home) && matchSummary.MatchDate.equals(matchDate)) {
            LogUtils.debug(TAG, "Match already exists.");
            mCallback.onMatchCreated(null);
            found = true;
            break;
          }
        }

        if (!found) {
          MatchSummary summary = new MatchSummary();
          summary.MatchDate = matchDate;
          summary.AwayId = getTeamId(away);
          summary.HomeId = getTeamId(home);
          summary.MatchId = UUID.randomUUID().toString();
          mCallback.onMatchCreated(summary);
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
      mCallback = (OnCreateMatchListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.ENGLISH, "%s must implement onPopulated(int) and onSelected(String).", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mMatchSummaries = arguments.getParcelableArrayList(BaseActivity.ARG_MATCH_SUMMARIES);
      mTeams = arguments.getParcelableArrayList(BaseActivity.ARG_TEAMS);
    } else {
      LogUtils.error(TAG, "Arguments were null.");
    }
  }

  private String getTeamId(String fullName) {

    if (mTeams == null) {
      LogUtils.warn(TAG, "Team data is empty/null.");
      return BaseActivity.DEFAULT_ID;
    }

    for (Team team : mTeams) {
      if (team.FullName.equals(fullName)) {
        return team.Id;
      }
    }

    LogUtils.warn(TAG, "Did not find Id for %s", fullName);
    return BaseActivity.DEFAULT_ID;
  }

  private String getTeamName(String teamId) {

    if (mTeams == null) {
      LogUtils.warn(TAG, "Team data is empty/null.");
      return "N/A";
    }

    for (Team team : mTeams) {
      if (team.Id.equals(teamId)) {
        return team.FullName;
      }
    }

    LogUtils.warn(TAG, "Did not find name for %s", teamId);
    return "N/A";
  }

  private void populateSpinners() {

    List<String> teams = new ArrayList<>();
    for (Team team : mTeams) {
      teams.add(team.FullName);
    }

    Collections.sort(teams);

    // get a list of teams for the object adapter used by the spinner controls
    if (getActivity() != null) {
      ArrayAdapter<String> teamsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<>(teams));
      teamsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mHomeSpinner.setAdapter(teamsAdapter);
      mAwaySpinner.setAdapter(teamsAdapter);
      if (mAwaySpinner.getSelectedItemPosition() == mHomeSpinner.getSelectedItemPosition()) {
        mAwaySpinner.setSelection(mHomeSpinner.getSelectedItemPosition() + 1);
      }
    }
  }
}
