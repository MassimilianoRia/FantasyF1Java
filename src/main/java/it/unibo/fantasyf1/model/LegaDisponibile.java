package it.unibo.fantasyf1.model;

/**
 * Dati mostrati dalla consultazione U5 delle leghe di un'edizione.
 */
public record LegaDisponibile(
    int id,
    String nome,
    int idAmministratore,
    String usernameAmministratore
) {
}
