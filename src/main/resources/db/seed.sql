-- Popolamento completo per test funzionali e di volume.
--
-- Lo script rispecchia la stima della relazione:
--   5 edizioni, 40 piloti, 98 iscrizioni pilota,
--   15 scuderie, 49 iscrizioni scuderia,
--   30 Gran Premi, 118 weekend,
--   1001 utenti, 1502 team, 201 leghe,
--   6008 componenti, 1501 partecipazioni,
--   2280 prestazioni e 34840 risultati team.
--
-- Nomi di piloti, scuderie, Gran Premi e circuiti sono usati per rendere
-- leggibile il dataset. Calendari, schieramenti e risultati sono invece
-- sintetici e deterministici: non rappresentano classifiche sportive reali.
--
-- ATTENZIONE: questo script è distruttivo. A ogni esecuzione cancella tutti
-- i dati presenti in fantasy_f1, azzera gli AUTO_INCREMENT e ripopola il
-- database. Eseguire schema.sql almeno una volta per creare la struttura.

USE fantasy_f1;

SET NAMES utf8mb4;

-- Ripristino completo dei dati. FOREIGN_KEY_CHECKS viene disabilitato soltanto
-- durante i TRUNCATE e ripristinato prima di iniziare il popolamento.
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE RISULTATO_TEAM;
TRUNCATE TABLE PARTECIPAZIONE_TEAM;
TRUNCATE TABLE PRESTAZIONE_WEEKEND;
TRUNCATE TABLE COMPOSIZIONE_TEAM;
TRUNCATE TABLE PILOTA_ISCRITTO;
TRUNCATE TABLE LEGA;
TRUNCATE TABLE TEAM_FANTASY;
TRUNCATE TABLE SCUDERIA_ISCRITTA;
TRUNCATE TABLE WEEKEND_DI_GARA;
TRUNCATE TABLE UTENTE;
TRUNCATE TABLE SCUDERIA;
TRUNCATE TABLE PILOTA;
TRUNCATE TABLE GRAN_PREMIO;
TRUNCATE TABLE EDIZIONE;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- Tabella temporanea 1..1500, riutilizzata per generare i dati di volume senza
-- dipendere da procedure memorizzate o dal limite di ricorsione delle CTE.
DROP TEMPORARY TABLE IF EXISTS _SEED_NUMERO;
DROP TEMPORARY TABLE IF EXISTS _SEED_NOME;
DROP TEMPORARY TABLE IF EXISTS _SEED_COGNOME;
DROP TEMPORARY TABLE IF EXISTS _SEED_POSIZIONE_1;
DROP TEMPORARY TABLE IF EXISTS _SEED_POSIZIONE_2;
DROP TEMPORARY TABLE IF EXISTS _SEED_POSIZIONE_3;
DROP TEMPORARY TABLE IF EXISTS _SEED_POSIZIONE_4;
DROP TEMPORARY TABLE IF EXISTS _SEED_ROSA;
CREATE TEMPORARY TABLE _SEED_NUMERO (
    N INT UNSIGNED NOT NULL,
    CONSTRAINT PK_SEED_NUMERO PRIMARY KEY (N)
);

INSERT INTO _SEED_NUMERO (N)
SELECT
    U.N + 10 * D.N + 100 * C.N + 1000 * M.N + 1
FROM (
    SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL
    SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL
    SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) AS U
CROSS JOIN (
    SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL
    SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL
    SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) AS D
CROSS JOIN (
    SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL
    SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL
    SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) AS C
CROSS JOIN (
    SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL
    SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL
    SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) AS M
WHERE U.N + 10 * D.N + 100 * C.N + 1000 * M.N < 1500;

-- MySQL non consente di riaprire più volte la stessa tabella temporanea
-- all'interno di una query. Queste quattro copie 1..20 servono a enumerare
-- le combinazioni distinte di piloti usate per le rose.
CREATE TEMPORARY TABLE _SEED_POSIZIONE_1 (
    N INT UNSIGNED NOT NULL PRIMARY KEY
);
CREATE TEMPORARY TABLE _SEED_POSIZIONE_2 (
    N INT UNSIGNED NOT NULL PRIMARY KEY
);
CREATE TEMPORARY TABLE _SEED_POSIZIONE_3 (
    N INT UNSIGNED NOT NULL PRIMARY KEY
);
CREATE TEMPORARY TABLE _SEED_POSIZIONE_4 (
    N INT UNSIGNED NOT NULL PRIMARY KEY
);
INSERT INTO _SEED_POSIZIONE_1 SELECT N FROM _SEED_NUMERO WHERE N <= 20;
INSERT INTO _SEED_POSIZIONE_2 SELECT N FROM _SEED_NUMERO WHERE N <= 20;
INSERT INTO _SEED_POSIZIONE_3 SELECT N FROM _SEED_NUMERO WHERE N <= 20;
INSERT INTO _SEED_POSIZIONE_4 SELECT N FROM _SEED_NUMERO WHERE N <= 20;

-- Vocabolari anagrafici: il prodotto cartesiano 25 x 40 genera mille utenti
-- uniformi, leggibili e tutti distinti.
CREATE TEMPORARY TABLE _SEED_NOME (
    IdNome INT UNSIGNED NOT NULL,
    Valore VARCHAR(50) NOT NULL,
    Chiave VARCHAR(50) NOT NULL,
    CONSTRAINT PK_SEED_NOME PRIMARY KEY (IdNome),
    CONSTRAINT UQ_SEED_NOME_CHIAVE UNIQUE (Chiave)
);

