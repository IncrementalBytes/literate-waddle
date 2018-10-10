package net.frostedbytes.android.trendfeeder.models;

import com.google.firebase.database.Exclude;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Trend implements Serializable {

  @Exclude
  public static final String AGGREGATE_ROOT = "Aggregate";

  @Exclude
  public static final String ROOT = "Trends";

  /**
   * List containing the accumulated goals scored against.
   */
  public HashMap<String, Long> GoalsAgainst;

  /**
   * List containing the accumulated goal differential.
   */
  public HashMap<String, Long> GoalDifferential;

  /**
   * List containing the accumulated goals scored.
   */
  public HashMap<String, Long> GoalsFor;

  /**
   * List containing the accumulated points per game.
   */
  public HashMap<String, Double> PointsPerGame;

  /**
   * List containing the accumulated total points.
   */
  public HashMap<String, Long> TotalPoints;

  /**
   * Year (season) this trend represents.
   */
  public int Year;

  /**
   * Constructs a new Trend object with default values.
   */
  public Trend() {

    // Default constructor required for calls to DataSnapshot.getValue(Trend.class)
    this.GoalsAgainst = new HashMap<>();
    this.GoalDifferential = new HashMap<>();
    this.GoalsFor = new HashMap<>();
    this.PointsPerGame = new HashMap<>();
    this.TotalPoints = new HashMap<>();
    this.Year = 0;
  }

  /**
   * Creates a mapped object based on values of this trend object
   * @return A mapped object of trend
   */
  public Map<String, Object> toMap() {

    HashMap<String, Object> result = new HashMap<>();
    result.put("GoalsAgainst", GoalsAgainst);
    result.put("GoalDifferential", GoalDifferential);
    result.put("GoalsFor", GoalsFor);
    result.put("PointsPerGame", PointsPerGame);
    result.put("TotalPoints", TotalPoints);
    result.put("Year", Year);
    return result;
  }
}
