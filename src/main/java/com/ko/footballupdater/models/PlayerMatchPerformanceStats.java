package com.ko.footballupdater.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "match_performance_stats")
public class PlayerMatchPerformanceStats {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    private DataSourceSiteName dataSourceSiteName;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "match_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private Match match;

    private Integer minutesPlayed;
    private Integer goals;
    private Integer assists;
    private Integer penaltiesScored;
    private Integer penaltiesWon;
    private Integer shots;
    private Integer shotsOnTarget;
    private Integer shotsBlocked;
    private Integer yellowCards;
    private Integer redCards;
    private Integer fouls;
    private Integer fouled;
    private Integer offsides;
    private Integer crosses;
    private String crossingAccuracyAll;
    private Integer dispossessed;
    private Integer touches;
    private Integer tackles;
    private Integer tacklesWon;
    private String tacklingSuccessAll;
    private Integer interceptions;
    private Integer blocks;
    private Integer defensiveActions;
    private Integer recoveries;
    private Integer duelsWon;
    private Integer duelsLost;
    private Integer groundDuelsWon;
    private Integer aerialDuelsWon;
    private Float xg;
    private Float xg_assist;
    private Integer shotCreatingActions;
    private Integer goalCreatingActions;
    private Integer chancesCreatedAll;
    private Integer passesCompleted;
    private Integer passesAttempted;
    private Float passesSuccessPercentage;
    private String passingAccuracyAll;
    private String longBallAccuracyAll;
    private Integer progressivePasses;
    private Integer passesIntoFinalThird;
    private Integer carries;
    private Integer progressiveCarries;
    private String carriesSuccessAll;
    private Integer takesOnsAttempted;
    private Integer takesOnsCompleted;
    private Integer gkShotsOnTargetAgainst;
    private Integer gkGoalsAgainst;
    private Integer gkSaves;
    private Float gkSavePercentage;
    private String gkSavesAll;
    private Integer gkPunches;
    private Integer gkThrows;
    private Integer gkHighClaim;
    private Integer gkRecoveries;
    private Integer gkPenaltiesAttemptedAgainst;
    private Integer gkPenaltiesScoredAgainst;
    private Integer gkPenaltiesSaved;

    public PlayerMatchPerformanceStats() {
    }

    // For tests
    public PlayerMatchPerformanceStats(DataSourceSiteName dataSourceSiteName) {
        this.dataSourceSiteName = dataSourceSiteName;
    }

    public PlayerMatchPerformanceStats(DataSourceSiteName dataSourceSiteName, Match match, Integer minutesPlayed, Integer goals, Integer yellowCards, Integer redCards) {
        this.dataSourceSiteName = dataSourceSiteName;
        this.match = match;
        this.minutesPlayed = minutesPlayed;
        this.goals = goals;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
    }

    // Used by FOTMOB goalkeepers
    public PlayerMatchPerformanceStats(DataSourceSiteName dataSourceSiteName, Match match, Integer minutesPlayed, Integer touches, String passingAccuracyAll, String longBallAccuracyAll, Integer gkGoalsAgainst, String gkSavesAll, Integer gkPunches, Integer gkThrows, Integer gkHighClaim, Integer gkRecoveries) {
        this.dataSourceSiteName = dataSourceSiteName;
        this.match = match;
        this.minutesPlayed = minutesPlayed;
        this.touches = touches;
        this.passingAccuracyAll = passingAccuracyAll;
        this.longBallAccuracyAll = longBallAccuracyAll;
        this.gkGoalsAgainst = gkGoalsAgainst;
        this.gkSavesAll = gkSavesAll;
        this.gkPunches = gkPunches;
        this.gkThrows = gkThrows;
        this.gkHighClaim = gkHighClaim;
        this.gkRecoveries = gkRecoveries;
    }

    // Used by FOTMOB outfield players
    public PlayerMatchPerformanceStats(DataSourceSiteName dataSourceSiteName, Match match, Integer minutesPlayed, Integer goals, Integer assists, Integer shots, Integer shotsBlocked, Integer fouls, Integer fouled, Integer offsides, String crossingAccuracyAll, Integer dispossessed, Integer touches, String tacklingSuccessAll, Integer defensiveActions, Integer recoveries, Integer duelsWon, Integer duelsLost, Integer groundDuelsWon, Integer aerialDuelsWon, Integer chancesCreatedAll, String passingAccuracyAll, Integer passesIntoFinalThird, String carriesSuccessAll) {
        this.dataSourceSiteName = dataSourceSiteName;
        this.match = match;
        this.minutesPlayed = minutesPlayed;
        this.goals = goals;
        this.assists = assists;
        this.shots = shots;
        this.shotsBlocked = shotsBlocked;
        this.fouls = fouls;
        this.fouled = fouled;
        this.offsides = offsides;
        this.crossingAccuracyAll = crossingAccuracyAll;
        this.dispossessed = dispossessed;
        this.touches = touches;
        this.tacklingSuccessAll = tacklingSuccessAll;
        this.defensiveActions = defensiveActions;
        this.recoveries = recoveries;
        this.duelsWon = duelsWon;
        this.duelsLost = duelsLost;
        this.groundDuelsWon = groundDuelsWon;
        this.aerialDuelsWon = aerialDuelsWon;
        this.chancesCreatedAll = chancesCreatedAll;
        this.passingAccuracyAll = passingAccuracyAll;
        this.passesIntoFinalThird = passesIntoFinalThird;
        this.carriesSuccessAll = carriesSuccessAll;
    }