INSERT INTO _SEED_NOME (IdNome, Valore, Chiave)
VALUES
    (1, 'Alessandro', 'alessandro'),
    (2, 'Beatrice', 'beatrice'),
    (3, 'Carlo', 'carlo'),
    (4, 'Diana', 'diana'),
    (5, 'Edoardo', 'edoardo'),
    (6, 'Federica', 'federica'),
    (7, 'Gabriele', 'gabriele'),
    (8, 'Helena', 'helena'),
    (9, 'Ivan', 'ivan'),
    (10, 'Jessica', 'jessica'),
    (11, 'Lorenzo', 'lorenzo'),
    (12, 'Martina', 'martina'),
    (13, 'Nicolò', 'nicolo'),
    (14, 'Olivia', 'olivia'),
    (15, 'Paolo', 'paolo'),
    (16, 'Rebecca', 'rebecca'),
    (17, 'Samuele', 'samuele'),
    (18, 'Teresa', 'teresa'),
    (19, 'Umberto', 'umberto'),
    (20, 'Valentina', 'valentina'),
    (21, 'Andrea', 'andrea'),
    (22, 'Chiara', 'chiara'),
    (23, 'Matteo', 'matteo'),
    (24, 'Sofia', 'sofia'),
    (25, 'Luca', 'luca');

CREATE TEMPORARY TABLE _SEED_COGNOME (
    IdCognome INT UNSIGNED NOT NULL,
    Valore VARCHAR(50) NOT NULL,
    Chiave VARCHAR(50) NOT NULL,
    CONSTRAINT PK_SEED_COGNOME PRIMARY KEY (IdCognome),
    CONSTRAINT UQ_SEED_COGNOME_CHIAVE UNIQUE (Chiave)
);

INSERT INTO _SEED_COGNOME (IdCognome, Valore, Chiave)
VALUES
    (1, 'Rossi', 'rossi'),
    (2, 'Bianchi', 'bianchi'),
    (3, 'Romano', 'romano'),
    (4, 'Colombo', 'colombo'),
    (5, 'Ricci', 'ricci'),
    (6, 'Marino', 'marino'),
    (7, 'Greco', 'greco'),
    (8, 'Bruno', 'bruno'),
    (9, 'Gallo', 'gallo'),
    (10, 'Conti', 'conti'),
    (11, 'De Luca', 'deluca'),
    (12, 'Mancini', 'mancini'),
    (13, 'Costa', 'costa'),
    (14, 'Giordano', 'giordano'),
    (15, 'Rizzo', 'rizzo'),
    (16, 'Lombardi', 'lombardi'),
    (17, 'Moretti', 'moretti'),
    (18, 'Barbieri', 'barbieri'),
    (19, 'Fontana', 'fontana'),
    (20, 'Santoro', 'santoro'),
    (21, 'Mariani', 'mariani'),
    (22, 'Rinaldi', 'rinaldi'),
    (23, 'Caruso', 'caruso'),
    (24, 'Ferrara', 'ferrara'),
    (25, 'Galli', 'galli'),
    (26, 'Martini', 'martini'),
    (27, 'Leone', 'leone'),
    (28, 'Longo', 'longo'),
    (29, 'Gentile', 'gentile'),
    (30, 'Martinelli', 'martinelli'),
    (31, 'Vitale', 'vitale'),
    (32, 'Serra', 'serra'),
    (33, 'Coppola', 'coppola'),
    (34, 'De Santis', 'desantis'),
    (35, 'D''Angelo', 'dangelo'),
    (36, 'Marchetti', 'marchetti'),
    (37, 'Parisi', 'parisi'),
    (38, 'Villa', 'villa'),
    (39, 'Conte', 'conte'),
    (40, 'Ferri', 'ferri');

START TRANSACTION;

-- Le edizioni 2021-2024 sono complete. Il 2025 resta intenzionalmente
-- incompleto per provare il popolamento progressivo dalla modalità admin.
INSERT INTO EDIZIONE (IdEdizione, NumeroEdizione, Anno)
VALUES
    (1, 1, 2021),
    (2, 2, 2022),
    (3, 3, 2023),
    (4, 4, 2024),
    (5, 5, 2025);

-- Trenta Gran Premi anagrafici. Ogni edizione ne usa 24 e condivide gran
-- parte del calendario con le edizioni adiacenti.
INSERT INTO GRAN_PREMIO (
    IdGranPremio,
    Nome,
    Circuito,
    Nazione,
    `Città`
)
VALUES
    (1, 'Gran Premio del Bahrain', 'Bahrain International Circuit', 'Bahrain', 'Sakhir'),
    (2, 'Gran Premio dell''Arabia Saudita', 'Jeddah Corniche Circuit', 'Arabia Saudita', 'Gedda'),
    (3, 'Gran Premio d''Australia', 'Albert Park Circuit', 'Australia', 'Melbourne'),
    (4, 'Gran Premio del Giappone', 'Suzuka International Racing Course', 'Giappone', 'Suzuka'),
    (5, 'Gran Premio di Cina', 'Shanghai International Circuit', 'Cina', 'Shanghai'),
    (6, 'Gran Premio di Miami', 'Miami International Autodrome', 'Stati Uniti', 'Miami'),
    (7, 'Gran Premio dell''Emilia-Romagna', 'Autodromo Enzo e Dino Ferrari', 'Italia', 'Imola'),
    (8, 'Gran Premio di Monaco', 'Circuit de Monaco', 'Monaco', 'Monte Carlo'),
    (9, 'Gran Premio del Canada', 'Circuit Gilles Villeneuve', 'Canada', 'Montréal'),
    (10, 'Gran Premio di Spagna', 'Circuit de Barcelona-Catalunya', 'Spagna', 'Montmeló'),
    (11, 'Gran Premio d''Austria', 'Red Bull Ring', 'Austria', 'Spielberg'),
    (12, 'Gran Premio di Gran Bretagna', 'Silverstone Circuit', 'Regno Unito', 'Silverstone'),
    (13, 'Gran Premio d''Ungheria', 'Hungaroring', 'Ungheria', 'Budapest'),
    (14, 'Gran Premio del Belgio', 'Circuit de Spa-Francorchamps', 'Belgio', 'Stavelot'),
    (15, 'Gran Premio dei Paesi Bassi', 'Circuit Zandvoort', 'Paesi Bassi', 'Zandvoort'),
    (16, 'Gran Premio d''Italia', 'Autodromo Nazionale Monza', 'Italia', 'Monza'),
    (17, 'Gran Premio dell''Azerbaigian', 'Baku City Circuit', 'Azerbaigian', 'Baku'),
    (18, 'Gran Premio di Singapore', 'Marina Bay Street Circuit', 'Singapore', 'Singapore'),
    (19, 'Gran Premio degli Stati Uniti', 'Circuit of the Americas', 'Stati Uniti', 'Austin'),
    (20, 'Gran Premio di Città del Messico', 'Autódromo Hermanos Rodríguez', 'Messico', 'Città del Messico'),
    (21, 'Gran Premio di San Paolo', 'Autódromo José Carlos Pace', 'Brasile', 'San Paolo'),
    (22, 'Gran Premio di Las Vegas', 'Las Vegas Strip Circuit', 'Stati Uniti', 'Las Vegas'),
    (23, 'Gran Premio del Qatar', 'Lusail International Circuit', 'Qatar', 'Lusail'),
    (24, 'Gran Premio di Abu Dhabi', 'Yas Marina Circuit', 'Emirati Arabi Uniti', 'Abu Dhabi'),
    (25, 'Gran Premio di Francia', 'Circuit Paul Ricard', 'Francia', 'Le Castellet'),
    (26, 'Gran Premio di Germania', 'Hockenheimring', 'Germania', 'Hockenheim'),
    (27, 'Gran Premio di Turchia', 'Istanbul Park', 'Turchia', 'Istanbul'),
    (28, 'Gran Premio di Russia', 'Sochi Autodrom', 'Russia', 'Sochi'),
    (29, 'Gran Premio della Malesia', 'Sepang International Circuit', 'Malesia', 'Sepang'),
    (30, 'Gran Premio di Corea', 'Korea International Circuit', 'Corea del Sud', 'Yeongam');

