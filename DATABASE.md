# Connessione al database

L'applicazione usa per impostazione predefinita:

- URL: `jdbc:mysql://localhost:3306/fantasy_f1`
- utente: `root`
- password: `Stellarium!23`

Le credenziali non vengono salvate nel repository. In PowerShell si impostano
per la sessione corrente con:

```powershell
$env:FANTASY_F1_DB_USER = "nome_utente"
$env:FANTASY_F1_DB_PASSWORD = "password"
```

Se necessario, anche l'URL puo essere sostituito:

```powershell
$env:FANTASY_F1_DB_URL = "jdbc:mysql://localhost:3306/fantasy_f1"
```

Per verificare configurazione, credenziali e presenza del database:

```powershell
.\gradlew.bat checkDatabaseConnection
```

Nel codice applicativo ogni connessione va aperta e chiusa con
try-with-resources:

```java
try (Connection connection = DatabaseConnection.open()) {
    // query SQL
}
```

In alternativa alle variabili d'ambiente si possono usare le proprieta JVM
`fantasyf1.db.url`, `fantasyf1.db.user` e `fantasyf1.db.password`.
