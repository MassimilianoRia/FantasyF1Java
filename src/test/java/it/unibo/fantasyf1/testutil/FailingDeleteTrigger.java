package it.unibo.fantasyf1.testutil;

import java.sql.Connection;
import java.sql.SQLException;

import org.h2.api.Trigger;

/**
 * Fault injection H2 per verificare il rollback atomico di A10.
 */
public final class FailingDeleteTrigger implements Trigger {

    @Override
    public void fire(
        final Connection connection,
        final Object[] oldRow,
        final Object[] newRow
    ) throws SQLException {
        throw new SQLException("Eliminazione A10 fallita intenzionalmente");
    }
}