-- Quaranta piloti anagrafici. Le finestre stagionali sovrapposte usate più
-- avanti fanno sì che molti piloti partecipino a più edizioni, mentre altri
-- entrano o escono dal campionato.
INSERT INTO PILOTA (
    IdPilota,
    Nome,
    Cognome,
    `Nazionalità`,
    DataNascita
)
VALUES
    (1, 'Max', 'Verstappen', 'Olandese', '1997-09-30'),
    (2, 'Sergio', 'Pérez', 'Messicana', '1990-01-26'),
    (3, 'Lewis', 'Hamilton', 'Britannica', '1985-01-07'),
    (4, 'George', 'Russell', 'Britannica', '1998-02-15'),
    (5, 'Charles', 'Leclerc', 'Monegasca', '1997-10-16'),
    (6, 'Carlos', 'Sainz', 'Spagnola', '1994-09-01'),
    (7, 'Lando', 'Norris', 'Britannica', '1999-11-13'),
    (8, 'Oscar', 'Piastri', 'Australiana', '2001-04-06'),
    (9, 'Fernando', 'Alonso', 'Spagnola', '1981-07-29'),
    (10, 'Lance', 'Stroll', 'Canadese', '1998-10-29'),
    (11, 'Pierre', 'Gasly', 'Francese', '1996-02-07'),
    (12, 'Esteban', 'Ocon', 'Francese', '1996-09-17'),
    (13, 'Alexander', 'Albon', 'Thailandese', '1996-03-23'),
    (14, 'Logan', 'Sargeant', 'Statunitense', '2000-12-31'),
    (15, 'Yuki', 'Tsunoda', 'Giapponese', '2000-05-11'),
    (16, 'Daniel', 'Ricciardo', 'Australiana', '1989-07-01'),
    (17, 'Valtteri', 'Bottas', 'Finlandese', '1989-08-28'),
    (18, 'Guanyu', 'Zhou', 'Cinese', '1999-05-30'),
    (19, 'Kevin', 'Magnussen', 'Danese', '1992-10-05'),
    (20, 'Nico', 'Hülkenberg', 'Tedesca', '1987-08-19'),
    (21, 'Sebastian', 'Vettel', 'Tedesca', '1987-07-03'),
    (22, 'Kimi', 'Räikkönen', 'Finlandese', '1979-10-17'),
    (23, 'Antonio', 'Giovinazzi', 'Italiana', '1993-12-14'),
    (24, 'Nicholas', 'Latifi', 'Canadese', '1995-06-29'),
    (25, 'Mick', 'Schumacher', 'Tedesca', '1999-03-22'),
    (26, 'Nikita', 'Mazepin', 'Russa', '1999-03-02'),
    (27, 'Nyck', 'de Vries', 'Olandese', '1995-02-06'),
    (28, 'Liam', 'Lawson', 'Neozelandese', '2002-02-11'),
    (29, 'Franco', 'Colapinto', 'Argentina', '2003-05-27'),
    (30, 'Oliver', 'Bearman', 'Britannica', '2005-05-08'),
    (31, 'Jack', 'Doohan', 'Australiana', '2003-01-20'),
    (32, 'Andrea Kimi', 'Antonelli', 'Italiana', '2006-08-25'),
    (33, 'Isack', 'Hadjar', 'Francese', '2004-09-28'),
    (34, 'Gabriel', 'Bortoleto', 'Brasiliana', '2004-10-14'),
    (35, 'Felipe', 'Drugovich', 'Brasiliana', '2000-05-23'),
    (36, 'Théo', 'Pourchaire', 'Francese', '2003-08-20'),
    (37, 'Robert', 'Kubica', 'Polacca', '1984-12-07'),
    (38, 'Daniil', 'Kvyat', 'Russa', '1994-04-26'),
    (39, 'Romain', 'Grosjean', 'Francese', '1986-04-17'),
    (40, 'Jenson', 'Button', 'Britannica', '1980-01-19');

