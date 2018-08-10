package net.frostedbytes.android.trendfeeder.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.database.Exclude;
import net.frostedbytes.android.trendfeeder.BaseActivity;

public class Team implements Parcelable {

  @Exclude
  public static final String ROOT = "Teams";

  public String FullName;

  public String Id;

  public String ShortName;

  public Team() {

    this.FullName = "";
    this.Id = BaseActivity.DEFAULT_ID;
    this.ShortName = "";
  }

  protected Team(Parcel in) {

    this.FullName = in.readString();
    this.Id = in.readString();
    this.ShortName = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {

    dest.writeString(this.FullName);
    dest.writeString(this.Id);
    dest.writeString(this.ShortName);
  }

  public static final Creator<Team> CREATOR = new Creator<Team>() {

    @Override
    public Team createFromParcel(Parcel in) {

      return new Team(in);
    }

    @Override
    public Team[] newArray(int size) {

      return new Team[size];
    }
  };
}
