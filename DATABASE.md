# Database, avvio e dati dimostrativi

## Requisiti

L'applicazione usa Java 21, JDBC diretto e MySQL. È richiesto MySQL 8.0.16 o
successivo: le versioni precedenti accettano la sintassi dei vincoli `CHECK` ma
non li applicano.

Le tabelle sono create con motore InnoDB, necessario per chiavi esterne,
transazioni, commit e rollback. Gli script SQL sono codificati in UTF-8 e usano
identificatori come `Città` e `Nazionalità`.

## Configurazione della connessione

La configurazione predefinita è:

- URL: `jdbc:mysql://localhost:3306/fantasy_f1`
- utente: `root`
- password: `Stellarium!23`

Non è necessario impostare variabili d'ambiente: l'applicazione usa
automaticamente questi valori a ogni avvio.

Per controllare soltanto configurazione, credenziali e raggiungibilità del
database:

```powershell
.\gradlew.bat checkDatabaseConnection
```

## Creazione e popolamento

Eseguire nell'ordine:

1. `src/main/resources/db/schema.sql`
2. `src/main/resources/db/seed.sql`

Per conservare un database creato con la versione precedente, eseguire invece
una sola volta `src/main/resources/db/migrate_concluso.sql`. La migrazione
aggiunge `Concluso`, riconosce come conclusi soltanto i weekend legacy già
completi e calcolati, elimina eventuali risultati provvisori e riallinea O3
senza usare `DataFine`.

`schema.sql` è intenzionalmente distruttivo: esegue
`DROP DATABASE IF EXISTS fantasy_f1` e ricrea l'intero database. Va usato
soltanto su un'istanza locale usa-e-getta, dopo aver verificato con attenzione
host e nome del database. Non eseguirlo su un database condiviso o contenente
dati da conservare.

`seed.sql` ripristina sempre il dataset dimostrativo: 5 edizioni,
40 piloti anagrafici e 98 iscrizioni stagionali, 15 scuderie anagrafiche e
49 iscrizioni stagionali, 30 Gran Premi, 118 weekend, 1001 utenti, 1502 team,
201 leghe, 6008 componenti, 1501 partecipazioni, 2280 prestazioni e
34840 risultati team.

Le edizioni 2021-2024 sono complete con 24 weekend conclusi, 10 scuderie,
20 piloti, 300 team e 40 leghe. Il 2025 resta intenzionalmente incompleto:
contiene 22 weekend, 9 scuderie, 18 piloti e prestazioni/punteggi soltanto per
i primi 20 weekend, marcati conclusi. Gli ultimi due sono non conclusi e senza
risultati, così A8 e A9 possono essere provate su dati coerenti.

`reset.sql` svuota tutte le 14 tabelle tramite `TRUNCATE` e azzera gli
`AUTO_INCREMENT`. Anche questo script è distruttivo: `TRUNCATE` produce commit
impliciti e non può essere annullato con `ROLLBACK`. Non deve essere eseguito
sul database di sviluppo se non è stato prima dimostrato che i dati siano
eliminabili.

`test.sql` è invece non distruttivo. Dopo il seed esegue soltanto `SET` e
`SELECT` per mostrare U3, U5, U7, U8, U9 e verificare lo stato A9/A10, O1,
O2, O3 e le principali invarianti dei dati dimostrativi.

Gli script di produzione sono specifici per MySQL e non devono essere eseguiti
direttamente su H2 o su un database di test casuale: contengono istruzioni
MySQL quali `USE`, `UNSIGNED`, `YEAR`, variabili di sessione,
`LAST_INSERT_ID()`, `SHA2()` e `FOREIGN_KEY_CHECKS`.

## Account del seed

I 1000 utenti sono generati uniformemente combinando 25 nomi e 40 cognomi.
Username ed email seguono sempre la convenzione `nome.cognome`; tutti gli
account usano la stessa password iniziale:

| Username | Password |
| --- | --- |
| `alessandro.rossi` | `fantasyf1-2025` |
| `beatrice.rossi` | `fantasyf1-2025` |
| `luca.ferri` | `fantasyf1-2025` |
| `max` | `database` |

Solo per dimostrare la compatibilità con dati preesistenti, il seed memorizza
gli hash SHA-256 legacy delle password. La password in chiaro non viene
salvata nella tabella `UTENTE`. Dopo un login legacy valido, il servizio di
autenticazione sostituisce automaticamente il valore con il formato adattivo e
salato usato per le nuove registrazioni. Password e hash non devono essere
scritti nei log.

I team sono denominati uniformemente combinando 30 identità racing con
10 qualificatori, ad esempio `Apex Racing`, `Vertex Motorsport` e
`Paddock Formula`. Le leghe seguono le due convenzioni `Trofeo <tema>` e
`Campionato <tema>`. I primi 60 proprietari hanno due team per edizione:
il primo partecipa a due leghe, mentre il secondo non è ancora iscritto e
consente di provare U6 senza creare altri dati.

L'account `max` possiede inoltre `TeamProva1`, `TeamProva2` e la lega
`LegaProva1` nell'edizione 2025. `TeamProva2` è già iscritto a
`LegaProva1`, mentre `TeamProva1` resta disponibile per altre prove.
Le rose generate per il volume sono combinazioni distinte e pseudo-casuali
ma ripetibili, così le classifiche non raggruppano sistematicamente team con
gli stessi quattro piloti.

