# Matrice di tracciabilità — FantasyF1Java

Fonte normativa: `main.pdf` (38 pagine PDF, numerazione interna 1–37). La
matrice è stata compilata prima delle modifiche funzionali, dopo la lettura
integrale della relazione e l'ispezione visiva degli schemi E/R (figure
2.1–2.7) e logico (figura 3.7).

Legenda stato: `ASSENTE`, `PARZIALE`, `DB`, `DA IMPLEMENTARE`, `VERIFICATO`.

## Operazioni utente

| Codice | Fonte | Comportamento richiesto | Stato iniziale | Garanzia già presente | Controllo applicativo necessario | File/simboli previsti | Test di accettazione |
|---|---|---|---|---|---|---|---|
| U1 | §1.5.1 p. 6; query p. 30 | Registrazione con dati personali, credenziali e recapiti | ASSENTE | `UTENTE`; UNIQUE username/email; lunghezze SQL | validazione, hash salato, duplicati comprensibili | `AuthService`, `UserDao`, `PasswordHasher`, `RegistrationRequest`, `LoginView` | registrazione valida; password non in chiaro; username/email duplicati |
| U2 | §1.5.1 p. 6; query pp. 30–31 | Creazione atomica di un team dell'edizione con esattamente quattro piloti distinti iscritti | ASSENTE | FK composite di `COMPOSIZIONE_TEAM`, PK anti-duplicato, generated key | proprietà da sessione, cardinalità 4, coerenza selezioni, transazione e recupero storico O2/O3 | `TeamService.createTeam`, `TeamDao`, `PilotDao`, `CreateTeamView` | 3/5/duplicati/altra edizione respinti; rollback; generated key; storico ricalcolato |
| U3 | §1.5.1 p. 6; §3.3.1 pp. 15–16; query p. 31 | Elenco dei propri team dell'edizione, composizione e totale | ASSENTE | FK team-utente-edizione; totale persistito | filtro obbligatorio sessione+edizione; grouping projection | `TeamService.myTeams`, `TeamDao.findOwnedWithRoster`, `TeamSummary` | nessun team altrui; composizione completa; totale esposto; filtro storico |
| U4 | §1.5.1 p. 6; query p. 31 | Creazione lega dell'edizione; utente autenticato amministratore | ASSENTE | FK a utente/edizione; UNIQUE nome per creatore/edizione | proprietà dalla sessione, validazione nome ed edizione selezionata | `LeagueService.createLeague`, `LeagueDao`, `LeaguesView` | lega creata nelle edizioni corrente e storica; proprietario corretto; duplicato |
| U5 | §1.5.1 p. 6; §3.3.2 pp. 16–17; query pp. 31–32 | Tutte le leghe dell'edizione | PARZIALE: DAO+tabella, senza login/service e con JDBC sul FX thread | query filtrata per edizione | accesso solo autenticato, service, caricamento asincrono | `LeagueService.availableLeagues`, `LeagueDao.findByEdition`, `LeaguesView` | filtro edizione, ordinamento nome, stato vuoto, nessun blocco FX |
| U6 | §1.5.1 p. 6; query p. 32 | Iscrizione di un proprio team a lega della stessa edizione | ASSENTE | PK evita stesso team due volte; FK base | lock lega→team/partecipazioni, proprietà, stessa edizione, massimo un team dello stesso utente | `LeagueService.joinLeague`, `LeagueDao.lock/findParticipation/insert` | team altrui/altra edizione/secondo team respinti; creatore ammesso; team in leghe diverse; concorrenza |
| U7 | §1.5.1 p. 7; query p. 32 | Leghe partecipate dai propri team nell'edizione | ASSENTE | FK partecipazioni | filtro sessione+edizione e projection team/lega | `LeagueService.joinedLeagues`, `LeagueDao.findJoinedByOwner` | solo partecipazioni dell'utente e dell'edizione; ordinamento |
| U8 | §1.5.1 p. 7; §3.3.3 p. 17; query p. 33 | Dettaglio dei quattro punteggi pilota del team per weekend terminato/registrato | ASSENTE | FK prestazione-pilota-weekend; CHECK posizioni | proprietà team, edizione, weekend elaborabile, esattamente 4 valori finali | `TeamService.weekendBreakdown`, `ResultDao`, `WeekendScoreRow` | team altrui respinto; solo weekend concluso/elaborato; quattro righe e somma coerente |
| U9 | §1.5.1 p. 7; §3.3.4 pp. 17–18; query p. 33 | Classifica dinamica per totale DESC, nome team | ASSENTE (query solo in `test.sql`) | `PunteggioTotale` ridondante persistito | lega dell'edizione selezionata; query senza ricalcolo | `LeagueService.standings`, `LeagueDao.findStandings`, `StandingRow` | ordine totale DESC/nome ASC; filtro lega/edizione; usa totale memorizzato |

