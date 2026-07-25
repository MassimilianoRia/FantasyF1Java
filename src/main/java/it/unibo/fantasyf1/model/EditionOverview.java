package it.unibo.fantasyf1.model;

import java.util.List;
import java.util.Objects;

/**
 * Fotografia consultabile del popolamento di un'edizione.
 */
public record EditionOverview(
    Edizione edition,
    EditionStatus status,
    List<RaceWeekend> weekends,
    List<EnrolledConstructorOption> constructors,
    List<DriverOption> drivers
) {

    public EditionOverview {
        edition = Objects.requireNonNull(
            edition,
            "L'edizione non può essere null"
        );
        status = Objects.requireNonNull(
            status,
            "Lo stato dell'edizione non può essere null"
        );
        weekends = List.copyOf(weekends);
        constructors = List.copyOf(constructors);
        drivers = List.copyOf(drivers);
    }
}