-- Quindici scuderie anagrafiche. Anche in questo caso le iscrizioni
-- stagionali si sovrappongono per rappresentare permanenze e avvicendamenti.
INSERT INTO SCUDERIA (IdScuderia, Nome)
VALUES
    (1, 'Red Bull Racing'),
    (2, 'Scuderia Ferrari'),
    (3, 'Mercedes Grand Prix'),
    (4, 'McLaren Racing'),
    (5, 'Aston Martin F1 Team'),
    (6, 'Alpine F1 Team'),
    (7, 'Williams Racing'),
    (8, 'Racing Bulls'),
    (9, 'Sauber Motorsport'),
    (10, 'Haas F1 Team'),
    (11, 'Renault F1 Team'),
    (12, 'Scuderia AlphaTauri'),
    (13, 'Alfa Romeo Racing'),
    (14, 'Force India'),
    (15, 'Lotus F1 Team');

-- Mille utenti ottenuti dal prodotto cartesiano dei vocabolari anagrafici.
-- Tutti seguono la medesima convenzione per username, email e password.
-- Gli hash SHA-256 legacy vengono migrati a PBKDF2 al primo login valido.
INSERT INTO UTENTE (
    IdUtente,
    Nome,
    Cognome,
    Username,
    PasswordHash,
    Email,
    Telefono
)
SELECT
    NUM.N,
    NM.Valore,
    CG.Valore,
    CONCAT(NM.Chiave, '.', CG.Chiave),
    SHA2('fantasyf1-2025', 256),
    CONCAT(NM.Chiave, '.', CG.Chiave, '@example.test'),
    CONCAT('+39 320 ', LPAD(NUM.N, 7, '0'))
FROM _SEED_NUMERO AS NUM
JOIN _SEED_NOME AS NM
    ON NM.IdNome = MOD(NUM.N - 1, 25) + 1
JOIN _SEED_COGNOME AS CG
    ON CG.IdCognome = FLOOR((NUM.N - 1) / 25) + 1
WHERE NUM.N <= 1000;

-- Account dedicato ai test manuali dell'applicazione.
-- Credenziali: username "max", password "database".
INSERT INTO UTENTE (
    IdUtente,
    Nome,
    Cognome,
    Username,
    PasswordHash,
    Email,
    Telefono
)
VALUES (
    1001,
    'Massimiliano',
    'Ria',
    'max',
    SHA2('database', 256),
    'massi.ria23@gmail.com',
    '3476334034'
);

-- Ventiquattro weekend per le edizioni concluse e ventidue per il 2025.
-- Risalendo nel tempo la finestra si sposta di tre GP per stagione.
INSERT INTO WEEKEND_DI_GARA (
    IdEdizione,
    IdGranPremio,
    NumeroRound,
    DataInizio,
    DataFine
)
SELECT
    E.IdEdizione,
    G.IdGranPremio,
    NUM.N,
    DATE_ADD(
        STR_TO_DATE(CONCAT(E.Anno, '-03-01'), '%Y-%m-%d'),
        INTERVAL ((NUM.N - 1) * 12) DAY
    ),
    DATE_ADD(
        STR_TO_DATE(CONCAT(E.Anno, '-03-01'), '%Y-%m-%d'),
        INTERVAL ((NUM.N - 1) * 12 + 2) DAY
    )
FROM EDIZIONE AS E
JOIN _SEED_NUMERO AS NUM
    ON NUM.N <= CASE WHEN E.IdEdizione = 5 THEN 22 ELSE 24 END
JOIN GRAN_PREMIO AS G
    ON G.IdGranPremio =
        MOD((5 - E.IdEdizione) * 3 + NUM.N - 1, 30) + 1;

-- Dieci scuderie nelle edizioni concluse e nove nel 2025.
INSERT INTO SCUDERIA_ISCRITTA (
    IdEdizione,
    IdScuderia,
    NomeIscrizione,
    NomeVettura
)
SELECT
    E.IdEdizione,
    S.IdScuderia,
    CONCAT(S.Nome, ' ', E.Anno),
    CONCAT('F1-', E.Anno, '-', LPAD(S.IdScuderia, 2, '0'))
FROM EDIZIONE AS E
JOIN _SEED_NUMERO AS NUM
    ON NUM.N <= CASE WHEN E.IdEdizione = 5 THEN 9 ELSE 10 END
JOIN SCUDERIA AS S
    ON S.IdScuderia =
        MOD((5 - E.IdEdizione) * 2 + NUM.N - 1, 15) + 1;

-- Venti piloti per edizione conclusa e diciotto nel 2025, sempre due per
-- ciascuna scuderia iscritta.
-- Le finestre adiacenti condividono 15 piloti: la ricorrenza è intenzionale
-- e permette di verificare correttamente lo storico.
INSERT INTO PILOTA_ISCRITTO (
    IdEdizione,
    IdPilota,
    SiglaGara,
    NumeroInGara,
    IdScuderia
)
SELECT
    E.IdEdizione,
    P.IdPilota,
    CASE P.IdPilota
        WHEN 1 THEN 'VER' WHEN 2 THEN 'PER' WHEN 3 THEN 'HAM'
        WHEN 4 THEN 'RUS' WHEN 5 THEN 'LEC' WHEN 6 THEN 'SAI'
        WHEN 7 THEN 'NOR' WHEN 8 THEN 'PIA' WHEN 9 THEN 'ALO'
        WHEN 10 THEN 'STR' WHEN 11 THEN 'GAS' WHEN 12 THEN 'OCO'
        WHEN 13 THEN 'ALB' WHEN 14 THEN 'SAR' WHEN 15 THEN 'TSU'
        WHEN 16 THEN 'RIC' WHEN 17 THEN 'BOT' WHEN 18 THEN 'ZHO'
        WHEN 19 THEN 'MAG' WHEN 20 THEN 'HUL' WHEN 21 THEN 'VET'
        WHEN 22 THEN 'RAI' WHEN 23 THEN 'GIO' WHEN 24 THEN 'LAT'
        WHEN 25 THEN 'MSC' WHEN 26 THEN 'MAZ' WHEN 27 THEN 'DEV'
        WHEN 28 THEN 'LAW' WHEN 29 THEN 'COL' WHEN 30 THEN 'BEA'
        WHEN 31 THEN 'DOO' WHEN 32 THEN 'ANT' WHEN 33 THEN 'HAD'
        WHEN 34 THEN 'BOR' WHEN 35 THEN 'DRU' WHEN 36 THEN 'POU'
        WHEN 37 THEN 'KUB' WHEN 38 THEN 'KVY' WHEN 39 THEN 'GRO'
        WHEN 40 THEN 'BUT'
    END,
    P.IdPilota,
    MOD(
        (5 - E.IdEdizione) * 2 + FLOOR((NUM.N - 1) / 2),
        15
    ) + 1
