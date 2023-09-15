package com.ko.footballupdater.models;

import lombok.Getter;

@Getter
public class PlayerMatchPerformanceStats {

    private final Match match;
    private final Integer minutesPlayed;
    private final Integer goals;
    private final Integer assists;
    private final Integer penaltiesScored;
    private final Integer penaltiesWon;
    private final Integer shots;
    private final Integer shotsOnTarget;
    private final Integer yellowCards;
    private final Integer redCards;
    private final Integer fouls;
    private final Integer fouled;
    private final Integer offsides;
    private final Integer crosses;
    private final Integer touches;
    private final Integer tackles;
    private final Integer tacklesWon;
    private final Integer interceptions;
    private final Integer blocks;
    private final Float xg;
    private final Float xg_assist;
    private final Integer shotCreatingActions;
    private final Integer goalCreatingActions;
    private final Integer passesCompleted;
    private final Integer passesAttempted;
    private final Integer passesSuccessPercentage;
    private final Integer progressivePasses;
    private final Integer carries;
    private final Integer progressiveCarries;
    private final Integer takesOnsAttempted;
    private final Integer takesOnsCompleted;
    private final Integer gkShotsOnTargetAgainst;
    private final Integer gkGoalsAgainst;
    private final Integer gkSaves;
    private final Integer gkSavePercentage;
    private final Integer gkPenaltiesAttamptedAgainst;
    private final Integer gkPenaltiesScoredAgainst;
    private final Integer gkPenaltiesSaved;

    public PlayerMatchPerformanceStats(Match match, Integer minutesPlayed, Integer goals, Integer assists, Integer penaltiesScored, Integer penaltiesWon, Integer shots, Integer shotsOnTarget, Integer yellowCards, Integer redCards, Integer fouls, Integer fouled, Integer offsides, Integer crosses, Integer touches, Integer tackles, Integer tacklesWon, Integer interceptions, Integer blocks, Float xg, Float xg_assist, Integer shotCreatingActions, Integer goalCreatingActions, Integer passesCompleted, Integer passesAttempted, Integer passesSuccessPercentage, Integer progressivePasses, Integer carries, Integer progressiveCarries, Integer takesOnsAttempted, Integer takesOnsCompleted, Integer gkShotsOnTargetAgainst, Integer gkGoalsAgainst, Integer gkSaves, Integer gkSavePercentage, Integer gkPenaltiesAttamptedAgainst, Integer gkPenaltiesScoredAgainst, Integer gkPenaltiesSaved) {
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
        this.gkPenaltiesAttamptedAgainst = gkPenaltiesAttamptedAgainst;
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
        if (shotsOnTarget != null) {
            builder.append("Shots on Target: ").append(shotsOnTarget).append("\n");
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
        if (fouled != null) {
            builder.append("Fouled: ").append(fouled).append("\n");
        }
        if (offsides != null) {
            builder.append("Offsides: ").append(offsides).append("\n");
        }
        if (crosses != null) {
            builder.append("Crosses: ").append(crosses).append("\n");
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
        if (interceptions != null) {
            builder.append("Interceptions: ").append(interceptions).append("\n");
        }
        if (blocks != null) {
            builder.append("Blocks: ").append(blocks).append("\n");
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
        if (passesCompleted != null) {
            builder.append("Passes Completed: ").append(passesCompleted).append("\n");
        }
        if (passesAttempted != null) {
            builder.append("Passes Attempted: ").append(passesAttempted).append("\n");
        }
        if (passesSuccessPercentage != null) {
            builder.append("Pass Success Percentage: ").append(passesSuccessPercentage).append("\n");
        }
        if (progressivePasses != null) {
            builder.append("Progressive Passes: ").append(progressivePasses).append("\n");
        }
        if (carries != null) {
            builder.append("Carries: ").append(carries).append("\n");
        }
        if (progressiveCarries != null) {
            builder.append("Progressive Carries: ").append(progressiveCarries).append("\n");
        }
        if (takesOnsAttempted != null) {
            builder.append("Take-Ons Attempted: ").append(takesOnsAttempted).append("\n");
        }
        if (takesOnsCompleted != null) {
            builder.append("Take-Ons Completed: ").append(takesOnsCompleted).append("\n");
        }

        return builder.toString();
    }
}
