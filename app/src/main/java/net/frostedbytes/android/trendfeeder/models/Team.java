package net.frostedbytes.android.trendfeeder.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.database.Exclude;
import java.util.Locale;
import net.frostedbytes.android.trendfeeder.BaseActivity;

public class Team implements Parcelable {

  @Exclude
  public static final String ROOT = "Teams";

  public int ConferenceId;

  public int Defunct;

  public int Established;

  public String FullName;

  @Exclude
  public String Id;

  @Exclude
  public boolean IsNew;

  public String ShortName;

  public Team() {

    this.ConferenceId = 0;
    this.Defunct = 0;
    this.Established = 0;
    this.FullName = "";
    this.Id = BaseActivity.DEFAULT_ID;
    this.IsNew = true;
    this.ShortName = "";
  }

  protected Team(Parcel in) {

    this.ConferenceId = in.readInt();
    this.Defunct = in.readInt();
    this.Established = in.readInt();
    this.FullName = in.readString();
    this.Id = in.readString();
    this.IsNew = in.readInt() != 0;
    this.ShortName = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public String toString() {

    return String.format(
      Locale.ENGLISH,
      "%s (%d-%s)",
      this.FullName,
      this.Established,
      this.Defunct == 0 ? "present" : String.valueOf(this.Defunct));
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {

    dest.writeInt(this.ConferenceId);
    dest.writeInt(this.Defunct);
    dest.writeInt(this.Established);
    dest.writeString(this.FullName);
    dest.writeString(this.Id);
    dest.writeInt(this.IsNew?1:0);
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