## Operazioni amministrative

L'amministratore è trusted ed esterno al dominio (§1.2 p. 4): nessuna entità,
registrazione o credenziale admin. La modalità amministratore è selezionabile
soltanto nella schermata iniziale comune e non appartiene alla sessione utente.

| Codice | Fonte | Comportamento richiesto | Stato iniziale | Garanzia già presente | Controllo applicativo necessario | File/simboli previsti | Test di accettazione |
|---|---|---|---|---|---|---|---|
| A1 | §1.5.2 p. 7; query p. 33 | Inserire edizione | ASSENTE | PK/UNIQUE numero e anno | range e campi obbligatori; errore duplicato | `AdminService.createEdition`, `AdminDao`, `AdminDashboard` | creazione; duplicati distinti; modalità amministratore |
| A2 | §1.5.2 p. 7; §3.3.5 p. 18; query pp. 33–34 | Inserire o aggiornare Gran Premio | ASSENTE | UNIQUE nome | scelta esplicita insert/update; validazione descrittivi; operazioni atomiche | `AdminService.createGrandPrix`, `AdminService.updateGrandPrix`, `AdminDao` | insert e update anche del nome sullo stesso identificatore |
| A3 | §1.5.2 p. 7; §3.3.6 pp. 18–19; query p. 34 | Inserire weekend nell'edizione | ASSENTE | FK; UNIQUE round; CHECK 1–24 e date | lock edizione, massimo 24, selezioni coerenti, intervallo | `AdminService.addRaceWeekend`, `AdminDao` | limite 24 sotto transazione; round/data invalidi; FK |
| A4 | §1.5.2 p. 7; query p. 34 | Inserire scuderia anagrafica | ASSENTE | UNIQUE nome | validazione | `AdminService.createConstructor`, `AdminDao` | inserimento e duplicato |
| A5 | §1.5.2 p. 7; query p. 34 | Iscrivere scuderia con nome stagionale e vettura | ASSENTE | PK stagionale; FK; UNIQUE nome iscrizione | lock edizione, massimo 10, selezione anagrafica | `AdminService.enrollConstructor`, `AdminDao` | limite 10; duplicati; rollback |
| A6 | §1.5.2 p. 7; query p. 34 | Inserire pilota anagrafico | ASSENTE | PK, tipi/data | validazione persona/data | `AdminService.createDriver`, `AdminDao` | inserimento; campi/date invalidi |
| A7 | §1.5.2 p. 7; query p. 34 | Iscrivere pilota con sigla, numero e scuderia della stessa edizione | ASSENTE | FK composita; UNIQUE sigla/numero | lock edizione, massimo 20 e massimo 2/scuderia, sigla 3, range numero | `AdminService.enrollDriver`, `AdminDao` | limiti 20/2; scuderia altra edizione; sigla/numero duplicati |
| A8 | §1.5.2 p. 7; §3.3.7 pp. 19–20; query p. 35 | Registrare/correggere prestazione ufficiale e ricalcolare | ASSENTE | PK/FK; CHECK posizioni | upsert, pilota/weekend stessa edizione, transazione unica A8→O1→O2→O3 | `WeekendProcessingService.recordPerformance`, `ResultDao`, `AdminDashboard` | inserimento, correzione a cascata, idempotenza, rollback completo |

## Operazioni automatiche

| Codice | Fonte | Comportamento richiesto | Stato iniziale | Garanzia già presente | Controllo applicativo necessario | File/simboli previsti | Test di accettazione |
|---|---|---|---|---|---|---|---|
| O1 | §1.5.3 p. 7; §3.3.8 p. 20; query p. 35 | Calcolare e memorizzare punteggio dalla prestazione | ASSENTE; seed contiene valori manuali | colonna nullable `PunteggioFantasy` | policy isolata deterministica; null gestiti; update nella transazione A8 | `ScoringPolicy`, `SimpleScoringPolicy`, `WeekendProcessingService` | casi gara/qualifica/bonus/malus/null; valore persistito |
| O2 | §1.5.3 p. 7; §3.3.9 p. 20; query p. 35 | Upsert somma dei quattro punteggi solo con team e dati completi | ASSENTE; seed manuale | PK risultato e query `HAVING` descritta in relazione | elaborabilità weekend; rimozione di risultati divenuti non definitivi; tutti i team completi | `ResultDao.recalculateWeekendResults` | assente con meno di 4 punteggi; corretto con 4; upsert idempotente |
| O3 | §1.5.3 p. 7; §3.3.10 p. 21; query pp. 35–36 | Totale team = somma risultati, zero se assenti | ASSENTE; seed manuale | colonna non-null `PunteggioTotale` | update di tutti i team interessati dopo O2 e per team storico nuovo | `ResultDao.recalculateTeamTotals` | somma/zero; correzione; ripetizione invariata |