## Policy dimostrativa di punteggio

La relazione richiede che il punteggio derivi dalla prestazione sportiva del
weekend, ma non stabilisce una formula numerica. Per il progetto dimostrativo è
usata una policy semplice e isolata:

```text
punti gara       = max(0, 21 - posizione in gara)
punti qualifica  = max(0,  6 - posizione in qualifica)
giro veloce      = +2
penalizzazione   = -5
punteggio        = punti gara + punti qualifica + bonus - malus
```

Una posizione nulla vale zero. I flag `Penalizzato` e
`RegistraGiroVeloce`, usati direttamente dalla policy, devono essere presenti
prima della convalida A9.
Il seed applica questa formula a tutte le 2280 prestazioni, calcola i 34840
risultati dei team come somma dei rispettivi quattro piloti e riallinea
`PunteggioTotale`. La policy è una scelta applicativa dimostrativa e non viene
presentata come formula prescritta dalla relazione.

## Conclusione amministrativa del weekend

`Concluso` è uno stato amministrativo esplicito e non deriva mai da
`DataFine`. Un weekend appena creato è non concluso.

A8 consente di inserire o correggere le prestazioni soltanto mentre il weekend
è aperto e lascia sempre `PunteggioFantasy` a `NULL`. A9 blocca il weekend,
verifica che esista una prestazione completa per ogni pilota iscritto, imposta
`Concluso = TRUE` e, nella stessa transazione, esegue O1, O2 e O3. Un errore
annulla l'intera operazione, incluso il cambio di stato.

O1 e O2 includono un controllo SQL su `Concluso = TRUE`; O3 somma
esclusivamente `RISULTATO_TEAM` collegati a weekend conclusi. U8 applica lo
stesso filtro e l'interfaccia utente presenta i punteggi come disponibili solo
dopo A9.

A10 è un'operazione amministrativa eccezionale per rettifiche sportive
successive alla convalida. È disponibile soltanto su un weekend concluso e,
nella stessa transazione JDBC:

1. imposta `Concluso = FALSE`;
2. porta a `NULL` tutti i relativi `PunteggioFantasy`;
3. elimina i relativi `RISULTATO_TEAM`;
4. ricalcola a zero o alla somma dei soli risultati conclusi il
   `PunteggioTotale` di ogni team dell'edizione.

Le righe di `PRESTAZIONE_WEEKEND` e i risultati sportivi restano intatti.
Riaperto il weekend, A8 può correggerli mantenendo il punteggio fantasy nullo;
una nuova A9 verifica nuovamente la completezza e rigenera O1-O3. La
cancellazione preventiva dei risultati rende il ciclo
conclusione → riapertura → nuova conclusione privo di duplicati. Errori in
qualunque fase di A10 causano il rollback anche del cambio di stato.

Le query applicative A10 sono `PreparedStatement` nei DAO e sono equivalenti a:

```sql
UPDATE WEEKEND_DI_GARA
SET Concluso = FALSE
WHERE IdEdizione = ? AND IdGranPremio = ? AND Concluso = TRUE;

UPDATE PRESTAZIONE_WEEKEND
SET PunteggioFantasy = NULL
WHERE IdEdizione = ? AND IdGranPremio = ?;

DELETE FROM RISULTATO_TEAM
WHERE IdEdizione = ? AND IdGranPremio = ?;
```

Il ricalcolo O3 già esistente aggiorna tutti i team dell'edizione usando
`COALESCE(SUM(...), 0)` e considera soltanto weekend con `Concluso = TRUE`.

L'edizione selezionata inizialmente è quella con l'anno più recente. L'utente
può cambiarla per consultare o provare anche le edizioni storiche.

## Avvio

L'applicazione ha un unico punto di accesso:

```powershell
.\gradlew.bat run
```

Alla prima apertura viene mostrata una schermata con due pulsanti affiancati
per scegliere la modalità **Utente** oppure **Admin**. La modalità
amministratore non richiede né crea un account `ADMIN` e deve essere usata
soltanto in un contesto fidato; la sessione utente non contiene ruoli o
meccanismi di auto-promozione.

## Test e build

Per eseguire la suite automatica e la build completa:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat smokeApp
```

La suite usa JUnit 5 e un database H2 in-memory distinto per ogni test, avviato
in modalità di compatibilità MySQL. Lo schema di test contiene le stesse 14
tabelle ma non contiene `DROP DATABASE`, `USE` o `TRUNCATE`: non può quindi
toccare il database di sviluppo. H2 verifica workflow, transazioni, query,
vincoli e concorrenza applicativa; la compatibilità finale con le peculiarità
del server MySQL va comunque controllata con
`checkDatabaseConnection` e un'istanza MySQL usa-e-getta.

Non vanno mai eseguiti `schema.sql`, `reset.sql`, `DROP DATABASE` o
`TRUNCATE` contro l'URL di sviluppo configurato durante i test.

Per una verifica manuale non distruttiva dei dati dimostrativi, eseguire
`src/main/resources/db/test.sql` dopo il seed.
