package net.frostedbytes.android.trendfeeder.models;

import com.google.firebase.database.Exclude;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.frostedbytes.android.trendfeeder.BaseActivity;

public class MatchSummary implements Serializable {

  @Exclude
  public static final String ROOT = "MatchSummaries";

  /**
   * Goals scored by the away team.
   */
  public long AwayScore;

  /**
   * Name for away team; used as identifier for other classes.
   */
  public String AwayTeamName;

  /**
   * Goals scored by the home team.
   */
  public long HomeScore;

  /**
   * Name for home team; used as identifier for other classes.
   */
  public String HomeTeamName;

  /**
   * Value indicating whether or not this match is final.
   */
  public boolean IsFinal;

  /**
   * Unique identifier for Match object; used as key for json.
   */
  public String MatchId;

  /**
   * Number of ticks representing the year, month, and date of this match.
   */
  public String MatchDate;

  /**
   * Iterated value representing the match throughout a season.
   */
  public int MatchDay;

  /**
   * Constructs a new MatchSummary object with default values.
   */
  public MatchSummary() {

    // Default constructor required for calls to DataSnapshot.getValue(MatchSummary.class)
    this.AwayScore = 0;
    this.AwayTeamName = "";
    this.HomeScore = 0;
    this.HomeTeamName = "";
    this.IsFinal = false;
    this.MatchId = BaseActivity.DEFAULT_ID;
    this.MatchDate = BaseActivity.DEFAULT_DATE;
    this.MatchDay = 0;
  }

  @Override
  public String toString() {

    return String.format(Locale.ENGLISH, "%1s vs. %2s - %s", this.HomeTeamName, this.AwayTeamName, this.MatchDate);
  }

  /**
   * Creates a mapped object based on values of this match summary object
   * @return A mapped object of match summary
   */
  public Map<String, Object> toMap() {

    HashMap<String, Object> result = new HashMap<>();
    result.put("AwayScore", AwayScore);
    result.put("AwayTeamName", AwayTeamName);
    result.put("HomeScore", HomeScore);
    result.put("HomeTeamName", HomeTeamName);
    result.put("IsFinal", IsFinal);
    result.put("MatchDate", MatchDate);
    result.put("MatchDay", MatchDay);
    return result;
  }
}