    // Used by FBREF
    public PlayerMatchPerformanceStats(DataSourceSiteName dataSourceSiteName, Match match, Integer minutesPlayed, Integer goals, Integer assists, Integer penaltiesScored, Integer penaltiesWon, Integer shots, Integer shotsOnTarget, Integer yellowCards, Integer redCards, Integer fouls, Integer fouled, Integer offsides, Integer crosses, Integer touches, Integer tackles, Integer tacklesWon, Integer interceptions, Integer blocks, Float xg, Float xg_assist, Integer shotCreatingActions, Integer goalCreatingActions, Integer passesCompleted, Integer passesAttempted, Float passesSuccessPercentage, Integer progressivePasses, Integer carries, Integer progressiveCarries, Integer takesOnsAttempted, Integer takesOnsCompleted, Integer gkShotsOnTargetAgainst, Integer gkGoalsAgainst, Integer gkSaves, Float gkSavePercentage, Integer gkPenaltiesAttemptedAgainst, Integer gkPenaltiesScoredAgainst, Integer gkPenaltiesSaved) {
        this.dataSourceSiteName = dataSourceSiteName;
        this.match = match;
        this.minutesPlayed = minutesPlayed;
        this.goals = goals;
        this.assists = assists;
        this.penaltiesScored = penaltiesScored;
        this.penaltiesWon = penaltiesWon;
        this.shots = shots;
        this.shotsOnTarget = shotsOnTarget;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
        this.fouls = fouls;
        this.fouled = fouled;
        this.offsides = offsides;
        this.crosses = crosses;
        this.touches = touches;
        this.tackles = tackles;
        this.tacklesWon = tacklesWon;
        this.interceptions = interceptions;
        this.blocks = blocks;
        this.xg = xg;
        this.xg_assist = xg_assist;
        this.shotCreatingActions = shotCreatingActions;
        this.goalCreatingActions = goalCreatingActions;
        this.passesCompleted = passesCompleted;
        this.passesAttempted = passesAttempted;
        this.passesSuccessPercentage = passesSuccessPercentage;
        this.progressivePasses = progressivePasses;
        this.carries = carries;
        this.progressiveCarries = progressiveCarries;
        this.takesOnsAttempted = takesOnsAttempted;
        this.takesOnsCompleted = takesOnsCompleted;
        this.gkShotsOnTargetAgainst = gkShotsOnTargetAgainst;
        this.gkGoalsAgainst = gkGoalsAgainst;
        this.gkSaves = gkSaves;
        this.gkSavePercentage = gkSavePercentage;
        this.gkPenaltiesAttemptedAgainst = gkPenaltiesAttemptedAgainst;
        this.gkPenaltiesScoredAgainst = gkPenaltiesScoredAgainst;
        this.gkPenaltiesSaved = gkPenaltiesSaved;
    }

    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();

