package com.ko.footballupdater.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents the performance statistics of a player in a specific match.
 */
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
    private Integer crossesSuccessful;
    private Integer crossesSuccessPercentage;
    private String crossingAccuracyAll;
    private Integer dispossessed;
    private Integer touches;
    private Integer tackles;
    private Integer tacklesWon;
    private Integer tacklesSuccessPercentage;
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
    private Integer passesSuccessPercentage;
    private String passingAccuracyAll;
    private Integer longBallsAttempted;
    private Integer longBallsCompleted;
    private String longBallAccuracyAll;
    private Integer progressivePasses;
    private Integer passesIntoFinalThird;
    private Integer carries;
    private Integer carriesSuccessful;
    private Integer carriesSuccessPercentage;
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
    public PlayerMatchPerformanceStats(Match match) {
        this.match = match;
    }

    @Builder
    public PlayerMatchPerformanceStats(Integer id, DataSourceSiteName dataSourceSiteName, Match match, Integer minutesPlayed, Integer goals, Integer assists, Integer penaltiesScored, Integer penaltiesWon, Integer shots, Integer shotsOnTarget, Integer shotsBlocked, Integer yellowCards, Integer redCards, Integer fouls, Integer fouled, Integer offsides, Integer crosses, Integer crossesSuccessful, String crossingAccuracyAll, Integer dispossessed, Integer touches, Integer tackles, Integer tacklesWon, String tacklingSuccessAll, Integer interceptions, Integer blocks, Integer defensiveActions, Integer recoveries, Integer duelsWon, Integer duelsLost, Integer groundDuelsWon, Integer aerialDuelsWon, Float xg, Float xg_assist, Integer shotCreatingActions, Integer goalCreatingActions, Integer chancesCreatedAll, Integer passesCompleted, Integer passesAttempted, String passingAccuracyAll, Integer longBallsAttempted, Integer longBallsCompleted, String longBallAccuracyAll, Integer progressivePasses, Integer passesIntoFinalThird, Integer carries, Integer carriesSuccessful, Integer progressiveCarries, String carriesSuccessAll, Integer takesOnsAttempted, Integer takesOnsCompleted, Integer gkShotsOnTargetAgainst, Integer gkGoalsAgainst, Integer gkSaves, Float gkSavePercentage, String gkSavesAll, Integer gkPunches, Integer gkThrows, Integer gkHighClaim, Integer gkRecoveries, Integer gkPenaltiesAttemptedAgainst, Integer gkPenaltiesScoredAgainst, Integer gkPenaltiesSaved) {
        this.id = id;
        this.dataSourceSiteName = dataSourceSiteName;
        this.match = match;
        this.minutesPlayed = minutesPlayed;
        this.goals = goals;
        this.assists = assists;
        this.penaltiesScored = penaltiesScored;
        this.penaltiesWon = penaltiesWon;
        this.shots = shots;
        this.shotsOnTarget = shotsOnTarget;
        this.shotsBlocked = shotsBlocked;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
        this.fouls = fouls;
        this.fouled = fouled;
        this.offsides = offsides;
        this.crosses = crosses;
        this.crossesSuccessful = crossesSuccessful;
        this.crossingAccuracyAll = crossingAccuracyAll;
        this.dispossessed = dispossessed;
        this.touches = touches;
        this.tackles = tackles;
        this.tacklesWon = tacklesWon;
        this.tacklingSuccessAll = tacklingSuccessAll;
        this.interceptions = interceptions;
        this.blocks = blocks;
        this.defensiveActions = defensiveActions;
        this.recoveries = recoveries;
        this.duelsWon = duelsWon;
        this.duelsLost = duelsLost;
        this.groundDuelsWon = groundDuelsWon;
        this.aerialDuelsWon = aerialDuelsWon;
        this.xg = xg;
        this.xg_assist = xg_assist;
        this.shotCreatingActions = shotCreatingActions;
        this.goalCreatingActions = goalCreatingActions;
        this.chancesCreatedAll = chancesCreatedAll;
        this.passesCompleted = passesCompleted;
        this.passesAttempted = passesAttempted;
        this.passingAccuracyAll = passingAccuracyAll;
        this.longBallsAttempted = longBallsAttempted;
        this.longBallsCompleted = longBallsCompleted;
        this.longBallAccuracyAll = longBallAccuracyAll;
        this.progressivePasses = progressivePasses;
        this.passesIntoFinalThird = passesIntoFinalThird;
        this.carries = carries;
        this.carriesSuccessful = carriesSuccessful;
        this.progressiveCarries = progressiveCarries;
        this.carriesSuccessAll = carriesSuccessAll;
        this.takesOnsAttempted = takesOnsAttempted;
        this.takesOnsCompleted = takesOnsCompleted;
        this.gkShotsOnTargetAgainst = gkShotsOnTargetAgainst;
        this.gkGoalsAgainst = gkGoalsAgainst;
        this.gkSaves = gkSaves;
        this.gkSavePercentage = gkSavePercentage;
        this.gkSavesAll = gkSavesAll;
        this.gkPunches = gkPunches;
        this.gkThrows = gkThrows;
        this.gkHighClaim = gkHighClaim;
        this.gkRecoveries = gkRecoveries;
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
