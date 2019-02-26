package net.frostedbytes.android.trendfeeder.utils;

import java.util.Comparator;
import net.frostedbytes.android.trendfeeder.models.Team;

public class SortUtils {

    public static class ByTeamName implements Comparator<Team> {

        public int compare(Team a, Team b) {

            return a.FullName.compareTo(b.FullName);
        }
    }

    public static class ByTotalPoints implements Comparator<Team> {

        public int compare(Team a, Team b) {

            if (a.TotalPoints == b.TotalPoints) {
                if (a.TotalWins == b.TotalWins) {
                    if (a.GoalDifferential == b.GoalDifferential) {
                        return Long.compare(a.GoalsScored, b.GoalsScored);
                    }

                    return Long.compare(a.GoalDifferential, b.GoalDifferential);
                }

                return Long.compare(a.TotalWins, b.TotalWins);
            }

            return Long.compare(a.TotalPoints, b.TotalPoints);
        }
    }
}
