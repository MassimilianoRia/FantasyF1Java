-- Esempi di verifica non distruttivi per MySQL.
-- Lo script esegue esclusivamente SET e SELECT: non modifica alcun dato.
-- È pensato per essere eseguito dopo schema.sql e seed.sql.

USE fantasy_f1;

SET NAMES utf8mb4;

-- Gli identificatori sono risolti tramite chiavi di dominio del seed, senza
-- dipendere dal valore assunto dai contatori AUTO_INCREMENT.
SET @id_edizione = (
    SELECT IdEdizione
    FROM EDIZIONE
    WHERE Anno = 2025
);
SET @id_utente_mario = (
    SELECT IdUtente
    FROM UTENTE
    WHERE Username = 'mario.rossi'
);
SET @id_team_mario = (
    SELECT IdTeam
    FROM TEAM_FANTASY
    WHERE IdUtente = @id_utente_mario
      AND IdEdizione = @id_edizione
      AND Nome = 'Pole Position Club'
);
SET @id_lega = (
    SELECT IdLega
    FROM LEGA
    WHERE IdEdizione = @id_edizione
      AND IdUtente = @id_utente_mario
      AND Nome = 'Lega di prova'
);
SET @id_gran_premio = (
    SELECT IdGranPremio
    FROM GRAN_PREMIO
    WHERE Nome = 'Gran Premio d''Italia'
);


-- U3 - Team dell'utente nell'edizione selezionata.
-- Atteso dal seed: quattro righe per Pole Position Club, totale 90.
SELECT
    TF.IdTeam,
    TF.Nome AS NomeTeam,
    TF.PunteggioTotale,
    P.IdPilota,
    P.Nome,
    P.Cognome,
    PI.SiglaGara,
    PI.NumeroInGara
FROM TEAM_FANTASY AS TF
JOIN COMPOSIZIONE_TEAM AS CT
    ON CT.IdTeam = TF.IdTeam
    AND CT.IdEdizione = TF.IdEdizione
JOIN PILOTA_ISCRITTO AS PI
    ON PI.IdEdizione = CT.IdEdizione
    AND PI.IdPilota = CT.IdPilota
JOIN PILOTA AS P
    ON P.IdPilota = PI.IdPilota
WHERE TF.IdUtente = @id_utente_mario
  AND TF.IdEdizione = @id_edizione
ORDER BY TF.Nome, P.Cognome, P.Nome;


-- U5 - Leghe disponibili per l'edizione.
-- Atteso dal seed: Lega di prova, amministrata da mario.rossi.
SELECT
    L.IdLega,
    L.Nome,
    U.IdUtente AS IdAmministratore,
    U.Username AS Amministratore
FROM LEGA AS L
JOIN UTENTE AS U
    ON U.IdUtente = L.IdUtente
WHERE L.IdEdizione = @id_edizione
ORDER BY L.Nome;


-- U7 - Leghe partecipate dall'utente nell'edizione selezionata.
-- Atteso dal seed: Pole Position Club partecipa a Lega di prova.
SELECT
    L.IdLega,
    L.Nome AS NomeLega,
    TF.IdTeam,
    TF.Nome AS NomeTeam
FROM TEAM_FANTASY AS TF
JOIN PARTECIPAZIONE_TEAM AS PT
    ON PT.IdTeam = TF.IdTeam
JOIN LEGA AS L
    ON L.IdLega = PT.IdLega
WHERE TF.IdUtente = @id_utente_mario
  AND TF.IdEdizione = @id_edizione
  AND L.IdEdizione = TF.IdEdizione
ORDER BY L.Nome, TF.Nome;


-- U8 - Dettaglio dei punteggi di un team in un weekend terminato.
-- Atteso dal seed: HAM=19, LEC=27, NOR=23, PIA=21.
SELECT
    TF.IdTeam,
    TF.Nome AS NomeTeam,
    P.IdPilota,
    P.Nome,
    P.Cognome,
    PW.PosizionamentoQualifica,
    PW.PosizionamentoGara,
    PW.Penalizzato,
    PW.RegistraGiroVeloce,
    PW.PunteggioFantasy
FROM TEAM_FANTASY AS TF
JOIN COMPOSIZIONE_TEAM AS CT
    ON CT.IdTeam = TF.IdTeam
    AND CT.IdEdizione = TF.IdEdizione
JOIN PILOTA AS P
    ON P.IdPilota = CT.IdPilota
