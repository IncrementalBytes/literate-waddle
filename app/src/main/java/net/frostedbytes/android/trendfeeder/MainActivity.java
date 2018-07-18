package net.frostedbytes.android.trendfeeder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.frostedbytes.android.trendfeeder.fragments.CreateMatchFragment;
import net.frostedbytes.android.trendfeeder.fragments.MainActivityFragment;
import net.frostedbytes.android.trendfeeder.fragments.MatchDetailsFragment;
import net.frostedbytes.android.trendfeeder.fragments.UserPreferencesFragment;
import net.frostedbytes.android.trendfeeder.models.MatchSummary;
import net.frostedbytes.android.trendfeeder.models.Trend;
import net.frostedbytes.android.trendfeeder.models.UserPreference;
import net.frostedbytes.android.trendfeeder.utils.LogUtils;
import net.frostedbytes.android.trendfeeder.utils.PathUtils;

public class MainActivity extends BaseActivity implements
  MainActivityFragment.OnMatchListListener,
  CreateMatchFragment.OnCreateMatchListener,
  MatchDetailsFragment.OnMatchDetailsListener,
  UserPreferencesFragment.OnPreferencesListener {

  private static final String TAG = MainActivity.class.getSimpleName();

  private UserPreference mUserPreference;

  private List<MatchSummary> mMatchSummaries;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.main_toolbar);
    setSupportActionBar(toolbar);

    mUserPreference = new UserPreference();
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    if (sharedPreferences.contains(UserPreferencesFragment.KEY_TEAM_PREFERENCE)) {
      String preference = sharedPreferences.getString(UserPreferencesFragment.KEY_TEAM_PREFERENCE, getString(R.string.none));
      if (preference.equals(getString(R.string.none))) {
        mUserPreference.TeamFullName = "";
        mUserPreference.TeamShortName = "";
      } else {
        mUserPreference.TeamFullName = preference.substring(0, preference.indexOf(','));
        mUserPreference.TeamShortName = preference.substring(preference.indexOf(',') + 1, preference.length());
      }
    }

    if (sharedPreferences.contains(UserPreferencesFragment.KEY_SEASON_PREFERENCE)) {
      String preference = sharedPreferences.getString(UserPreferencesFragment.KEY_SEASON_PREFERENCE, getString(R.string.none));
      if (preference.equals(getString(R.string.none))) {
        mUserPreference.Season = 0;
      } else {
        mUserPreference.Season = Integer.parseInt(preference);
      }
    }

    replaceFragment(MainActivityFragment.newInstance(mUserPreference));
  }

  @Override
  public void onCreateMatch() {

    LogUtils.debug(TAG, "++onCreateMatch()");
    setTitle("New Match");
    replaceFragment(CreateMatchFragment.newInstance(mUserPreference));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public void onMatchCreated(MatchSummary matchSummary) {

    LogUtils.debug(TAG, "++onMatchCreated(MatchSummary)");
    if (matchSummary != null) {
      String queryPath = PathUtils.combine(MatchSummary.ROOT, mUserPreference.Season, mUserPreference.TeamShortName);
      FirebaseDatabase.getInstance().getReference().child(queryPath).child(matchSummary.MatchId).setValue(
        matchSummary.toMap(),
        (databaseError, databaseReference) -> {

          if (databaseError != null && databaseError.getCode() < 0) {
            LogUtils.error(TAG, "Could not create match: %s", databaseError.getMessage());
            Snackbar.make(findViewById(R.id.fragment_container), getString(R.string.err_match_not_created), Snackbar.LENGTH_LONG).show();
          }
        });
    }

    replaceFragment(MainActivityFragment.newInstance(mUserPreference));
  }

  @Override
  public void onMatchUpdated(MatchSummary matchSummary) {

    LogUtils.debug(TAG, "++onMatchUpdated()");
    replaceFragment(MainActivityFragment.newInstance(mUserPreference));
    if (matchSummary.IsFinal) {
      showProgressDialog(getString(R.string.generating_trends));

      // generate trend data for this match day
      if (mUserPreference != null && mUserPreference.Season > 0 && !mUserPreference.TeamShortName.isEmpty()) {
        String queryPath = PathUtils.combine(MatchSummary.ROOT, mUserPreference.Season, mUserPreference.TeamShortName);
        LogUtils.debug(TAG, "Query: %s", queryPath);
        Query matchSummaryQuery = FirebaseDatabase.getInstance().getReference().child(queryPath).orderByChild("MatchDay");
        ValueEventListener valueEventListener = new ValueEventListener() {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {

            mMatchSummaries = new ArrayList<>();
            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
              MatchSummary summary = snapshot.getValue(MatchSummary.class);
              if (summary != null) {
                summary.MatchId = snapshot.getKey();
                mMatchSummaries.add(summary);
              }
            }

            // keep list in ascending order by date
            generateTrends();
          }

          @Override
          public void onCancelled(@NonNull DatabaseError databaseError) {

            LogUtils.debug(TAG, "++onCancelled(DatabaseError)");
          }
        };
        matchSummaryQuery.addValueEventListener(valueEventListener);
      } else {
        LogUtils.warn(TAG, "User preferences were incomplete.");
        LogUtils.debug(TAG, "%s (%s): %d Season", mUserPreference.TeamFullName, mUserPreference.TeamShortName, mUserPreference.Season);
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
      case R.id.action_settings:
        replaceFragment(new UserPreferencesFragment());
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onPopulated(int size) {

    LogUtils.debug(TAG, "++onPopulated(%1d)", size);
    hideProgressDialog();
    if (mUserPreference != null && !mUserPreference.TeamShortName.isEmpty()) {
      setTitle(getResources().getQuantityString(R.plurals.subtitle, size, mUserPreference.TeamShortName, size));
    } else {
      setTitle("Match Summaries");
      Snackbar.make(findViewById(R.id.fragment_container), getString(R.string.no_matches), Snackbar.LENGTH_LONG).show();
    }
  }

  @Override
  public void onPreferenceChanged() {

    LogUtils.debug(TAG, "++onPreferenceChanged()");
    if (mUserPreference != null) {
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
      if (sharedPreferences.contains(UserPreferencesFragment.KEY_TEAM_PREFERENCE)) {
        String preference = sharedPreferences.getString(UserPreferencesFragment.KEY_TEAM_PREFERENCE, getString(R.string.none));
        if (preference.equals(getString(R.string.none))) {
          mUserPreference.TeamFullName = "";
          mUserPreference.TeamShortName = "";
        } else {
          mUserPreference.TeamFullName = preference.substring(0, preference.indexOf(','));
          mUserPreference.TeamShortName = preference.substring(preference.indexOf(',') + 1, preference.length());
        }
      }

      if (sharedPreferences.contains(UserPreferencesFragment.KEY_SEASON_PREFERENCE)) {
        String preference = sharedPreferences.getString(UserPreferencesFragment.KEY_SEASON_PREFERENCE, getString(R.string.none));
        if (preference.equals(getString(R.string.none))) {
          mUserPreference.Season = 0;
        } else {
          mUserPreference.Season = Integer.parseInt(preference);
        }
      }
    }
  }

  @Override
  public void onSelected(MatchSummary matchSummary) {

    LogUtils.debug(TAG, "++onSelected(MatchSummary)");
    setTitle("Match Details");
    replaceFragment(MatchDetailsFragment.newInstance(mUserPreference, matchSummary));
  }

  @Override
  public void onMatchUpdateFailed() {

    LogUtils.debug(TAG, "++onMatchUpdateFailed()");
    Snackbar.make(findViewById(R.id.fragment_container), getString(R.string.err_match_not_updated), Snackbar.LENGTH_LONG).show();
  }

  private void generateTrends() {

    LogUtils.debug(TAG, "++generateTrends()");
    Map<String, Object> mappedTrends = new HashMap<>();
    Map<String, Long> goalsAgainstMap = new HashMap<>();
    Map<String, Long> goalsForMap = new HashMap<>();
    Map<String, Long> goalDifferentialMap = new HashMap<>();
    Map<String, Long> totalPointsMap = new HashMap<>();
    Map<String, Double> pointsPerGameMap = new HashMap<>();
    Map<String, Long> maxPointsPossibleMap = new HashMap<>();
    Map<String, Long> pointsByAverageMap = new HashMap<>();

    long goalsAgainst;
    long goalDifferential;
    long goalsFor;
    long totalPoints;
    long prevGoalAgainst = 0;
    long prevGoalDifferential = 0;
    long prevGoalFor = 0;
    long prevTotalPoints = 0;
    long totalMatches = 34;
    long matchesRemaining = totalMatches;
    for (MatchSummary summary : mMatchSummaries) {
      if (summary.HomeTeamName.equals(mUserPreference.TeamFullName)) { // targetTeam is the home team
        goalsAgainst = summary.AwayScore;
        goalDifferential = summary.HomeScore - summary.AwayScore;
        goalsFor = summary.HomeScore;
        if (summary.HomeScore > summary.AwayScore) {
          totalPoints = (long) 3;
        } else if (summary.HomeScore < summary.AwayScore) {
          totalPoints = (long) 0;
        } else {
          totalPoints = (long) 1;
        }
      } else { // targetTeam is the away team
        goalsAgainst = summary.HomeScore;
        goalDifferential = summary.AwayScore - summary.HomeScore;
        goalsFor = summary.AwayScore;
        if (summary.AwayScore > summary.HomeScore) {
          totalPoints = (long) 3;
        } else if (summary.AwayScore < summary.HomeScore) {
          totalPoints = (long) 0;
        } else {
          totalPoints = (long) 1;
        }
      }

      String key = String.format(Locale.ENGLISH, "ID_%02d", summary.MatchDay);
      goalsAgainstMap.put(key, goalsAgainst + prevGoalAgainst);
      goalDifferentialMap.put(key, goalDifferential + prevGoalDifferential);
      goalsForMap.put(key, goalsFor + prevGoalFor);
      totalPointsMap.put(key, totalPoints + prevTotalPoints);
      maxPointsPossibleMap.put(key, (totalPoints + prevTotalPoints) + (--matchesRemaining * 3));

      double result = (double) totalPoints + prevTotalPoints;
      if (result > 0) {
        result = (totalPoints + prevTotalPoints) / (double) (totalPointsMap.size());
      }

      pointsPerGameMap.put(key, result);
      pointsByAverageMap.put(key, (long) (result * totalMatches));

      // update previous values for next pass
      prevGoalAgainst = goalsAgainst + prevGoalAgainst;
      prevGoalDifferential = goalDifferential + prevGoalDifferential;
      prevGoalFor = goalsFor + prevGoalFor;
      prevTotalPoints = totalPoints + prevTotalPoints;
    }

    mappedTrends.put("GoalsAgainst", goalsAgainstMap);
    mappedTrends.put("GoalDifferential", goalDifferentialMap);
    mappedTrends.put("GoalsFor", goalsForMap);
    mappedTrends.put("TotalPoints", totalPointsMap);
    mappedTrends.put("PointsPerGame", pointsPerGameMap);
    mappedTrends.put("MaxPointsPossible", maxPointsPossibleMap);
    mappedTrends.put("PointsByAverage", pointsByAverageMap);

    String queryPath = PathUtils.combine(Trend.ROOT, mUserPreference.Season, mUserPreference.TeamShortName);
    Map<String, Object> childUpdates = new HashMap<>();
    childUpdates.put(queryPath, mappedTrends);
    FirebaseDatabase.getInstance().getReference().updateChildren(
      childUpdates,
      (databaseError, databaseReference) -> {

        if (databaseError != null && databaseError.getCode() < 0) {
          LogUtils.error(TAG, "Could not generate trends: %s", databaseError.getMessage());
          Snackbar.make(findViewById(R.id.fragment_container), getString(R.string.err_trends_not_created), Snackbar.LENGTH_LONG).show();
        }
      });

    hideProgressDialog();
  }

  private void replaceFragment(Fragment fragment){

    LogUtils.debug(TAG, "++replaceFragment()");
    String backStateName = fragment.getClass().getName();
    if (mUserPreference.toString().length() > 0) {
      backStateName = String.format(
        Locale.ENGLISH,
        "%1s-%2s",
        fragment.getClass().getName(),
        mUserPreference.toString());
    }

    FragmentManager fragmentManager = getSupportFragmentManager();
    boolean fragmentPopped = fragmentManager.popBackStackImmediate (backStateName, 0);
    if (!fragmentPopped){ //fragment not in back stack, create it.
      FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
      fragmentTransaction.replace(R.id.fragment_container, fragment);
      fragmentTransaction.addToBackStack(backStateName);
      fragmentTransaction.commit();
    }
  }
}
