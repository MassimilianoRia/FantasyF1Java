package it.unibo.fantasyf1.service;

/**
 * Esito leggibile del workflow A8/O1/O2/O3.
 */
public record ProcessingOutcome(
    int driverFantasyPoints,
    boolean weekendProcessable
) {
}
