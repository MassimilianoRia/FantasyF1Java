USE fantasy_f1;

SET NAMES utf8mb4;

START TRANSACTION;

-- EDIZIONE
INSERT INTO EDIZIONE (NumeroEdizione, Anno)
VALUES (1, 2025);
SET @id_edizione = LAST_INSERT_ID();

-- GRAN_PREMIO
INSERT INTO GRAN_PREMIO (Nome, Circuito, Nazione, `Città`)
VALUES ('Gran Premio d''Italia', 'Autodromo Nazionale Monza', 'Italia', 'Monza');
SET @id_gran_premio = LAST_INSERT_ID();

-- PILOTA
INSERT INTO PILOTA (Nome, Cognome, `Nazionalità`, DataNascita)
VALUES ('Charles', 'Leclerc', 'Monegasca', '1997-10-16');
SET @id_pilota_leclerc = LAST_INSERT_ID();

INSERT INTO PILOTA (Nome, Cognome, `Nazionalità`, DataNascita)
VALUES ('Lewis', 'Hamilton', 'Britannica', '1985-01-07');
SET @id_pilota_hamilton = LAST_INSERT_ID();

INSERT INTO PILOTA (Nome, Cognome, `Nazionalità`, DataNascita)
VALUES ('Lando', 'Norris', 'Britannica', '1999-11-13');
SET @id_pilota_norris = LAST_INSERT_ID();

INSERT INTO PILOTA (Nome, Cognome, `Nazionalità`, DataNascita)
VALUES ('Oscar', 'Piastri', 'Australiana', '2001-04-06');
SET @id_pilota_piastri = LAST_INSERT_ID();

-- SCUDERIA
INSERT INTO SCUDERIA (Nome)
VALUES ('Scuderia Ferrari');
SET @id_scuderia_ferrari = LAST_INSERT_ID();

INSERT INTO SCUDERIA (Nome)
VALUES ('McLaren Racing');
SET @id_scuderia_mclaren = LAST_INSERT_ID();

-- UTENTE
-- Le password dimostrative sono memorizzate come hash SHA-256, non in chiaro.
INSERT INTO UTENTE (Nome, Cognome, Username, PasswordHash, Email, Telefono)
VALUES (
    'Mario',
    'Rossi',
    'mario.rossi',
    SHA2('demo-mario-2025', 256),
    'mario.rossi@example.test',
    '+39 333 0000001'
);
SET @id_utente_mario = LAST_INSERT_ID();

INSERT INTO UTENTE (Nome, Cognome, Username, PasswordHash, Email, Telefono)
VALUES (
    'Giulia',
    'Bianchi',
    'giulia.bianchi',
    SHA2('demo-giulia-2025', 256),
    'giulia.bianchi@example.test',
    '+39 333 0000002'
);
SET @id_utente_giulia = LAST_INSERT_ID();

-- WEEKEND_DI_GARA
INSERT INTO WEEKEND_DI_GARA (
    IdEdizione,
    IdGranPremio,
    NumeroRound,
    DataInizio,
    DataFine
)
VALUES (@id_edizione, @id_gran_premio, 1, '2025-09-05', '2025-09-07');

-- SCUDERIA_ISCRITTA
INSERT INTO SCUDERIA_ISCRITTA (
    IdEdizione,
    IdScuderia,
    NomeIscrizione,
    NomeVettura
)
VALUES (
    @id_edizione,
    @id_scuderia_ferrari,
    'Scuderia Ferrari HP',
    'SF-25'
);

INSERT INTO SCUDERIA_ISCRITTA (
    IdEdizione,
    IdScuderia,
    NomeIscrizione,
    NomeVettura
)
VALUES (
    @id_edizione,
    @id_scuderia_mclaren,
    'McLaren Formula 1 Team',
    'MCL39'
);

-- TEAM_FANTASY
INSERT INTO TEAM_FANTASY (Nome, PunteggioTotale, IdUtente, IdEdizione)
VALUES ('Pole Position Club', 70, @id_utente_mario, @id_edizione);
SET @id_team_mario = LAST_INSERT_ID();

INSERT INTO TEAM_FANTASY (Nome, PunteggioTotale, IdUtente, IdEdizione)
VALUES ('Box Box Racing', 0, @id_utente_giulia, @id_edizione);
SET @id_team_giulia = LAST_INSERT_ID();

-- LEGA
INSERT INTO LEGA (Nome, IdUtente, IdEdizione)
VALUES ('Lega di prova', @id_utente_mario, @id_edizione);
SET @id_lega = LAST_INSERT_ID();