## Vincoli trasversali, architettura e sicurezza

| Vincolo | Fonte | DB/codice iniziale | Responsabilità da implementare | Test/criterio |
|---|---|---|---|---|
| Sessione, login, logout, proprietà risorse | account §1.3 pp. 4–5; schema pp. 25–29 | tabella `UTENTE`, nessun auth/session | sessione immutabile; nessun ID utente inseribile; gate dashboard | login valido/non valido, logout, dashboard negata |
| Hash password e compatibilità seed | `PasswordHash` nello schema pp. 25–29; requisito applicativo allegato | SHA-256 non salato nel seed | PBKDF2 salato e upgrade trasparente del legacy SHA-256 al login | formato hash, verify, migrazione, nessun log sensibile |
| Edizione selezionata e storico | §1.2 p. 4 | `EdizioneDao` ordina anno DESC | contesto selezione; default più recente; ogni query stagionale filtrata; creazione storica ammessa | default, cambio, persistenza navigazione, isolamento |
| Completezza progressiva | §2.4 pp. 11–12; figure E/R | FK/PK; nessun conteggio aggregato | limiti massimi durante insert; stato completa solo 24/10/20 e 2 per scuderia | limiti e stato incompleta/completa |
| Team esattamente quattro e mai parziale | §1.2 pp. 3–4; §2.4 pp. 11–12 | PK/FK permettono 0–N persistenti | un solo service transazionale; DAO non esposto alla UI | rollback e cardinalità |
| U6 concorrente | query p. 32 insufficiente da sola | PK solo coppia lega/team | transazione `SELECT ... FOR UPDATE` su lega, team e partecipazioni in ordine | due richieste concorrenti: una sola partecipazione per proprietario |
| Weekend terminato/elaborabile | U8 p. 7; O3 p. 7 | data fine e punteggi nullable | terminato se `DataFine <= oggi`; definitivo/elaborabile solo quando ogni pilota attualmente iscritto possiede una prestazione con punteggio calcolato. Ciò supporta il popolamento progressivo senza creare risultati parziali | data futura; una prestazione mancante; tutte presenti; nessun risultato parziale |
| JDBC e transazioni | query pp. 30–36 | prepared statement nei 2 DAO; ogni DAO apre connessione; UI chiama DAO sul FX thread | DAO con `Connection` fornita; try-with-resources; commit/rollback; mapping errori | rollback fault injection; query parametrizzate; test service |
| UI reattiva e navigazione | capitolo 4 p. 37 vuoto; requisiti allegati | singola schermata U5 sincrona | scelta iniziale Utente/Admin con pulsanti affiancati; login/registrazione; dashboard a tab; selezione edizione persistente; `Task`; unico entry point | smoke dell'applicazione unificata; nessun pulsante stub/ID tecnico |
| Errori uniformi | requisito allegato | messaggio DB generico + stderr | eccezioni dominio tipizzate e mapper SQL; alert comprensibili, niente stack trace UI | duplicate/not found/validation/connection distinti |
| DDL e seed coerenti | §3.7–3.9 pp. 25–36 | schema quasi conforme; mojibake nei nomi; seed con team vuoto partecipante e ridondanze incoerenti | correggere encoding e seed; mantenere ridondanze; nessuna tabella ADMIN/REGOLAMENTO/CLASSIFICA | script staticamente coerenti; test JDBC isolato se disponibile |
| Isolamento trusted admin | §1.2 p. 4 | assente | modalità scelta all'avvio nello stesso `FantasyF1Application`; nessuna autenticazione/ruolo/tabella admin | unico entry point; sessione utente priva di ruolo admin; dashboard distinte |
| Formula dimostrativa | gap confermato dall'utente | nessuna formula normativa | policy minima basata sui dati sportivi, sostituibile e documentata | unit test completo della policy |

## Decisioni e discrepanze registrate

1. **SPEC GAP risolto con assunzione esplicita:** la relazione non assegna valori
   numerici al regolamento. Su indicazione dell'utente si adotta una policy
   dimostrativa semplice, purché basata chiaramente sui risultati sportivi.
2. Lo schema E/R iniziale usa cardinalità `0–4` per la composizione per
   consentire il popolamento progressivo, ma il testo normativo richiede che un
   team applicativo nasca con esattamente quattro piloti. La cardinalità esatta
   viene quindi garantita dalla transazione U2, non modificando il DDL in modo
   da impedire il popolamento progressivo generale.
