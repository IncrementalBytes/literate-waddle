package net.frostedbytes.android.trendfeeder.utils;

import java.util.Comparator;
import net.frostedbytes.android.trendfeeder.models.Team;

public class SortUtils {

  public static class ByTeamName implements Comparator<Team>
  {
    public int compare(Team a, Team b) {

      return a.FullName.compareTo(b.FullName);
    }
  }
}