        // Append performance statistics if they are not null
        if (minutesPlayed != null) {
            builder.append("Minutes Played: ").append(minutesPlayed).append("\n");
        }
        if (goals != null) {
            builder.append("Goals: ").append(goals).append("\n");
        }
        if (assists != null) {
            builder.append("Assists: ").append(assists).append("\n");
        }
        if (penaltiesScored != null && penaltiesScored != 0) {
            builder.append("Penalties Scored: ").append(penaltiesScored).append("\n");
        }
        if (penaltiesWon != null && penaltiesWon != 0) {
            builder.append("Penalties Won: ").append(penaltiesWon).append("\n");
        }
        if (shots != null) {
            builder.append("Shots: ").append(shots).append("\n");
        }
        if (shotsOnTarget != null && shotsOnTarget != 0) {
            builder.append("Shots on Target: ").append(shotsOnTarget).append("\n");
        }
        if (shotsBlocked != null && shotsBlocked != 0) {
            builder.append("Shots Blocked: ").append(shotsBlocked).append("\n");
        }
        if (yellowCards != null && yellowCards != 0) {
            builder.append("Yellow Cards: ").append(yellowCards).append("\n");
        }
        if (redCards != null && redCards != 0) {
            builder.append("Red Cards: ").append(redCards).append("\n");
        }
        if (fouls != null) {
            builder.append("Fouls: ").append(fouls).append("\n");
        }
        if (fouled != null && fouled != 0) {
            builder.append("Fouled: ").append(fouled).append("\n");
        }
        if (offsides != null && offsides != 0) {
            builder.append("Offsides: ").append(offsides).append("\n");
        }
        if (crosses != null && crosses != 0) {
            builder.append("Crosses: ").append(crosses).append("\n");
        }
        if (crossingAccuracyAll != null) {
            builder.append("Crossing Accuracy: ").append(crossingAccuracyAll).append("\n");
        }
        if (dispossessed != null) {
            builder.append("Dispossessed: ").append(dispossessed).append("\n");
        }
        if (touches != null) {
            builder.append("Touches: ").append(touches).append("\n");
        }
        if (tackles != null) {
            builder.append("Tackles: ").append(tackles).append("\n");
        }
        if (tacklesWon != null) {
            builder.append("Tackles Won: ").append(tacklesWon).append("\n");
        }
        if (tacklingSuccessAll != null) {
            builder.append("Tackling Success: ").append(tacklingSuccessAll).append("\n");
        }
        if (interceptions != null) {
            builder.append("Interceptions: ").append(interceptions).append("\n");
        }
        if (blocks != null) {
            builder.append("Blocks: ").append(blocks).append("\n");
        }
        if (defensiveActions != null) {
            builder.append("Defensive Actions: ").append(defensiveActions).append("\n");
        }
        if (recoveries != null) {
            builder.append("Recoveries: ").append(recoveries).append("\n");
        }
        if (duelsWon != null) {
            builder.append("Duels Won: ").append(duelsWon).append("\n");
        }
        if (duelsLost != null) {
            builder.append("Duels Lost: ").append(duelsLost).append("\n");
        }
        if (groundDuelsWon != null) {
            builder.append("Ground Duels Won: ").append(groundDuelsWon).append("\n");
        }
        if (aerialDuelsWon != null) {
            builder.append("Aerial Duels Won: ").append(aerialDuelsWon).append("\n");
        }
        if (xg != null) {
            builder.append("Expected Goals (xG): ").append(xg).append("\n");
        }
        if (xg_assist != null) {
            builder.append("Expected Goals Assist (xA): ").append(xg_assist).append("\n");
        }
        if (shotCreatingActions != null) {
            builder.append("Shot Creating Actions: ").append(shotCreatingActions).append("\n");
        }
        if (goalCreatingActions != null) {
            builder.append("Goal Creating Actions: ").append(goalCreatingActions).append("\n");
        }
        if (chancesCreatedAll != null) {
            builder.append("Chances Created: ").append(chancesCreatedAll).append("\n");
        }
        if (passesCompleted != null) {
            builder.append("Passes Completed: ").append(passesCompleted).append("\n");
        }
        if (passesAttempted != null) {
            builder.append("Passes Attempted: ").append(passesAttempted).append("\n");
        }
        if (passesSuccessPercentage != null) {
            builder.append("Pass Success Percentage: ").append(passesSuccessPercentage).append("\n");
        }
        if (passingAccuracyAll != null) {
            builder.append("Passing Accuracy: ").append(passingAccuracyAll).append("\n");
        }
        if (progressivePasses != null) {
            builder.append("Progressive Passes: ").append(progressivePasses).append("\n");
        }
        if (passesIntoFinalThird != null) {
            builder.append("Passes Into Final Third: ").append(passesIntoFinalThird).append("\n");
        }
        if (carries != null) {
            builder.append("Carries: ").append(carries).append("\n");
        }
        if (progressiveCarries != null) {
            builder.append("Progressive Carries: ").append(progressiveCarries).append("\n");
        }
        if (carriesSuccessAll != null) {
            builder.append("Carries Success Rate: ").append(carriesSuccessAll).append("\n");
        }
        if (takesOnsAttempted != null) {
            builder.append("Take-Ons Attempted: ").append(takesOnsAttempted).append("\n");
        }
        if (takesOnsCompleted != null) {
            builder.append("Take-Ons Completed: ").append(takesOnsCompleted).append("\n");
        }
        if (gkShotsOnTargetAgainst != null) {
            builder.append("Shots On Target Against: ").append(gkShotsOnTargetAgainst).append("\n");
        }
        if (gkGoalsAgainst != null) {
            builder.append("Goals Against: ").append(gkGoalsAgainst).append("\n");
        }
        if (gkSaves != null) {
            builder.append("Saves: ").append(gkSaves).append("\n");
        }
        if (gkSavePercentage != null) {
            builder.append("Save Percentage: ").append(gkSavePercentage).append("\n");
        }
        if (gkPenaltiesAttemptedAgainst != null) {
            builder.append("Penalties Attempted Against: ").append(gkPenaltiesAttemptedAgainst).append("\n");
        }
        if (gkPenaltiesScoredAgainst != null) {
            builder.append("Penalties Scored Against: ").append(gkPenaltiesScoredAgainst).append("\n");
        }
        if (gkPenaltiesSaved != null) {
            builder.append("Penalties Saved: ").append(gkPenaltiesSaved).append("\n");
        }
        builder.append("\n");

        return builder.toString();
    }
}
