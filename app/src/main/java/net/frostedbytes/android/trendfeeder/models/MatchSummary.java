package net.frostedbytes.android.trendfeeder.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.database.Exclude;
import java.util.HashMap;
import java.util.Map;
import net.frostedbytes.android.trendfeeder.BaseActivity;

public class MatchSummary implements Parcelable {

  @Exclude
  public static final String ROOT = "MatchSummaries";

  /**
   * Unique identifier for away team.
   */
  public String AwayId;

  /**
   * Goals scored by the away team.
   */
  public long AwayScore;

  /**
   * Unique identifier for home team.
   */
  public String HomeId;

  /**
   * Goals scored by the home team.
   */
  public long HomeScore;

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
    this.AwayId = BaseActivity.DEFAULT_ID;
    this.AwayScore = 0;
    this.HomeId = BaseActivity.DEFAULT_ID;
    this.HomeScore = 0;
    this.IsFinal = false;
    this.MatchId = BaseActivity.DEFAULT_ID;
    this.MatchDate = BaseActivity.DEFAULT_DATE;
    this.MatchDay = 0;
  }

  /**
   * Creates a mapped object based on values of this match summary object
   * @return A mapped object of match summary
   */
  public Map<String, Object> toMap() {

    HashMap<String, Object> result = new HashMap<>();
    result.put("AwayId", this.AwayId);
    result.put("AwayScore", this.AwayScore);
    result.put("HomeId", this.HomeId);
    result.put("HomeScore", this.HomeScore);
    result.put("IsFinal", this.IsFinal);
    result.put("MatchDate", this.MatchDate);
    result.put("MatchDay", this.MatchDay);
    return result;
  }

  protected MatchSummary(Parcel in) {

    this.AwayId = in.readString();
    this.AwayScore = in.readLong();
    this.HomeId = in.readString();
    this.HomeScore = in.readLong();
    this.IsFinal = in.readInt() != 0;
    this.MatchId = in.readString();
    this.MatchDate = in.readString();
    this.MatchDay = in.readInt();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {

    dest.writeString(this.AwayId);
    dest.writeLong(this.AwayScore);
    dest.writeString(this.HomeId);
    dest.writeLong(this.HomeScore);
    dest.writeInt(this.IsFinal?1:0);
    dest.writeString(this.MatchId);
    dest.writeString(this.MatchDate);
    dest.writeInt(this.MatchDay);
  }

  public static final Creator<MatchSummary> CREATOR = new Creator<MatchSummary>() {

    @Override
    public MatchSummary createFromParcel(Parcel in) {

      return new MatchSummary(in);
    }

    @Override
    public MatchSummary[] newArray(int size) {

      return new MatchSummary[size];
    }
  };
}
