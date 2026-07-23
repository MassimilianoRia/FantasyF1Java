package it.unibo.fantasyf1.model.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Factory iniettabile di connessioni JDBC.
 */
@FunctionalInterface
public interface ConnectionProvider {

    Connection open() throws SQLException;
}