FROM EDIZIONE AS E
JOIN _SEED_NUMERO AS NUM
    ON NUM.N <= CASE WHEN E.IdEdizione = 5 THEN 18 ELSE 20 END
JOIN PILOTA AS P
    ON P.IdPilota = (5 - E.IdEdizione) * 5 + NUM.N;

-- Trecento team per edizione. I nomi combinano in modo uniforme trenta
-- identità racing e dieci qualificatori; i primi 60 dei 240 proprietari
-- possiedono due team nella stessa edizione, utile per U2/U3/U6.
INSERT INTO TEAM_FANTASY (
    IdTeam,
    Nome,
    PunteggioTotale,
    IdUtente,
    IdEdizione
)
SELECT
    DATI.IdTeam,
    CONCAT(
        CASE MOD(DATI.Ordinale - 1, 30) + 1
            WHEN 1 THEN 'Apex' WHEN 2 THEN 'Vertex'
            WHEN 3 THEN 'Velocity' WHEN 4 THEN 'Titan'
            WHEN 5 THEN 'Phoenix' WHEN 6 THEN 'Falcon'
            WHEN 7 THEN 'Vortex' WHEN 8 THEN 'Quantum'
            WHEN 9 THEN 'Eclipse' WHEN 10 THEN 'Thunder'
            WHEN 11 THEN 'Crimson' WHEN 12 THEN 'Silver'
            WHEN 13 THEN 'Golden' WHEN 14 THEN 'Iron'
            WHEN 15 THEN 'Neon' WHEN 16 THEN 'Rapid'
            WHEN 17 THEN 'Stellar' WHEN 18 THEN 'Prime'
            WHEN 19 THEN 'Summit' WHEN 20 THEN 'Horizon'
            WHEN 21 THEN 'Tempest' WHEN 22 THEN 'Aurora'
            WHEN 23 THEN 'Pinnacle' WHEN 24 THEN 'Momentum'
            WHEN 25 THEN 'Ignition' WHEN 26 THEN 'Redline'
            WHEN 27 THEN 'Slipstream' WHEN 28 THEN 'Overtake'
            WHEN 29 THEN 'Chicane' WHEN 30 THEN 'Paddock'
        END,
        ' ',
        CASE FLOOR((DATI.Ordinale - 1) / 30)
            WHEN 0 THEN 'Racing'
            WHEN 1 THEN 'Motorsport'
            WHEN 2 THEN 'Performance'
            WHEN 3 THEN 'Engineering'
            WHEN 4 THEN 'Corse'
            WHEN 5 THEN 'GP'
            WHEN 6 THEN 'Competition'
            WHEN 7 THEN 'Speedworks'
            WHEN 8 THEN 'Dynamics'
            WHEN 9 THEN 'Formula'
        END
    ),
    0,
    MOD(DATI.Ordinale - 1, 240) + 1,
    E.IdEdizione
FROM (
    SELECT
        N AS IdTeam,
        MOD(N - 1, 300) + 1 AS Ordinale,
        FLOOR((N - 1) / 300) + 1 AS IdEdizione
    FROM _SEED_NUMERO
    WHERE N <= 1500
) AS DATI
JOIN EDIZIONE AS E
    ON E.IdEdizione = DATI.IdEdizione;

-- Due team espliciti dell'account manuale "max" nell'edizione 2025.
INSERT INTO TEAM_FANTASY (
    IdTeam,
    Nome,
    PunteggioTotale,
    IdUtente,
    IdEdizione
)
VALUES
    (1501, 'TeamProva1', 0, 1001, 5),
    (1502, 'TeamProva2', 0, 1001, 5);

-- Quaranta leghe per edizione, tutte nominate secondo le convenzioni
-- "Trofeo <tema>" o "Campionato <tema>" e amministrate da utenti differenti.
INSERT INTO LEGA (
    IdLega,
    Nome,
    IdUtente,
    IdEdizione
)
SELECT
    DATI.IdLega,
    CONCAT(
        CASE
            WHEN DATI.Ordinale <= 20 THEN 'Trofeo '
            ELSE 'Campionato '
        END,
        CASE MOD(DATI.Ordinale - 1, 20) + 1
            WHEN 1 THEN 'Apex' WHEN 2 THEN 'Horizon'
            WHEN 3 THEN 'Velocity' WHEN 4 THEN 'Titan'
            WHEN 5 THEN 'Phoenix' WHEN 6 THEN 'Silverstone'
            WHEN 7 THEN 'Monza' WHEN 8 THEN 'Monaco'
            WHEN 9 THEN 'Imola' WHEN 10 THEN 'Suzuka'
            WHEN 11 THEN 'Spa' WHEN 12 THEN 'Interlagos'
            WHEN 13 THEN 'Zandvoort' WHEN 14 THEN 'Marina Bay'
            WHEN 15 THEN 'Catalunya' WHEN 16 THEN 'Redline'
            WHEN 17 THEN 'Paddock' WHEN 18 THEN 'Pole'
            WHEN 19 THEN 'Chicane' WHEN 20 THEN 'Pitlane'
        END
    ),
    DATI.Ordinale,
    E.IdEdizione
