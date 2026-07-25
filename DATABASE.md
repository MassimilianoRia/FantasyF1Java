# Database, avvio e dati dimostrativi

## Requisiti

L'applicazione usa Java 21, JDBC diretto e MySQL 8.0.16.

## Configurazione della connessione

L'unico punto di configurazione della connessione è
`src/main/resources/database.properties`:

```properties
database.url=jdbc:mysql://localhost:3306/fantasy_f1
database.user=root
database.password=Stellarium!23
```

Per usare un altro server MySQL e sufficiente modificare questi tre valori.
L'URL deve indicare il database `fantasy_f1` creato eseguendo `schema.sql` su
quello stesso server.

## Creazione e popolamento

Prima di avviare l'applicazione, eseguire sul server configurato:

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
