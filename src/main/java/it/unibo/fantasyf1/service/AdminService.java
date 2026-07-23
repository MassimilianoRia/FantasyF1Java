package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.model.ConstructorOption;
import it.unibo.fantasyf1.model.DriverOption;
import it.unibo.fantasyf1.model.DriverRegistryOption;
import it.unibo.fantasyf1.model.EditionStatus;
import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.EnrolledConstructorOption;
import it.unibo.fantasyf1.model.GrandPrixOption;
import it.unibo.fantasyf1.model.RaceWeekend;
import it.unibo.fantasyf1.model.dao.AdminDao;
import it.unibo.fantasyf1.model.dao.EdizioneDao;
import it.unibo.fantasyf1.model.dao.ResultDao;
import it.unibo.fantasyf1.model.dao.TeamDao;
import it.unibo.fantasyf1.model.database.TransactionManager;
import it.unibo.fantasyf1.validation.InputValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A1–A8. Questa classe è usata soltanto dalla modalità amministratore trusted.
 */
public final class AdminService {

    private final TransactionManager transactions;
    private final AdminDao admin;
    private final EdizioneDao editions;
    private final TeamDao teams;
    private final ResultDao results;
    private final WeekendProcessingService processing;
    private final Clock clock;

    public AdminService(
        final TransactionManager transactions,
        final AdminDao admin,
        final EdizioneDao editions,
        final TeamDao teams,
        final ResultDao results,
        final WeekendProcessingService processing,
        final Clock clock
    ) {
        this.transactions = Objects.requireNonNull(transactions);
        this.admin = Objects.requireNonNull(admin);
        this.editions = Objects.requireNonNull(editions);
        this.teams = Objects.requireNonNull(teams);
        this.results = Objects.requireNonNull(results);
        this.processing = Objects.requireNonNull(processing);
        this.clock = Objects.requireNonNull(clock);
    }

    public int createEdition(final int number, final int year) {
        InputValidator.intRange(
            number,
            1,
            10_000,
            "Il numero dell'edizione"
        );
        InputValidator.intRange(year, 1950, 2200, "L'anno");
        return transactions.executeInTransaction(
            connection -> admin.insertEdition(connection, number, year)
        );
    }

    public int upsertGrandPrix(
        final String nameValue,
        final String circuitValue,
        final String countryValue,
        final String cityValue
    ) {
        final String name = text(nameValue, "Il nome", 100);
        final String circuit = text(circuitValue, "Il circuito", 100);
        final String country = text(countryValue, "La nazione", 80);
        final String city = text(cityValue, "La città", 80);
        return transactions.executeInTransaction(
            connection -> admin.upsertGrandPrix(
            connection,
            name,
            circuit,
            country,
            city
        ));
    }

    public int createGrandPrix(
        final String nameValue,
        final String circuitValue,
        final String countryValue,
        final String cityValue
    ) {
        final GrandPrixData data = grandPrixData(
            nameValue,
            circuitValue,
            countryValue,
            cityValue
        );
        return transactions.executeInTransaction(
            connection -> admin.insertGrandPrix(
                connection,
                data.name(),
                data.circuit(),
                data.country(),
                data.city()
            )
        );
    }

    public void updateGrandPrix(
        final int grandPrixId,
        final String nameValue,
        final String circuitValue,
        final String countryValue,
        final String cityValue
    ) {
        if (grandPrixId <= 0) {
            throw ServiceGuards.invalid(
                "Seleziona un Gran Premio da aggiornare."
            );
        }
        final GrandPrixData data = grandPrixData(
            nameValue,
            circuitValue,
            countryValue,
            cityValue
        );
        transactions.inTransaction(connection -> {
            admin.updateGrandPrix(
                connection,
                grandPrixId,
                data.name(),
                data.circuit(),
                data.country(),
                data.city()
            );
        });
    }

    public void addWeekend(
        final int editionId,
        final int grandPrixId,
        final int round,
        final LocalDate startDate,
        final LocalDate endDate
    ) {
        InputValidator.intRange(round, 1, 24, "Il round");
        InputValidator.dateRange(startDate, endDate);
        transactions.inTransaction(connection -> {
            requireLockedEdition(connection, editionId);
            if (!admin.isGrandPrixPresent(connection, grandPrixId)) {
                throw ServiceGuards.notFound("Gran Premio non trovato.");
            }
            if (admin.countWeekends(connection, editionId) >= 24) {
                throw ServiceGuards.conflict(
                    "L'edizione contiene già il massimo di 24 weekend."
                );
            }
            admin.insertWeekend(
                connection,
                editionId,
                grandPrixId,
                round,
                startDate,
                endDate
            );
        });
    }

    public int createConstructor(final String nameValue) {
        final String name = text(
            nameValue,
            "Il nome della scuderia",
            100
        );
        return transactions.executeInTransaction(
            connection -> admin.insertConstructor(connection, name)
        );
    }

    public void enrollConstructor(
        final int editionId,
        final int constructorId,
        final String registeredNameValue,
        final String carNameValue
    ) {
        final String registeredName = text(
            registeredNameValue,
            "Il nome d'iscrizione",
            100
        );
        final String carName = text(
            carNameValue,
            "Il nome della vettura",
            100
        );
        transactions.inTransaction(connection -> {
            requireLockedEdition(connection, editionId);
            if (!admin.isConstructorPresent(connection, constructorId)) {
                throw ServiceGuards.notFound("Scuderia non trovata.");
            }
            if (admin.countConstructors(connection, editionId) >= 10) {
                throw ServiceGuards.conflict(
                    "L'edizione contiene già il massimo di 10 scuderie."
                );
            }
            admin.enrollConstructor(
                connection,
                editionId,
                constructorId,
                registeredName,
                carName
            );
        });
    }