FROM (
    SELECT
        N AS IdLega,
        MOD(N - 1, 40) + 1 AS Ordinale,
        FLOOR((N - 1) / 40) + 1 AS IdEdizione
    FROM _SEED_NUMERO
    WHERE N <= 200
) AS DATI
JOIN EDIZIONE AS E
    ON E.IdEdizione = DATI.IdEdizione;

-- Lega di prova amministrata dall'account manuale "max".
INSERT INTO LEGA (IdLega, Nome, IdUtente, IdEdizione)
VALUES (201, 'LegaProva1', 1001, 5);

-- Quattro piloti distinti e iscritti alla corretta edizione per ogni team.
-- Le 300 rose di volume sono combinazioni tutte diverse, ordinate tramite
-- hash deterministico: il seed è ripetibile ma evita i blocchi di team
-- identici prodotti dalla precedente rotazione di sole venti rose.
CREATE TEMPORARY TABLE _SEED_ROSA (
    IdEdizione INT UNSIGNED NOT NULL,
    Ordinale INT UNSIGNED NOT NULL,
    P1 INT UNSIGNED NOT NULL,
    P2 INT UNSIGNED NOT NULL,
    P3 INT UNSIGNED NOT NULL,
    P4 INT UNSIGNED NOT NULL,
    PRIMARY KEY (IdEdizione, Ordinale)
);

INSERT INTO _SEED_ROSA (
    IdEdizione,
    Ordinale,
    P1,
    P2,
    P3,
    P4
)
SELECT
    COMBINAZIONE.IdEdizione,
    ROW_NUMBER() OVER (
        PARTITION BY COMBINAZIONE.IdEdizione
        ORDER BY SHA2(
            CONCAT(
                COMBINAZIONE.IdEdizione, '-',
                COMBINAZIONE.P1, '-',
                COMBINAZIONE.P2, '-',
                COMBINAZIONE.P3, '-',
                COMBINAZIONE.P4, '-rosa'
            ),
            256
        )
    ) AS Ordinale,
    COMBINAZIONE.P1,
    COMBINAZIONE.P2,
    COMBINAZIONE.P3,
    COMBINAZIONE.P4
FROM (
    SELECT
        E.IdEdizione,
        P1.N AS P1,
        P2.N AS P2,
        P3.N AS P3,
        P4.N AS P4
    FROM EDIZIONE AS E
    JOIN _SEED_POSIZIONE_1 AS P1
        ON P1.N <= CASE
            WHEN E.IdEdizione = 5 THEN 18 ELSE 20
        END
    JOIN _SEED_POSIZIONE_2 AS P2
        ON P2.N > P1.N
        AND P2.N <= CASE
            WHEN E.IdEdizione = 5 THEN 18 ELSE 20
        END
    JOIN _SEED_POSIZIONE_3 AS P3
        ON P3.N > P2.N
        AND P3.N <= CASE
            WHEN E.IdEdizione = 5 THEN 18 ELSE 20
        END
    JOIN _SEED_POSIZIONE_4 AS P4
        ON P4.N > P3.N
        AND P4.N <= CASE
            WHEN E.IdEdizione = 5 THEN 18 ELSE 20
        END
) AS COMBINAZIONE;

INSERT INTO COMPOSIZIONE_TEAM (
    IdEdizione,
    IdPilota,
    IdTeam
)
SELECT
    TF.IdEdizione,
    PI.IdPilota,
    TF.IdTeam
FROM TEAM_FANTASY AS TF
JOIN _SEED_ROSA AS ROSA
    ON ROSA.IdEdizione = TF.IdEdizione
    AND ROSA.Ordinale = MOD(TF.IdTeam - 1, 300) + 1
JOIN _SEED_NUMERO AS POS
    ON POS.N <= 4
JOIN PILOTA_ISCRITTO AS PI
    ON PI.IdEdizione = TF.IdEdizione
    AND PI.IdPilota =
        (5 - TF.IdEdizione) * 5
        + CASE POS.N
            WHEN 1 THEN ROSA.P1
            WHEN 2 THEN ROSA.P2
            WHEN 3 THEN ROSA.P3
            WHEN 4 THEN ROSA.P4
        END
WHERE TF.IdTeam <= 1500;

-- Rose leggibili e differenti per i due team manuali di Max.
INSERT INTO COMPOSIZIONE_TEAM (IdEdizione, IdPilota, IdTeam)
VALUES
    (5, 1, 1501),
    (5, 5, 1501),
    (5, 9, 1501),
    (5, 13, 1501),
    (5, 2, 1502),
    (5, 6, 1502),
    (5, 10, 1502),
    (5, 14, 1502);

-- Tutte le prestazioni delle edizioni concluse. Per il 2025 sono compilati
-- solo i primi venti dei ventidue weekend presenti: gli ultimi due restano
-- senza risultati per il test manuale di A8. Qualifica e gara sono
-- permutazioni pseudocasuali ma ripetibili, così i punteggi dei piloti e
-- delle rose risultano distribuiti meglio. PunteggioFantasy applica la policy:
-- max(0, 21 - gara) + max(0, 6 - qualifica)
-- + 2 per il giro veloce - 5 in caso di penalizzazione.
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
SELECT
    DATI.IdGranPremio,
    DATI.IdEdizione,
    DATI.IdPilota,
    DATI.PosizioneQualifica,
    DATI.PosizioneGara,
    DATI.Penalizzato,
    DATI.GiroVeloce,
    GREATEST(0, 21 - CAST(DATI.PosizioneGara AS SIGNED))
        + GREATEST(0, 6 - CAST(DATI.PosizioneQualifica AS SIGNED))
        + CASE WHEN DATI.GiroVeloce THEN 2 ELSE 0 END
        - CASE WHEN DATI.Penalizzato THEN 5 ELSE 0 END