JOIN PRESTAZIONE_WEEKEND AS PW
    ON PW.IdEdizione = CT.IdEdizione
    AND PW.IdPilota = CT.IdPilota
JOIN WEEKEND_DI_GARA AS W
    ON W.IdEdizione = PW.IdEdizione
    AND W.IdGranPremio = PW.IdGranPremio
WHERE TF.IdTeam = @id_team_mario
  AND TF.IdUtente = @id_utente_mario
  AND TF.IdEdizione = @id_edizione
  AND PW.IdGranPremio = @id_gran_premio
  AND W.DataFine <= CURRENT_DATE
  AND PW.PunteggioFantasy IS NOT NULL
ORDER BY P.Cognome, P.Nome;


-- U9 - Classifica dinamica della lega.
-- Atteso dal seed: entrambi i team hanno 90 punti; il nome risolve il pareggio.
SELECT
    TF.IdTeam,
    TF.Nome AS NomeTeam,
    U.Username AS Proprietario,
    TF.PunteggioTotale
FROM LEGA AS L
JOIN PARTECIPAZIONE_TEAM AS PT
    ON PT.IdLega = L.IdLega
JOIN TEAM_FANTASY AS TF
    ON TF.IdTeam = PT.IdTeam
    AND TF.IdEdizione = L.IdEdizione
JOIN UTENTE AS U
    ON U.IdUtente = TF.IdUtente
WHERE L.IdLega = @id_lega
  AND L.IdEdizione = @id_edizione
ORDER BY TF.PunteggioTotale DESC, TF.Nome;


-- O1 - Verifica della policy demo sui punteggi memorizzati.
-- Tutte le righe devono restituire PunteggioCoerente = 1.
SELECT
    P.Cognome,
    PW.PunteggioFantasy AS PunteggioMemorizzato,
    (
        CASE
            WHEN PW.PosizionamentoGara IS NULL THEN 0
            ELSE GREATEST(0, 21 - PW.PosizionamentoGara)
        END
        + CASE
            WHEN PW.PosizionamentoQualifica IS NULL THEN 0
            ELSE GREATEST(0, 6 - PW.PosizionamentoQualifica)
        END
        + CASE WHEN COALESCE(PW.RegistraGiroVeloce, FALSE) THEN 2 ELSE 0 END
        - CASE WHEN COALESCE(PW.Penalizzato, FALSE) THEN 5 ELSE 0 END
    ) AS PunteggioAtteso,
    PW.PunteggioFantasy = (
        CASE
            WHEN PW.PosizionamentoGara IS NULL THEN 0
            ELSE GREATEST(0, 21 - PW.PosizionamentoGara)
        END
        + CASE
            WHEN PW.PosizionamentoQualifica IS NULL THEN 0
            ELSE GREATEST(0, 6 - PW.PosizionamentoQualifica)
        END
        + CASE WHEN COALESCE(PW.RegistraGiroVeloce, FALSE) THEN 2 ELSE 0 END
        - CASE WHEN COALESCE(PW.Penalizzato, FALSE) THEN 5 ELSE 0 END
    ) AS PunteggioCoerente
FROM PRESTAZIONE_WEEKEND AS PW
JOIN PILOTA AS P
    ON P.IdPilota = PW.IdPilota
WHERE PW.IdEdizione = @id_edizione
  AND PW.IdGranPremio = @id_gran_premio
ORDER BY P.Cognome, P.Nome;


-- Elaborabilità - il weekend è terminato e ogni pilota attualmente iscritto
-- possiede una prestazione con punteggio non nullo.
-- La colonna Elaborabile deve valere 1 per il weekend del seed.
SELECT
    W.IdEdizione,
    W.IdGranPremio,
    W.DataFine,
    COUNT(DISTINCT PI.IdPilota) AS PilotiIscritti,
    COUNT(DISTINCT CASE
        WHEN PW.PunteggioFantasy IS NOT NULL THEN PI.IdPilota
    END) AS PrestazioniCalcolate,
    (
        W.DataFine <= CURRENT_DATE
        AND COUNT(DISTINCT PI.IdPilota) > 0
        AND COUNT(DISTINCT PI.IdPilota) = COUNT(DISTINCT CASE
            WHEN PW.PunteggioFantasy IS NOT NULL THEN PI.IdPilota
        END)
    ) AS Elaborabile
FROM WEEKEND_DI_GARA AS W
JOIN PILOTA_ISCRITTO AS PI
    ON PI.IdEdizione = W.IdEdizione