    public int createDriver(
        final String firstNameValue,
        final String lastNameValue,
        final String nationalityValue,
        final LocalDate birthDate
    ) {
        final String firstName = text(firstNameValue, "Il nome", 50);
        final String lastName = text(lastNameValue, "Il cognome", 50);
        final String nationality = text(
            nationalityValue,
            "La nazionalità",
            50
        );
        InputValidator.required(birthDate, "La data di nascita");
        if (birthDate.isAfter(LocalDate.now(clock))) {
            throw ServiceGuards.invalid(
                "La data di nascita non può essere futura."
            );
        }
        return transactions.executeInTransaction(
            connection -> admin.insertDriver(
            connection,
            firstName,
            lastName,
            nationality,
            birthDate
        ));
    }

    public void enrollDriver(
        final int editionId,
        final int driverId,
        final String codeValue,
        final int raceNumber,
        final int constructorId
    ) {
        final String code = InputValidator.exact(
            codeValue,
            3,
            "La sigla di gara"
        ).toUpperCase(Locale.ROOT);
        if (!code.chars().allMatch(Character::isLetter)) {
            throw ServiceGuards.invalid(
                "La sigla di gara deve contenere tre lettere."
            );
        }
        InputValidator.intRange(
            raceNumber,
            0,
            999,
            "Il numero di gara"
        );
        transactions.inTransaction(connection -> {
            requireLockedEdition(connection, editionId);
            if (!admin.isDriverPresent(connection, driverId)) {
                throw ServiceGuards.notFound("Pilota non trovato.");
            }
            if (!admin.isConstructorEnrolled(
                connection,
                editionId,
                constructorId
            )) {
                throw ServiceGuards.invalid(
                    "La scuderia deve essere iscritta alla stessa edizione."
                );
            }
            if (admin.countDrivers(connection, editionId) >= 20) {
                throw ServiceGuards.conflict(
                    "L'edizione contiene già il massimo di 20 piloti."
                );
            }
            if (
                admin.countDriversForConstructor(
                    connection,
                    editionId,
                    constructorId
                ) >= 2
            ) {
                throw ServiceGuards.conflict(
                    "La scuderia ha già due piloti iscritti."
                );
            }
            admin.enrollDriver(
                connection,
                editionId,
                driverId,
                code,
                raceNumber,
                constructorId
            );

            // L'aggiunta di un pilota rende non definitivi i weekend per cui
            // manca la nuova prestazione: rimuove O2 e riallinea O3.
            final LocalDate today = LocalDate.now(clock);
            for (int grandPrixId :
                results.findEndedWeekendIds(connection, editionId, today)) {
                if (results.isProcessable(
                    connection,
                    editionId,
                    grandPrixId,
                    today
                )) {
                    results.recalculateWeekendResults(
                        connection,
                        editionId,
                        grandPrixId
                    );
                } else {
                    results.clearWeekendResults(
                        connection,
                        editionId,
                        grandPrixId
                    );
                }
            }
            results.recalculateEditionTotals(connection, editionId);
        });
    }

    public ProcessingOutcome recordPerformance(
        final PerformanceRequest request
    ) {
        return processing.recordPerformance(request);
    }

    public boolean processWeekend(
        final int editionId,
        final int grandPrixId
    ) {
        return processing.processWeekend(editionId, grandPrixId);
    }

    public List<Edizione> editions() {
        return transactions.query(editions::findAll);
    }

    public List<GrandPrixOption> grandPrix() {
        return transactions.query(admin::findGrandPrix);
    }

    public List<ConstructorOption> constructors() {
        return transactions.query(admin::findConstructors);
    }

    public List<EnrolledConstructorOption> enrolledConstructors(
        final int editionId
    ) {
        return transactions.query(
            connection -> admin.findEnrolledConstructors(
                connection,
                editionId
            )
        );
    }

    public List<DriverRegistryOption> drivers() {
        return transactions.query(admin::findDrivers);
    }

    public List<DriverOption> enrolledDrivers(final int editionId) {
        return transactions.query(
            connection -> teams.findDriversByEdition(connection, editionId)
        );
    }

    public List<RaceWeekend> weekends(final int editionId) {
        return transactions.query(
            connection -> admin.findWeekends(connection, editionId)
        );
    }

    public EditionStatus editionStatus(final int editionId) {
        return transactions.query(
            connection -> admin.editionStatus(connection, editionId)
        );
    }

    private void requireLockedEdition(
        final java.sql.Connection connection,
        final int editionId
    ) throws java.sql.SQLException {
        if (editionId <= 0 || !admin.lockEdition(connection, editionId)) {
            throw ServiceGuards.notFound("Edizione non trovata.");
        }
    }

    private static String text(
        final String value,
        final String label,
        final int maximum
    ) {
        return InputValidator.max(
            InputValidator.required(value, label),
            maximum,
            label
        );
    }

    private static GrandPrixData grandPrixData(
        final String name,
        final String circuit,
        final String country,
        final String city
    ) {
        return new GrandPrixData(
            text(name, "Il nome", 100),
            text(circuit, "Il circuito", 100),
            text(country, "La nazione", 80),
            text(city, "La città", 80)
        );
    }

    private record GrandPrixData(
        String name,
        String circuit,
        String country,
        String city
    ) {
    }
}