FROM (
    SELECT
        W.IdGranPremio,
        W.IdEdizione,
        PI.IdPilota,
        ROW_NUMBER() OVER (
            PARTITION BY W.IdEdizione, W.IdGranPremio
            ORDER BY SHA2(
                CONCAT(
                    W.IdEdizione, '-',
                    W.NumeroRound, '-',
                    PI.IdPilota, '-qualifica'
                ),
                256
            )
        ) AS PosizioneQualifica,
        ROW_NUMBER() OVER (
            PARTITION BY W.IdEdizione, W.IdGranPremio
            ORDER BY SHA2(
                CONCAT(
                    W.IdEdizione, '-',
                    W.NumeroRound, '-',
                    PI.IdPilota, '-gara'
                ),
                256
            )
        ) AS PosizioneGara,
        MOD(
            CRC32(CONCAT(
                W.IdEdizione, '-',
                W.NumeroRound, '-',
                PI.IdPilota, '-penalita'
            )),
            17
        ) = 0 AS Penalizzato,
        ROW_NUMBER() OVER (
            PARTITION BY W.IdEdizione, W.IdGranPremio
            ORDER BY SHA2(
                CONCAT(
                    W.IdEdizione, '-',
                    W.NumeroRound, '-',
                    PI.IdPilota, '-giro-veloce'
                ),
                256
            )
        ) = 1 AS GiroVeloce
    FROM WEEKEND_DI_GARA AS W
    JOIN PILOTA_ISCRITTO AS PI
        ON PI.IdEdizione = W.IdEdizione
    WHERE W.IdEdizione <> 5 OR W.NumeroRound <= 20
) AS DATI;

-- Trecento partecipazioni per edizione (1500 totali):
-- - i primi 240 team partecipano a una lega;
-- - i primi 60 partecipano anche a una seconda lega;
-- - gli ultimi 60 restano disponibili per provare una nuova iscrizione.
-- Nessun utente ha due propri team già iscritti alla stessa lega.
INSERT INTO PARTECIPAZIONE_TEAM (IdLega, IdTeam)
SELECT
    (TF.IdEdizione - 1) * 40
        + MOD(MOD(TF.IdTeam - 1, 300), 40) + 1,
    TF.IdTeam
FROM TEAM_FANTASY AS TF
WHERE TF.IdTeam <= 1500
  AND MOD(TF.IdTeam - 1, 300) + 1 <= 240

UNION ALL

SELECT
    (TF.IdEdizione - 1) * 40
        + MOD(MOD(TF.IdTeam - 1, 300) + 1, 40) + 1,
    TF.IdTeam
FROM TEAM_FANTASY AS TF
WHERE TF.IdTeam <= 1500
  AND MOD(TF.IdTeam - 1, 300) + 1 <= 60;

-- TeamProva2 partecipa alla lega creata da Max; TeamProva1 resta libero.
INSERT INTO PARTECIPAZIONE_TEAM (IdLega, IdTeam)
VALUES (201, 1502);

-- O2: risultati completi per 24 weekend nelle edizioni 2021-2024 e per i
-- primi 20 weekend del 2025, inclusi i due team manuali di Max.
INSERT INTO RISULTATO_TEAM (
    IdEdizione,
    IdGranPremio,
    IdTeam,
    PunteggioWeekend
)
SELECT
    CT.IdEdizione,
    PW.IdGranPremio,
    CT.IdTeam,
    SUM(PW.PunteggioFantasy)
FROM COMPOSIZIONE_TEAM AS CT
JOIN PRESTAZIONE_WEEKEND AS PW
    ON PW.IdEdizione = CT.IdEdizione
    AND PW.IdPilota = CT.IdPilota
GROUP BY
    CT.IdEdizione,
    PW.IdGranPremio,
    CT.IdTeam
HAVING COUNT(*) = 4
   AND COUNT(PW.PunteggioFantasy) = 4;

-- O3: riallineamento della ridondanza usata dalla classifica U9.
UPDATE TEAM_FANTASY AS TF
JOIN (
    SELECT
        IdTeam,
        IdEdizione,
        SUM(PunteggioWeekend) AS Totale
    FROM RISULTATO_TEAM
    GROUP BY IdTeam, IdEdizione
) AS RT
    ON RT.IdTeam = TF.IdTeam
    AND RT.IdEdizione = TF.IdEdizione
SET TF.PunteggioTotale = RT.Totale
-- IdTeam è la chiave primaria: il predicato rende l'UPDATE compatibile anche
-- con SQL_SAFE_UPDATES attivo in MySQL Workbench.
WHERE TF.IdTeam > 0;

COMMIT;

DROP TEMPORARY TABLE _SEED_COGNOME;
DROP TEMPORARY TABLE _SEED_NOME;
DROP TEMPORARY TABLE _SEED_ROSA;
DROP TEMPORARY TABLE _SEED_POSIZIONE_4;
DROP TEMPORARY TABLE _SEED_POSIZIONE_3;
DROP TEMPORARY TABLE _SEED_POSIZIONE_2;
DROP TEMPORARY TABLE _SEED_POSIZIONE_1;
DROP TEMPORARY TABLE _SEED_NUMERO;

-- Riepilogo dei volumi: deve coincidere con la stima della relazione.
SELECT
    (SELECT COUNT(*) FROM EDIZIONE) AS Edizioni,
    (SELECT COUNT(*) FROM PILOTA) AS Piloti,
    (SELECT COUNT(*) FROM PILOTA_ISCRITTO) AS PilotiIscritti,
    (SELECT COUNT(*) FROM SCUDERIA) AS Scuderie,
    (SELECT COUNT(*) FROM SCUDERIA_ISCRITTA) AS ScuderieIscritte,
    (SELECT COUNT(*) FROM GRAN_PREMIO) AS GranPremi,
    (SELECT COUNT(*) FROM WEEKEND_DI_GARA) AS WeekendDiGara,
    (SELECT COUNT(*) FROM UTENTE) AS Utenti,
    (SELECT COUNT(*) FROM TEAM_FANTASY) AS TeamFantasy,
    (SELECT COUNT(*) FROM LEGA) AS Leghe,
    (SELECT COUNT(*) FROM COMPOSIZIONE_TEAM) AS ComposizioniTeam,
    (SELECT COUNT(*) FROM PARTECIPAZIONE_TEAM) AS Partecipazioni,
    (SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND) AS Prestazioni,
    (SELECT COUNT(*) FROM RISULTATO_TEAM) AS RisultatiTeam;

