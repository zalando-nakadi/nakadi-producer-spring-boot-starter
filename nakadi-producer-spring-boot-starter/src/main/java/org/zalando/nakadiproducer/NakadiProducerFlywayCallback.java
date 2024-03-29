package org.zalando.nakadiproducer;

import org.flywaydb.core.api.MigrationInfo;

import java.sql.Connection;

/**
 * This is the main callback interface that should be implemented to get access to flyway lifecycle notifications.
 * Simply add code to the callback method you are interested in having.
 *
 * <p>Each callback method will run within its own transaction.</p>
 */
public interface NakadiProducerFlywayCallback {
    /**
     * Runs before the clean task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void beforeClean(Connection connection) {}

    /**
     * Runs after the clean task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void afterClean(Connection connection) {};

    /**
     * Runs before the migrate task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void beforeMigrate(Connection connection) {};

    /**
     * Runs after the migrate task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void afterMigrate(Connection connection) {}

    /**
     * Runs before the undo task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void beforeUndo(Connection connection) {}

    /**
     * Runs before each migration script is undone.
     *
     * @param connection A valid connection to the database.
     * @param info The current MigrationInfo for the migration to be undone.
     */
    default void beforeEachUndo(Connection connection, MigrationInfo info) {}

    /**
     * Runs after each migration script is undone.
     *
     * @param connection A valid connection to the database.
     * @param info The current MigrationInfo for the migration just undone.
     */
    default void afterEachUndo(Connection connection, MigrationInfo info) {}

    /**
     * Runs after the undo task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void afterUndo(Connection connection) {}

    /**
     * Runs before each migration script is executed.
     *
     * @param connection A valid connection to the database.
     * @param info The current MigrationInfo for this migration.
     */
    default void beforeEachMigrate(Connection connection, MigrationInfo info) {}

    /**
     * Runs after each migration script is executed.
     *
     * @param connection A valid connection to the database.
     * @param info The current MigrationInfo for this migration.
     */
    default  void afterEachMigrate(Connection connection, MigrationInfo info) {}

    /**
     * Runs before the validate task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void beforeValidate(Connection connection) {}

    /**
     * Runs after the validate task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void afterValidate(Connection connection) {}

    /**
     * Runs before the baseline task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void beforeBaseline(Connection connection) {}

    /**
     * Runs after the baseline task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void afterBaseline(Connection connection) {}

    /**
     * Runs before the repair task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void beforeRepair(Connection connection) {}

    /**
     * Runs after the repair task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void afterRepair(Connection connection) {}

    /**
     * Runs before the info task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void beforeInfo(Connection connection) {}

    /**
     * Runs after the info task executes.
     *
     * @param connection A valid connection to the database.
     */
    default void afterInfo(Connection connection) {}
}