3. La relazione descrive A8 come inserimento con punteggio inizialmente nullo;
   il requisito finale richiede anche correzioni. Si usa un upsert applicativo e
   si esegue il ricalcolo a cascata nella stessa transazione.
4. La relazione non definisce un flag di weekend terminato. La semantica
   applicativa finale è quella documentata nella riga dedicata sopra: data di
   fine raggiunta e punteggio calcolato per ogni pilota attualmente iscritto.
   Mantiene usabile il popolamento progressivo senza produrre risultati
   parziali.

## Esito finale della checklist

| Requisito | Stato | Implementazione principale | Verifica automatica |
|---|---|---|---|
| U1 | VERIFICATO | `AuthenticationService`, `Pbkdf2PasswordHasher`, login/registrazione JavaFX | `AuthenticationServiceH2Test`, `Pbkdf2PasswordHasherTest` |
| U2 | VERIFICATO | `TeamService.createTeam`, generated key e unica transazione | `TeamLeagueServiceH2Test` (3/5/duplicati/altra edizione, rollback, key) |
| U3 | VERIFICATO | `TeamDao.findOwnedWithRoster`, tab Team | `TeamLeagueServiceH2Test` |
| U4 | VERIFICATO | `LeagueService.createLeague`, tab Leghe | `TeamLeagueServiceH2Test` |
| U5 | VERIFICATO | `LegaDao.findByEdition`, tab Leghe asincrona | `TeamLeagueServiceH2Test` |
| U6 | VERIFICATO | lock LEGA → TEAM → partecipazioni in `LeagueService.joinLeague` | `TeamLeagueServiceH2Test`, incluso test concorrente a due connessioni |
| U7 | VERIFICATO | `LegaDao.findJoinedByOwner` | `TeamLeagueServiceH2Test` |
| U8 | VERIFICATO | `TeamService.weekendBreakdown`, solo risultato definitivo | `WeekendProcessingH2Test` |
| U9 | VERIFICATO | `LegaDao.findStandings`, totale ridondante e ordinamento richiesto | `TeamLeagueServiceH2Test` |
| A1 | VERIFICATO | `AdminService.createEdition`, tab A1 | `AdminCrudH2Test` |
| A2 | VERIFICATO | upsert `AdminDao.upsertGrandPrix`, tab A2 | `AdminCrudH2Test` |
| A3 | VERIFICATO | lock edizione, limite 24, tab A3 | `AdminLimitsH2Test`, `AdminCrudH2Test` |
| A4 | VERIFICATO | `AdminService.createConstructor`, tab A4 | `AdminCrudH2Test` |
| A5 | VERIFICATO | lock edizione, limite 10, tab A5 | `AdminLimitsH2Test`, `AdminCrudH2Test` |
| A6 | VERIFICATO | `AdminService.createDriver`, tab A6 | `AdminCrudH2Test` |
| A7 | VERIFICATO | lock edizione, limiti 20/2 e FK stagionale, tab A7 | `AdminLimitsH2Test`, `AdminCrudH2Test` |
| A8 | VERIFICATO | upsert/correzione in `WeekendProcessingService`, tab A8 | `WeekendProcessingH2Test` |
| O1 | VERIFICATO | `ScoringPolicy` isolata e ricalcolo di tutte le prestazioni | `SimpleScoringPolicyTest`, `WeekendProcessingH2Test` |
| O2 | VERIFICATO | ricostruzione/upsert risultati soltanto con quattro score | `WeekendProcessingH2Test` |
| O3 | VERIFICATO | somma risultati memorizzati, zero in assenza | `WeekendProcessingH2Test` |
| Auth/sessione | VERIFICATO | sessione immutabile, migrazione legacy, logout, dashboard protetta | test auth/sessione e smoke applicazione |
| Limiti/completezza | VERIFICATO | controlli sotto lock e `EditionStatus` | `AdminLimitsH2Test` |
| Vincoli DB | VERIFICATO SU H2 ISOLATO | 14 tabelle equivalenti per test | `DatabaseConstraintsH2Test` |
| UI utente/admin | VERIFICATO STATICAMENTE E CON SMOKE | schermata iniziale comune, unico entry point, viste distinte, soli service, `Task` daemon, nessun ID tecnico | `smokeApp`, compilazione |
| MySQL locale | BLOCCATO ESTERNAMENTE | configurazione invariata | il server risponde ma rifiuta le credenziali disponibili; nessuno script distruttivo eseguito |

La suite H2 con `MODE=MySQL` valida workflow e query su uno schema isolato.
Non sostituisce una prova conclusiva delle specificità di lock/collation del
server MySQL reale; tale verifica resta subordinata a credenziali accettate dal
server o a un'istanza MySQL usa-e-getta.
