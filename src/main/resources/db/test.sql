-- Test delle principali query di consultazione.
-- Script eseguibile direttamente in MySQL Workbench.
--
-- Valori di test utilizzati:
--   IdEdizione   = 1
--   IdUtente     = 1
--   IdLega       = 1
--   IdTeam       = 1
--   IdGranPremio = 1
--
-- Se il database contiene identificativi diversi, sostituire questi valori
-- direttamente nelle clausole WHERE.


-- U5 - Visualizzazione delle leghe disponibili per un'edizione
SELECT
    L.IdLega,
    L.Nome,
    U.IdUtente AS IdAmministratore,
    U.Username AS Amministratore
FROM LEGA AS L
JOIN UTENTE AS U
    ON U.IdUtente = L.IdUtente
WHERE L.IdEdizione = 1
ORDER BY L.Nome;


-- U9 - Classifica di una lega
SELECT
    TF.IdTeam,
    TF.Nome AS NomeTeam,
    U.Username AS Proprietario,
    TF.PunteggioTotale
FROM PARTECIPAZIONE_TEAM AS PT
JOIN TEAM_FANTASY AS TF
    ON TF.IdTeam = PT.IdTeam
JOIN UTENTE AS U
    ON U.IdUtente = TF.IdUtente
WHERE PT.IdLega = 1
ORDER BY TF.PunteggioTotale DESC, TF.Nome;


-- U3 - Team dell'utente nell'edizione selezionata
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
WHERE TF.IdUtente = 1
  AND TF.IdEdizione = 1
ORDER BY TF.Nome, P.Cognome, P.Nome;


-- U7 - Leghe partecipate dall'utente nell'edizione selezionata
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
WHERE TF.IdUtente = 1
  AND TF.IdEdizione = 1
  AND L.IdEdizione = TF.IdEdizione
ORDER BY L.Nome, TF.Nome;


-- U8 - Punteggi dei piloti di un team in un determinato weekend
SELECT
    TF.IdTeam,
    TF.Nome AS NomeTeam,
    P.IdPilota,
    P.Nome,
    P.Cognome,
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
WHERE TF.IdTeam = 1
  AND PW.IdEdizione = 1
  AND PW.IdGranPremio = 1
ORDER BY P.Cognome, P.Nome;