-- PILOTA_ISCRITTO
INSERT INTO PILOTA_ISCRITTO (
    IdEdizione,
    IdPilota,
    SiglaGara,
    NumeroInGara,
    IdScuderia
)
VALUES (
    @id_edizione,
    @id_pilota_leclerc,
    'LEC',
    16,
    @id_scuderia_ferrari
);

INSERT INTO PILOTA_ISCRITTO (
    IdEdizione,
    IdPilota,
    SiglaGara,
    NumeroInGara,
    IdScuderia
)
VALUES (
    @id_edizione,
    @id_pilota_hamilton,
    'HAM',
    44,
    @id_scuderia_ferrari
);

INSERT INTO PILOTA_ISCRITTO (
    IdEdizione,
    IdPilota,
    SiglaGara,
    NumeroInGara,
    IdScuderia
)
VALUES (
    @id_edizione,
    @id_pilota_norris,
    'NOR',
    4,
    @id_scuderia_mclaren
);

INSERT INTO PILOTA_ISCRITTO (
    IdEdizione,
    IdPilota,
    SiglaGara,
    NumeroInGara,
    IdScuderia
)
VALUES (
    @id_edizione,
    @id_pilota_piastri,
    'PIA',
    81,
    @id_scuderia_mclaren
);

-- COMPOSIZIONE_TEAM: il primo team contiene tutti e quattro i piloti.
INSERT INTO COMPOSIZIONE_TEAM (IdEdizione, IdPilota, IdTeam)
VALUES
    (@id_edizione, @id_pilota_leclerc, @id_team_mario),
    (@id_edizione, @id_pilota_hamilton, @id_team_mario),
    (@id_edizione, @id_pilota_norris, @id_team_mario),
    (@id_edizione, @id_pilota_piastri, @id_team_mario);

-- PRESTAZIONE_WEEKEND
INSERT INTO PRESTAZIONE_WEEKEND (
    IdGranPremio,
    IdEdizione,
    IdPilota,
    PosizionamentoQualifica,
    PosizionamentoGara,
    Penalizzato,
    RegistraGiroVeloce,
    PunteggioFantasy
)
VALUES
    (@id_gran_premio, @id_edizione, @id_pilota_leclerc, 1, 1, FALSE, TRUE, 25),
    (@id_gran_premio, @id_edizione, @id_pilota_norris, 2, 2, FALSE, FALSE, 18),
    (@id_gran_premio, @id_edizione, @id_pilota_piastri, 3, 3, FALSE, FALSE, 15),
    (@id_gran_premio, @id_edizione, @id_pilota_hamilton, 4, 4, FALSE, FALSE, 12);

-- PARTECIPAZIONE_TEAM: entrambi i team partecipano alla lega.
INSERT INTO PARTECIPAZIONE_TEAM (IdLega, IdTeam)
VALUES
    (@id_lega, @id_team_mario),
    (@id_lega, @id_team_giulia);

-- RISULTATO_TEAM
INSERT INTO RISULTATO_TEAM (
    IdEdizione,
    IdGranPremio,
    IdTeam,
    PunteggioWeekend
)
VALUES
    (@id_edizione, @id_gran_premio, @id_team_mario, 70),
    (@id_edizione, @id_gran_premio, @id_team_giulia, 0);

COMMIT;

-- Controllo rapido: restituisce le quantità inserite dal seed.
SELECT
    (SELECT COUNT(*) FROM EDIZIONE) AS Edizioni,
    (SELECT COUNT(*) FROM SCUDERIA) AS Scuderie,
    (SELECT COUNT(*) FROM PILOTA) AS Piloti,
    (SELECT COUNT(*) FROM SCUDERIA_ISCRITTA) AS ScuderieIscritte,
    (SELECT COUNT(*) FROM PILOTA_ISCRITTO) AS PilotiIscritti,
    (SELECT COUNT(*) FROM GRAN_PREMIO) AS GranPremi,
    (SELECT COUNT(*) FROM WEEKEND_DI_GARA) AS WeekendDiGara,
    (SELECT COUNT(*) FROM UTENTE) AS Utenti,
    (SELECT COUNT(*) FROM TEAM_FANTASY) AS TeamFantasy,
    (SELECT COUNT(*) FROM LEGA) AS Leghe,
    (SELECT COUNT(*) FROM COMPOSIZIONE_TEAM) AS PilotiNelPrimoTeam,
    (SELECT COUNT(*) FROM PARTECIPAZIONE_TEAM) AS Partecipazioni,
    (SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND) AS Prestazioni,
    (SELECT COUNT(*) FROM RISULTATO_TEAM) AS RisultatiTeam;