-- Completezza per edizione. Il 2025 mostra intenzionalmente
-- 22 weekend, 9 scuderie, 18 piloti e risultati per soli 20 GP.
SELECT
    E.Anno,
    (SELECT COUNT(*)
     FROM WEEKEND_DI_GARA AS W
     WHERE W.IdEdizione = E.IdEdizione) AS Weekend,
    (SELECT COUNT(*)
     FROM SCUDERIA_ISCRITTA AS SI
     WHERE SI.IdEdizione = E.IdEdizione) AS Scuderie,
    (SELECT COUNT(*)
     FROM PILOTA_ISCRITTO AS PI
     WHERE PI.IdEdizione = E.IdEdizione) AS Piloti,
    (SELECT COUNT(*)
     FROM TEAM_FANTASY AS TF
     WHERE TF.IdEdizione = E.IdEdizione) AS TeamFantasy,
    (SELECT COUNT(*)
     FROM LEGA AS L
     WHERE L.IdEdizione = E.IdEdizione) AS Leghe,
    (SELECT COUNT(*)
     FROM COMPOSIZIONE_TEAM AS CT
     WHERE CT.IdEdizione = E.IdEdizione) AS Composizioni,
    (SELECT COUNT(*)
     FROM PARTECIPAZIONE_TEAM AS PT
     JOIN TEAM_FANTASY AS TF ON TF.IdTeam = PT.IdTeam
     WHERE TF.IdEdizione = E.IdEdizione) AS Partecipazioni,
    (SELECT COUNT(*)
     FROM PRESTAZIONE_WEEKEND AS PW
     WHERE PW.IdEdizione = E.IdEdizione) AS Prestazioni,
    (SELECT COUNT(*)
     FROM RISULTATO_TEAM AS RT
     WHERE RT.IdEdizione = E.IdEdizione) AS RisultatiTeam
FROM EDIZIONE AS E
ORDER BY E.Anno;

-- Invarianti principali: tutte le colonne devono valere zero.
SELECT
    (
        SELECT COUNT(*)
        FROM (
            SELECT TF.IdTeam
            FROM TEAM_FANTASY AS TF
            LEFT JOIN COMPOSIZIONE_TEAM AS CT
                ON CT.IdTeam = TF.IdTeam
                AND CT.IdEdizione = TF.IdEdizione
            GROUP BY TF.IdTeam
            HAVING COUNT(DISTINCT CT.IdPilota) <> 4
        ) AS TEAM_INCOMPLETI
    ) AS TeamConNumeroPilotiErrato,
    (
        SELECT COUNT(*)
        FROM PRESTAZIONE_WEEKEND AS PW
        WHERE PW.PunteggioFantasy <>
            GREATEST(0, 21 - CAST(PW.PosizionamentoGara AS SIGNED))
            + GREATEST(
                0,
                6 - CAST(PW.PosizionamentoQualifica AS SIGNED)
            )
            + CASE WHEN PW.RegistraGiroVeloce THEN 2 ELSE 0 END
            - CASE WHEN PW.Penalizzato THEN 5 ELSE 0 END
    ) AS PunteggiPilotaIncoerenti,
    (
        SELECT COUNT(*)
        FROM RISULTATO_TEAM AS RT
        JOIN (
            SELECT
                CT.IdEdizione,
                PW.IdGranPremio,
                CT.IdTeam,
                SUM(PW.PunteggioFantasy) AS PunteggioAtteso
            FROM COMPOSIZIONE_TEAM AS CT
            JOIN PRESTAZIONE_WEEKEND AS PW
                ON PW.IdEdizione = CT.IdEdizione
                AND PW.IdPilota = CT.IdPilota
            GROUP BY CT.IdEdizione, PW.IdGranPremio, CT.IdTeam
        ) AS ATTESO
            ON ATTESO.IdEdizione = RT.IdEdizione
            AND ATTESO.IdGranPremio = RT.IdGranPremio
            AND ATTESO.IdTeam = RT.IdTeam
        WHERE ATTESO.PunteggioAtteso <> RT.PunteggioWeekend
    ) AS RisultatiTeamIncoerenti,
    (
        SELECT COUNT(*)
        FROM TEAM_FANTASY AS TF
        JOIN (
            SELECT
                IdTeam,
                IdEdizione,
                SUM(PunteggioWeekend) AS TotaleAtteso
            FROM RISULTATO_TEAM
            GROUP BY IdTeam, IdEdizione
        ) AS ATTESO
            ON ATTESO.IdTeam = TF.IdTeam
            AND ATTESO.IdEdizione = TF.IdEdizione
        WHERE ATTESO.TotaleAtteso <> TF.PunteggioTotale
    ) AS TotaliTeamIncoerenti,
    (
        SELECT COUNT(*)
        FROM PARTECIPAZIONE_TEAM AS PT
        JOIN LEGA AS L ON L.IdLega = PT.IdLega
        JOIN TEAM_FANTASY AS TF ON TF.IdTeam = PT.IdTeam
        WHERE L.IdEdizione <> TF.IdEdizione
    ) AS PartecipazioniDiAltraEdizione,
    (
        SELECT COUNT(*)
        FROM (
            SELECT PT.IdLega, TF.IdUtente
            FROM PARTECIPAZIONE_TEAM AS PT
            JOIN TEAM_FANTASY AS TF ON TF.IdTeam = PT.IdTeam
            GROUP BY PT.IdLega, TF.IdUtente
            HAVING COUNT(*) > 1
        ) AS PARTECIPAZIONI_DUPLICATE
    ) AS UtentiConPiuTeamNellaStessaLega;
