package it.unibo.fantasyf1.service;

/**
 * Dati amministrativi A8.
 */
public record PerformanceRequest(
    int editionId,
    int grandPrixId,
    int driverId,
    Integer qualifyingPosition,
    Integer racePosition,
    boolean penalized,
    boolean fastestLap
) {
}