LEFT JOIN PRESTAZIONE_WEEKEND AS PW
    ON PW.IdEdizione = W.IdEdizione
    AND PW.IdGranPremio = W.IdGranPremio
    AND PW.IdPilota = PI.IdPilota
WHERE W.IdEdizione = @id_edizione
  AND W.IdGranPremio = @id_gran_premio
GROUP BY W.IdEdizione, W.IdGranPremio, W.DataFine;


-- O2 - Verifica del risultato di ciascun team nel weekend.
-- Ogni riga deve avere 4 componenti, 4 punteggi e SommaPiloti = 90 =
-- PunteggioWeekend.
SELECT
    RT.IdTeam,
    TF.Nome AS NomeTeam,
    COUNT(CT.IdPilota) AS NumeroComponenti,
    COUNT(PW.PunteggioFantasy) AS NumeroPunteggi,
    SUM(PW.PunteggioFantasy) AS SommaPiloti,
    RT.PunteggioWeekend,
    (
        COUNT(CT.IdPilota) = 4
        AND COUNT(PW.PunteggioFantasy) = 4
        AND SUM(PW.PunteggioFantasy) = RT.PunteggioWeekend
    ) AS RisultatoCoerente
FROM RISULTATO_TEAM AS RT
JOIN TEAM_FANTASY AS TF
    ON TF.IdTeam = RT.IdTeam
    AND TF.IdEdizione = RT.IdEdizione
JOIN COMPOSIZIONE_TEAM AS CT
    ON CT.IdTeam = RT.IdTeam
    AND CT.IdEdizione = RT.IdEdizione
LEFT JOIN PRESTAZIONE_WEEKEND AS PW
    ON PW.IdEdizione = CT.IdEdizione
    AND PW.IdGranPremio = RT.IdGranPremio
    AND PW.IdPilota = CT.IdPilota
WHERE RT.IdEdizione = @id_edizione
  AND RT.IdGranPremio = @id_gran_premio
GROUP BY RT.IdTeam, TF.Nome, RT.PunteggioWeekend
ORDER BY TF.Nome;


-- O3 - Verifica della ridondanza PunteggioTotale.
-- Ogni riga deve restituire TotaleCoerente = 1.
SELECT
    TF.IdTeam,
    TF.Nome AS NomeTeam,
    TF.PunteggioTotale AS TotaleMemorizzato,
    COALESCE(SUM(RT.PunteggioWeekend), 0) AS TotaleAtteso,
    TF.PunteggioTotale = COALESCE(SUM(RT.PunteggioWeekend), 0)
        AS TotaleCoerente
FROM TEAM_FANTASY AS TF
LEFT JOIN RISULTATO_TEAM AS RT
    ON RT.IdTeam = TF.IdTeam
    AND RT.IdEdizione = TF.IdEdizione
WHERE TF.IdEdizione = @id_edizione
GROUP BY TF.IdTeam, TF.Nome, TF.PunteggioTotale
ORDER BY TF.Nome;


-- Invarianti del seed: entrambe le query seguenti devono restituire zero righe.

-- Nessun team partecipante incompleto o appartenente a un'altra edizione.
SELECT
    PT.IdLega,
    TF.IdTeam,
    TF.Nome AS NomeTeam,
    COUNT(DISTINCT CT.IdPilota) AS NumeroComponenti
FROM PARTECIPAZIONE_TEAM AS PT
JOIN LEGA AS L
    ON L.IdLega = PT.IdLega
JOIN TEAM_FANTASY AS TF
    ON TF.IdTeam = PT.IdTeam
LEFT JOIN COMPOSIZIONE_TEAM AS CT
    ON CT.IdTeam = TF.IdTeam
    AND CT.IdEdizione = TF.IdEdizione
GROUP BY
    PT.IdLega,
    TF.IdTeam,
    TF.Nome,
    L.IdEdizione,
    TF.IdEdizione
HAVING L.IdEdizione <> TF.IdEdizione
    OR COUNT(DISTINCT CT.IdPilota) <> 4;

-- Nessun utente ha più di un proprio team nella stessa lega.
SELECT
    PT.IdLega,
    TF.IdUtente,
    COUNT(*) AS TeamDelloStessoUtente
FROM PARTECIPAZIONE_TEAM AS PT
JOIN TEAM_FANTASY AS TF
    ON TF.IdTeam = PT.IdTeam
GROUP BY PT.IdLega, TF.IdUtente
HAVING COUNT(*) > 1;
