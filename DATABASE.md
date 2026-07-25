# Database, avvio e dati dimostrativi

## Requisiti

L'applicazione usa Java 21, JDBC diretto e MySQL 8.0.16.

## Configurazione della connessione

La configurazione predefinita è:

- URL: `jdbc:mysql://localhost:3306/fantasy_f1`
- utente: `root`
- password: `Stellarium!23`

## Creazione e popolamento

Eseguire nell'ordine:

1. `src/main/resources/db/schema.sql`
2. `src/main/resources/db/seed.sql`

## Avvio

L'applicazione ha un unico punto di accesso:

```powershell
.\gradlew.bat run
```

## Test e build

Per eseguire la suite automatica e la build completa:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat smokeApp
```
