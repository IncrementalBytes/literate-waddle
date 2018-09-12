package net.frostedbytes.android.trendfeeder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.frostedbytes.android.trendfeeder.fragments.CreateMatchFragment;
import net.frostedbytes.android.trendfeeder.fragments.EmptyFragment;
import net.frostedbytes.android.trendfeeder.fragments.MatchSummaryListFragment;
import net.frostedbytes.android.trendfeeder.fragments.MatchDetailsFragment;
import net.frostedbytes.android.trendfeeder.fragments.UserPreferencesFragment;
import net.frostedbytes.android.trendfeeder.models.MatchSummary;
import net.frostedbytes.android.trendfeeder.models.Team;
import net.frostedbytes.android.trendfeeder.models.Trend;
import net.frostedbytes.android.trendfeeder.models.UserPreference;
import net.frostedbytes.android.trendfeeder.utils.LogUtils;
import net.frostedbytes.android.trendfeeder.utils.PathUtils;
import net.frostedbytes.android.trendfeeder.utils.SortUtils;

public class MainActivity extends BaseActivity implements
  MatchSummaryListFragment.OnMatchListListener,
  CreateMatchFragment.OnCreateMatchListener,
  MatchDetailsFragment.OnMatchDetailsListener,
  UserPreferencesFragment.OnPreferencesListener {

  private static final String TAG = BASE_TAG + MainActivity.class.getSimpleName();

  private UserPreference mUserPreference;

  private ArrayList<MatchSummary> mAllSummaries;
  private ArrayList<MatchSummary> mSummaries;
  private String mSeasonalSummaryQueryPath;
  private ArrayList<Team> mTeams;

  private Query mMatchSummariesQuery;
  private Query mTeamsQuery;
  private ValueEventListener mMatchSummariesListener;
  private ValueEventListener mTeamsListener;

  @LayoutRes
  protected int getLayoutResId() {

    return R.layout.activity_masterdetail;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mAllSummaries = new ArrayList<>();
    mSummaries = new ArrayList<>();
    mTeams = new ArrayList<>();

    setContentView(getLayoutResId());
    if (findViewById(R.id.main_fragment_container_detail) == null) {
      Toolbar toolbar = findViewById(R.id.main_toolbar);
      setSupportActionBar(toolbar);
    } else {
      Toolbar toolbar = findViewById(R.id.main_tablet_toolbar);
      setSupportActionBar(toolbar);
    }

    mUserPreference = new UserPreference();
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    if (sharedPreferences.contains(UserPreferencesFragment.KEY_TEAM_PREFERENCE)) {
      String preference = sharedPreferences.getString(UserPreferencesFragment.KEY_TEAM_PREFERENCE, getString(R.string.none));
      if (preference.equals(getString(R.string.none))) {
        mUserPreference.TeamId = "";
      } else {
        try {
          UUID.fromString(preference);
          mUserPreference.TeamId = preference;
        } catch (Exception ex) {
          mUserPreference.TeamId = "";
        }
      }
    }

    if (sharedPreferences.contains(UserPreferencesFragment.KEY_SEASON_PREFERENCE)) {
      String preference = sharedPreferences.getString(UserPreferencesFragment.KEY_SEASON_PREFERENCE, getString(R.string.none));
      if (preference.equals(getString(R.string.none))) {
        mUserPreference.Season = Calendar.getInstance().get(Calendar.YEAR);
      } else {
        mUserPreference.Season = Integer.parseInt(preference);
      }
    }

    mSeasonalSummaryQueryPath = PathUtils.combine(MatchSummary.ROOT, mUserPreference.Season);

    // load team data and compare to firebase
    showProgressDialog(getString(R.string.checking_data));
    loadTeamData();

    // setup value listeners but do not add them yet
    mTeamsQuery = FirebaseDatabase.getInstance().getReference().child(Team.ROOT);
    mTeamsListener = new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

        LogUtils.debug(TAG, "Data changed under: %s", Team.ROOT);
        mTeams = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
          Team team = snapshot.getValue(Team.class);
          if (team != null) {
            team.Id = snapshot.getKey();
            mTeams.add(team);
          }
        }

        mTeams.sort(new SortUtils.ByTeamName());
      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {

        LogUtils.debug(TAG, "++onCancelled(DatabaseError");
        LogUtils.error(TAG, databaseError.getMessage());
      }
    };

    mMatchSummariesQuery = FirebaseDatabase.getInstance().getReference().child(mSeasonalSummaryQueryPath).orderByChild("MatchDay");
    mMatchSummariesListener = new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

        LogUtils.debug(TAG, "Data changed under: %s", mSeasonalSummaryQueryPath);
        mAllSummaries = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
          MatchSummary matchSummary = snapshot.getValue(MatchSummary.class);
          if (matchSummary != null) {
            matchSummary.MatchId = snapshot.getKey();
            mAllSummaries.add(matchSummary);
          }
        }

        mAllSummaries.sort((summary1, summary2) -> Integer.compare(summary2.MatchDate.compareTo(summary1.MatchDate), 0));

        // send only the match summaries for the user preferred team (if available)
        if (!mUserPreference.TeamId.isEmpty()) {
          mSummaries = new ArrayList<>();
          for (MatchSummary summary : mAllSummaries) {
            if (summary.HomeId.equals(mUserPreference.TeamId) || summary.AwayId.equals(mUserPreference.TeamId)) {
              mSummaries.add(summary);
            }
          }
        } else {
          mSummaries = new ArrayList<>(mAllSummaries);
        }

        if (findViewById(R.id.main_fragment_container_detail) == null) {
          replaceFragment(MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries));
        } else {
          Fragment fragment = MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries);
          Fragment emptyFragment = EmptyFragment.newInstance(getString(R.string.empty_default));
          getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .replace(R.id.main_fragment_container_detail, emptyFragment)
            .commit();
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {

        LogUtils.debug(TAG, "++onCancelled(DatabaseError");
        LogUtils.error(TAG, databaseError.getMessage());
      }
    };

    if (findViewById(R.id.main_fragment_container_detail) != null) {
      getSupportFragmentManager().beginTransaction()
        .replace(R.id.main_fragment_container, EmptyFragment.newInstance(""))
        .replace(R.id.main_fragment_container_detail, EmptyFragment.newInstance(""))
        .commit();
    }
  }

  @Override
  public void onCreateMatch() {

    LogUtils.debug(TAG, "++onCreateMatch()");
    setTitle("New Match");
    if (findViewById(R.id.main_fragment_container_detail) == null) {
      replaceFragment(CreateMatchFragment.newInstance(mTeams, mAllSummaries));
    } else {
      Fragment fragment = MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries);
      Fragment createFragment = CreateMatchFragment.newInstance(mTeams, mAllSummaries);
      getSupportFragmentManager().beginTransaction()
        .replace(R.id.main_fragment_container, fragment)
        .replace(R.id.main_fragment_container_detail, createFragment)
        .commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public void onDeleteMatch(MatchSummary matchSummary) {

    LogUtils.debug(TAG, "++onDeleteMatch(MatchSummary)");
    String queryPath = PathUtils.combine(MatchSummary.ROOT, mUserPreference.Season, matchSummary.MatchId);
    FirebaseDatabase.getInstance().getReference().child(queryPath).removeValue((databaseError, databaseReference) -> {

      if (databaseError != null && databaseError.getCode() < 0) {
        LogUtils.error(TAG, "Could not delete match: %s", databaseError.getMessage());
        Snackbar.make(findViewById(R.id.main_fragment_container), getString(R.string.err_match_not_deleted), Snackbar.LENGTH_LONG).show();
      } else {
        if (findViewById(R.id.main_fragment_container_detail) == null) {
          replaceFragment(MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries));
        } else {
          Fragment fragment = MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries);
          Fragment emptyFragment = EmptyFragment.newInstance(getString(R.string.empty_default));
          getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .replace(R.id.main_fragment_container_detail, emptyFragment)
            .commit();
        }
      }
    });

      // TODO: remove trends?
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LogUtils.debug(TAG, "++onDestroy()");
    if (mMatchSummariesQuery != null && mMatchSummariesListener != null) {
      mMatchSummariesQuery.removeEventListener(mMatchSummariesListener);
    }

    if (mTeamsQuery != null && mTeamsListener != null) {
      mTeamsQuery.removeEventListener(mTeamsListener);
    }

    mAllSummaries = null;
    mSummaries = null;
    mTeams = null;
  }

  @Override
  public void onMatchCreated(MatchSummary matchSummary) {

    LogUtils.debug(TAG, "++onMatchCreated(MatchSummary)");
    if (matchSummary != null) {
      String queryPath = PathUtils.combine(MatchSummary.ROOT, mUserPreference.Season);
      FirebaseDatabase.getInstance().getReference().child(queryPath).child(matchSummary.MatchId).setValue(
        matchSummary.toMap(),
        (databaseError, databaseReference) -> {

          if (databaseError != null && databaseError.getCode() < 0) {
            LogUtils.error(TAG, "Could not create match: %s", databaseError.getMessage());
            Snackbar.make(findViewById(R.id.main_fragment_container), getString(R.string.err_match_not_created), Snackbar.LENGTH_LONG).show();
          } else {
            if (findViewById(R.id.main_fragment_container_detail) == null) {
              replaceFragment(MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries));
            } else {
              Fragment fragment = MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries);
              Fragment emptyFragment = EmptyFragment.newInstance(getString(R.string.empty_default));
              getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .replace(R.id.main_fragment_container_detail, emptyFragment)
                .commit();
            }
          }
        });
    }
  }

  @Override
  public void onMatchUpdated(MatchSummary matchSummary) {

    LogUtils.debug(TAG, "++onMatchUpdated()");
    replaceFragment(MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries));
    if (matchSummary.IsFinal) {
      showProgressDialog(getString(R.string.generating_trends));
      this.generateTrends();
    }
  }

  @Override
  public void onMatchUpdateFailed() {

    LogUtils.debug(TAG, "++onMatchUpdateFailed()");
    Snackbar.make(findViewById(R.id.main_fragment_container), getString(R.string.err_match_not_updated), Snackbar.LENGTH_LONG).show();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
      case R.id.action_create:
        this.onCreateMatch();
        return true;
      case R.id.action_generate:
        this.generateTrends();
        return true;
      case R.id.action_settings:

        if (findViewById(R.id.main_fragment_container_detail) == null) {
          replaceFragment(UserPreferencesFragment.newInstance(mTeams));
        } else {
          Fragment fragment = MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries);
          Fragment settingsFragment = UserPreferencesFragment.newInstance(mTeams);
          getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .replace(R.id.main_fragment_container_detail, settingsFragment)
            .commit();
          Fragment emptyFragment = EmptyFragment.newInstance("");
          getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_fragment_container, emptyFragment)
            .commit();
        }

        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onPopulated() {

    LogUtils.debug(TAG, "++onPopulated()");
    hideProgressDialog();
    if (mUserPreference != null && !mUserPreference.TeamId.isEmpty()) {
      setTitle(getResources().getQuantityString(R.plurals.subtitle, mSummaries.size(), getTeamName(mUserPreference.TeamId), mSummaries.size()));
    } else {
      setTitle("Match Summaries");
      missingPreference();
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
          mUserPreference.TeamId = "";
        } else {
          try {
            UUID.fromString(preference);
            mUserPreference.TeamId = preference;
          } catch (Exception ex) {
            mUserPreference.TeamId = "";
          }
        }
      }

      if (sharedPreferences.contains(UserPreferencesFragment.KEY_SEASON_PREFERENCE)) {
        String preference = sharedPreferences.getString(UserPreferencesFragment.KEY_SEASON_PREFERENCE, getString(R.string.none));
        if (preference.equals(getString(R.string.none))) {
          mUserPreference.Season = Calendar.getInstance().get(Calendar.YEAR);
        } else {
          mUserPreference.Season = Integer.parseInt(preference);
        }

        mSeasonalSummaryQueryPath = PathUtils.combine(MatchSummary.ROOT, mUserPreference.Season);
      }
    }

    if (mUserPreference == null || mUserPreference.TeamId.isEmpty()) {
      missingPreference();
    } else { // send only the match summaries for the user preferred team (if available)
      mSummaries = new ArrayList<>();
      if (!mUserPreference.TeamId.isEmpty()) {
        for (MatchSummary summary : mAllSummaries) {
          if (summary.HomeId.equals(mUserPreference.TeamId) || summary.AwayId.equals(mUserPreference.TeamId)) {
            mSummaries.add(summary);
          }
        }
      } else {
        mSummaries = new ArrayList<>(mAllSummaries);
      }

      if (findViewById(R.id.main_fragment_container_detail) == null) {
        replaceFragment(MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries));
      } else {
        Fragment fragment = MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries);
        Fragment emptyFragment = EmptyFragment.newInstance(getString(R.string.empty_default));
        getSupportFragmentManager().beginTransaction()
          .replace(R.id.main_fragment_container, fragment)
          .replace(R.id.main_fragment_container_detail, emptyFragment)
          .commit();
      }
    }
  }

  @Override
  public void onSelected(MatchSummary matchSummary) {

    LogUtils.debug(TAG, "++onSelected(MatchSummary)");
    setTitle("Match Details");
    if (findViewById(R.id.main_fragment_container_detail) == null) {
      replaceFragment(MatchDetailsFragment.newInstance(mUserPreference, matchSummary, mTeams));
    } else {
      Fragment fragment = MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries);
      Fragment detailsFragment = MatchDetailsFragment.newInstance(mUserPreference, matchSummary, mTeams);
      getSupportFragmentManager().beginTransaction()
        .replace(R.id.main_fragment_container, fragment)
        .replace(R.id.main_fragment_container_detail, detailsFragment)
        .commit();
    }
  }

  private void checkMatchSummaryData(HashMap<String, MatchSummary> localMatchSummaries) {

    LogUtils.debug(TAG, "++checkMatchSummaryData(HashMap<String, MatchSummary>)");
    for (MatchSummary matchSummary : localMatchSummaries.values()) {
      if (!matchSummary.IsNew) {
        continue;
      }

      // add this match summary to firebase
      String queryPath = PathUtils.combine(MatchSummary.ROOT, mUserPreference.Season);
      FirebaseDatabase.getInstance().getReference().child(queryPath).child(matchSummary.MatchId).setValue(
        matchSummary,
        (databaseError, databaseReference) -> {

          if (databaseError != null && databaseError.getCode() < 0) {
            LogUtils.error(TAG, "Could not create match summary: %s", databaseError.getMessage());
            Snackbar.make(findViewById(R.id.main_fragment_container), getString(R.string.err_match_not_created), Snackbar.LENGTH_LONG).show();
          } else {
            LogUtils.debug(
              TAG,
              "Adding %s vs. %s on %s",
              getTeamName(matchSummary.HomeId),
              getTeamName(matchSummary.AwayId),
              matchSummary.MatchDate);
            mAllSummaries.add(matchSummary);
          }
        });
    }
  }

  private void checkTeamData(HashMap<String, Team> localTeams) {

    LogUtils.debug(TAG, "++checkTeamData(HashMap<String, Team>)");
    for (Team team : localTeams.values()) {
      if (!team.IsNew) {
        continue;
      }

      // add this team to firebase
      String queryPath = PathUtils.combine(Team.ROOT);
      FirebaseDatabase.getInstance().getReference().child(queryPath).child(team.Id).setValue(
        team,
        (databaseError, databaseReference) -> {

          if (databaseError != null && databaseError.getCode() < 0) {
            LogUtils.error(TAG, "Could not create team: %s", databaseError.getMessage());
            Snackbar.make(findViewById(R.id.main_fragment_container), getString(R.string.err_team_not_created), Snackbar.LENGTH_LONG).show();
          } else {
            LogUtils.debug(TAG, "Adding %s to known list of teams.", team.FullName);
            mTeams.add(team);
          }
        });
    }
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

    showProgressDialog(getString(R.string.generating_trends));
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
    if (mUserPreference == null || mUserPreference.Season == 0 || mUserPreference.TeamId.equals(BaseActivity.DEFAULT_ID)) {
      LogUtils.error(TAG, "Missing user preferences; halting trend generation.");
      Snackbar.make(findViewById(R.id.main_fragment_container), getString(R.string.err_missing_preferences), Snackbar.LENGTH_LONG).show();
    } else {
      int matchDay = 0;
      ArrayList<MatchSummary> temporary = new ArrayList<>(mSummaries);
      Collections.reverse(temporary);
      for (MatchSummary summary : temporary) {
        if (!summary.IsFinal) {
          continue;
        }

        if (summary.HomeId.equals(mUserPreference.TeamId)) { // targetTeam is the home team
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
        } else if (summary.AwayId.equals(mUserPreference.TeamId)) { // targetTeam is the away team
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
        } else {
          LogUtils.warn(TAG, "%s is neither Home or Away; skipping.", mUserPreference.TeamId);
          continue;
        }

        String key = String.format(Locale.ENGLISH, "ID_%02d", ++matchDay);
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

      String queryPath = PathUtils.combine(Trend.ROOT, mUserPreference.Season, mUserPreference.TeamId);
      Map<String, Object> childUpdates = new HashMap<>();
      childUpdates.put(queryPath, mappedTrends);
      FirebaseDatabase.getInstance().getReference().updateChildren(
        childUpdates,
        (databaseError, databaseReference) -> {

          if (databaseError != null && databaseError.getCode() < 0) {
            LogUtils.error(TAG, "Could not generate trends: %s", databaseError.getMessage());
            Snackbar.make(findViewById(R.id.main_fragment_container), getString(R.string.err_trends_not_created), Snackbar.LENGTH_LONG).show();
          }
        });
    }

    hideProgressDialog();
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

    return "N/A";
  }

  private void loadTeamData() {

    LogUtils.debug(TAG, "++loadTeamData()");
    String parsableString;
    HashMap<String, Team> localTeams = new HashMap<>();
    String resourcePath = "Teams.txt";
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(getAssets().open(resourcePath)));
      while ((parsableString = reader.readLine()) != null) { //process line
        if (parsableString.startsWith("--")) { // comment line; ignore
          continue;
        }

        // [UUID],[FRIENDLY_NAME],[ABBREVIATION]
        List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split(";")));
        Team team = new Team();
        team.Id = elements.remove(0);
        team.FullName = elements.remove(0);
        team.ShortName = elements.remove(0);
        localTeams.put(team.Id, team);
      }
    } catch (IOException e) {
      String errorMessage = getString(R.string.err_team_data_load_failed);
      LogUtils.error(TAG, errorMessage);
      Snackbar.make(findViewById(R.id.main_fragment_container), errorMessage, Snackbar.LENGTH_LONG).show();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          String errorMessage = getString(R.string.err_team_data_cleanup_failed);
          LogUtils.error(TAG, errorMessage);
          Snackbar.make(findViewById(R.id.main_fragment_container), errorMessage, Snackbar.LENGTH_LONG).show();
        }
      }
    }

    // when initially checking, we do not want to setup a listener that will fire if we need to add a new team
    LogUtils.debug(TAG, "Query: %s", Team.ROOT);
    Query teamsQuery = FirebaseDatabase.getInstance().getReference().child(Team.ROOT);
    ValueEventListener teamsListener = new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

        mTeams = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
          Team team = snapshot.getValue(Team.class);
          if (team != null) {
            team.Id = snapshot.getKey();
            mTeams.add(team);
            if (localTeams.containsKey(team.Id)) {
              localTeams.get(team.Id).IsNew = false;
            }
          }
        }

        checkTeamData(localTeams);
        mTeams.sort(new SortUtils.ByTeamName());

        // once teams are sorted out; move onto matches for user preferred season
        mTeamsQuery.addValueEventListener(mTeamsListener);

        // now process the match summaries
        loadMatchSummaryData();
      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {

        LogUtils.debug(TAG, "++onCancelled(DatabaseError)");
        LogUtils.error(TAG, "%s", databaseError.getDetails());
      }
    };
    teamsQuery.addListenerForSingleValueEvent(teamsListener);
  }

  private void loadMatchSummaryData() {

    LogUtils.debug(TAG, "++loadMatchSummaryData()");
    String parsableString;
    HashMap<String, MatchSummary> localMatchSummaries = new HashMap<>();
    String resourcePath = String.format(Locale.ENGLISH, "%d.txt", mUserPreference.Season);
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(getAssets().open(resourcePath)));
      while ((parsableString = reader.readLine()) != null) { //process line
        if (parsableString.startsWith("--")) { // comment line; ignore
          continue;
        }

        // [DATE, DD.MM.YYYY];[HOMEID];[AWAYID];[HOMESCORE] : [AWAYSCORE]
        System.out.println(String.format(Locale.ENGLISH, "Processing: %s", parsableString));
        List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split(";")));
        String dateString = elements.remove(0);
        List<String> dateElements = new ArrayList<>(Arrays.asList(dateString.split("\\.")));
        String dayElement = dateElements.remove(0);
        String monthElement = dateElements.remove(0);
        String yearElement = dateElements.remove(0);
        MatchSummary currentSummary = new MatchSummary();
        currentSummary.MatchDate = String.format(Locale.ENGLISH, "%s%s%s", yearElement, monthElement, dayElement);

        currentSummary.MatchId = UUID.randomUUID().toString();
        currentSummary.HomeId = elements.remove(0);
        currentSummary.AwayId = elements.remove(0);

        String scoreString = elements.remove(0);
        List<String> scoreElements = new ArrayList<>(Arrays.asList(scoreString.split(":")));
        currentSummary.HomeScore = Integer.parseInt(scoreElements.remove(0).trim());
        currentSummary.AwayScore = Integer.parseInt(scoreElements.remove(0).trim());

        // put last summary into collection
        currentSummary.IsFinal = true;
        localMatchSummaries.put(currentSummary.MatchDate + currentSummary.HomeId, currentSummary);
      }
    } catch (IOException e) {
      String errorMessage = getString(R.string.err_match_summary_data_load_failed);
      LogUtils.error(TAG, errorMessage);
      Snackbar.make(findViewById(R.id.main_fragment_container), errorMessage, Snackbar.LENGTH_LONG).show();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          String errorMessage = getString(R.string.err_match_summary_data_cleanup_failed);
          LogUtils.error(TAG, errorMessage);
          Snackbar.make(findViewById(R.id.main_fragment_container), errorMessage, Snackbar.LENGTH_LONG).show();
        }
      }
    }

    // when initially checking, we do not want to setup a listener that will fire if we need to add a new match summary
    Query matchSummariesQuery = FirebaseDatabase.getInstance().getReference().child(mSeasonalSummaryQueryPath).orderByChild("MatchDay");
    ValueEventListener matchSummariesListener = new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

        mAllSummaries = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
          MatchSummary matchSummary = snapshot.getValue(MatchSummary.class);
          if (matchSummary != null) {
            matchSummary.MatchId = snapshot.getKey();
            mAllSummaries.add(matchSummary);
            if (localMatchSummaries.containsKey(matchSummary.MatchDate + matchSummary.HomeId)) {
              localMatchSummaries.get(matchSummary.MatchDate + matchSummary.HomeId).IsNew = false;
            }
          }
        }

        checkMatchSummaryData(localMatchSummaries);
        mAllSummaries.sort((summary1, summary2) -> Integer.compare(summary2.MatchDate.compareTo(summary1.MatchDate), 0));

        mSummaries = new ArrayList<>();
        if (!mUserPreference.TeamId.isEmpty()) {
          for (MatchSummary summary : mAllSummaries) {
            if (summary.HomeId.equals(mUserPreference.TeamId) || summary.AwayId.equals(mUserPreference.TeamId)) {
              mSummaries.add(summary);
            }
          }
        } else {
          mSummaries = new ArrayList<>(mAllSummaries);
        }

        if (findViewById(R.id.main_fragment_container_detail) == null) {
          replaceFragment(MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries));
        } else {
          Fragment fragment = MatchSummaryListFragment.newInstance(mUserPreference, mTeams, mSummaries);
          Fragment emptyFragment = EmptyFragment.newInstance(getString(R.string.empty_default));
          getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .replace(R.id.main_fragment_container_detail, emptyFragment)
            .commit();
        }

        hideProgressDialog();

        // now that we have the match summaries up-to-date, we can hook up the all-the-time listener
        mMatchSummariesQuery.addValueEventListener(mMatchSummariesListener);
      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {

        LogUtils.debug(TAG, "++onCancelled(DatabaseError");
        LogUtils.error(TAG, databaseError.getMessage());
      }
    };
    matchSummariesQuery.addListenerForSingleValueEvent(matchSummariesListener);
  }

  private void missingPreference() {

    LogUtils.debug(TAG, "++missingPreference()");
    Snackbar snackbar = Snackbar.make(findViewById(R.id.main_fragment_container), getString(R.string.no_matches), Snackbar.LENGTH_INDEFINITE);
    snackbar.setAction("Settings", v -> {
      snackbar.dismiss();
      if (findViewById(R.id.main_fragment_container_detail) == null) {
        replaceFragment(UserPreferencesFragment.newInstance(mTeams));
      } else {
        Fragment emptyFragment = EmptyFragment.newInstance("");
        Fragment fragment = UserPreferencesFragment.newInstance(mTeams);
        getSupportFragmentManager().beginTransaction()
          .replace(R.id.main_fragment_container, emptyFragment)
          .replace(R.id.main_fragment_container_detail, fragment)
          .commit();
      }
    });
    snackbar.show();
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
      fragmentTransaction.replace(R.id.main_fragment_container, fragment);
      fragmentTransaction.addToBackStack(backStateName);
      fragmentTransaction.commit();
    }
  }
}
