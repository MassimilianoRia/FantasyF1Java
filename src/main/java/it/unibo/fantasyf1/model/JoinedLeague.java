package it.unibo.fantasyf1.model;

/**
 * Lega a cui partecipa uno dei team dell'utente autenticato.
 */
public record JoinedLeague(
    int leagueId,
    String leagueName,
    int teamId,
    String teamName
) {

    @Override
    public String toString() {
        return "%s — team: %s".formatted(leagueName, teamName);
    }
}
