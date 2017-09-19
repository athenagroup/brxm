/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import javax.jcr.Repository;
import javax.sql.DataSource;

import org.apache.jackrabbit.core.util.db.ConnectionHelperDataSourceAccessor;
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.lock.LockException;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.repository.journal.JournalConnectionHelperAccessor;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.lock.InternalLockManager;
import org.onehippo.repository.lock.MutableLock;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.lock.db.DbLockManager.CREATE_STATEMENT;
import static org.onehippo.repository.lock.db.DbLockManager.SELECT_STATEMENT;
import static org.onehippo.repository.lock.db.DbLockManager.TABLE_NAME_LOCK;

public class LockManagerTest extends RepositoryTestCase {

    private InternalLockManager lockManager;
    // dataSource is not null in case of cluster Db test
    private DataSource dataSource;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        lockManager = (InternalLockManager)HippoServiceRegistry.getService(LockManager.class);

        Repository repository = server.getRepository();
        if (repository instanceof RepositoryDecorator) {
            repository = RepositoryDecorator.unwrap(repository);
        }
        if (repository instanceof RepositoryImpl) {
            JournalConnectionHelperAccessor journalConnectionHelperAccessor = ((RepositoryImpl)repository).getJournalConnectionHelperAccessor();
            if (journalConnectionHelperAccessor.getConnectionHelper() != null) {
                // running a cluster db test
                dataSource = ConnectionHelperDataSourceAccessor.getDataSource(journalConnectionHelperAccessor.getConnectionHelper());
            }
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {

        lockManager.clear();

        // DELETE ALL ROWS if there are any present
        if (dataSource != null) {
            Connection connection = null;
            boolean originalAutoCommit = false;
            try {
                connection = dataSource.getConnection();
                originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(true);
                final PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM hippo_lock");
                deleteStatement.execute();

            } catch (SQLException e) {
                fail("Failed to delete rows : " + e.toString());
            } finally {
                close(connection, originalAutoCommit);
            }
        }
        super.tearDown();
    }

    private void close(final Connection connection, final boolean originalAutoCommit)  {
        if (connection == null) {
            return;
        }
        try {
            connection.setAutoCommit(originalAutoCommit);
            connection.close();
        } catch (SQLException e) {
            log.error("Failed to close connection.", e);
        }
    }

    @Test
    public void general_single_threaded_lock_interaction() throws Exception {
        final String key = "123";
        lockManager.lock(key);

        dbRowAssertion(key, "RUNNING");

        lockManager.lock(key);

        dbRowAssertion(key, "RUNNING");

        assertEquals(1, lockManager.getLocks().size());
        assertEquals(key, lockManager.getLocks().iterator().next().getLockKey());
        assertEquals(Thread.currentThread().getName(), lockManager.getLocks().iterator().next().getLockThread());

        assertEquals(2, ((MutableLock)lockManager.getLocks().iterator().next()).getHoldCount());

        lockManager.unlock(key);

        assertEquals(1, lockManager.getLocks().size());
        assertEquals(1, ((MutableLock)lockManager.getLocks().iterator().next()).getHoldCount());

        dbRowAssertion(key, "RUNNING");

        lockManager.unlock(key);
        assertEquals(0, lockManager.getLocks().size());
        
        assertDbRowDoesExist(key);
        dbRowAssertion(key, "FREE");

        // now we should be able to lock again
        lockManager.lock(key);
        dbRowAssertion(key, "RUNNING");
    }
    
    private void dbRowAssertion(final String key, final String expectedStatus) throws SQLException {
        if (dataSource == null) {
            // not a clustered db test
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            final PreparedStatement selectStatement = connection.prepareStatement(SELECT_STATEMENT);
            selectStatement.setString(1, key);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                String status = resultSet.getString("status");
                assertEquals(expectedStatus, status);
            } else {
                fail(String.format("A row with lockKey '%s' should exist", key));
            }
        }
    }

    private void assertDbRowDoesExist(final String key) throws SQLException {
        if (dataSource == null) {
            // not a clustered db test
            return;
        }
        final String selectStatement = "SELECT * FROM " + TABLE_NAME_LOCK + " WHERE lockKey=?";
        try (Connection connection = dataSource.getConnection()) {
            final PreparedStatement preparedSelectStatement = connection.prepareStatement(selectStatement);
            preparedSelectStatement.setString(1, key);
            preparedSelectStatement.setQueryTimeout(10);
            ResultSet resultSet = preparedSelectStatement.executeQuery();
            assertTrue(String.format("There should be a database row for ", key), resultSet.next());
        }
    }

    @Test
    public void same_thread_can_unlock_() throws Exception {
        final String key = "123";
        lockManager.lock(key);
        dbRowAssertion(key, "RUNNING");
        lockManager.unlock(key);
        dbRowAssertion(key, "FREE");
        assertDbRowDoesExist(key);
        assertEquals(0, lockManager.getLocks().size());
    }

    @Test
    public void other_thread_cannot_unlock_() throws Exception {
        final String key = "123";
        lockManager.lock(key);
        dbRowAssertion(key, "RUNNING");
        Thread lockThread = new Thread(() -> {
            try {
                lockManager.unlock(key);
            } catch (LockException e) {
                // expected
                try {
                    dbRowAssertion(key, "RUNNING");
                } catch (SQLException e1) {
                    fail(e1.toString());
                }
            }
        });

        lockThread.start();
        lockThread.join();
        assertEquals(1, lockManager.getLocks().size());
        dbRowAssertion(key, "RUNNING");
        lockManager.unlock(key);
        dbRowAssertion(key, "FREE");
    }

    @Test
    public void when_other_thread_contains_lock_a_lock_exception_is_thrown_on_lock_attempt() throws Exception {
        final String key = "123";
        lockManager.lock(key);
        dbRowAssertion(key, "RUNNING");
        try {
            newSingleThreadExecutor().submit(() -> {
                lockManager.lock(key);
                return true;
            }).get();
            fail("ExecutionException excpected");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof LockException);
            dbRowAssertion(key, "RUNNING");
            lockManager.unlock(key);
            dbRowAssertion(key, "FREE");
        }
    }

    class LockRunnable implements Runnable {

        private String key;
        private volatile boolean keepAlive;

        LockRunnable(final String key , final boolean keepAlive) {
            this.key = key;
            this.keepAlive = keepAlive;
        }

        @Override
        public void run() {
            try {
                lockManager.lock(key);
                while (keepAlive) {
                    Thread.sleep(25);
                }
            } catch (LockException | InterruptedException e) {
                try {
                    dbRowAssertion(key, "RUNNING");
                } catch (SQLException e1) {
                    fail(e1.toString());
                }
                fail(e.toString());
            }
        }
    }

    @Test
    public void when_other_thread_contains_lock_it_cannot_be_unlocked_by_other_thread() throws Exception {
        final String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true);
        final Thread lockThread = new Thread(runnable);

        lockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        try {
            lockManager.unlock(key);
            fail("Main thread should not be able to unlock");
        } catch (LockException e) {
            dbRowAssertion(key, "RUNNING");
            // expected
        }

        runnable.keepAlive = false;

        // after the thread is finished, the lock manager should have no locks any more
        lockThread.join();


    }

    @Test
    public void when_thread_containing_lock_has_ended_without_unlocking_the_lock_can_be_reclaimed_by_another_thread() throws Exception {
        final String key = "123";
        final LockRunnable runnable = new LockRunnable(key, true);
        final Thread lockThread = new Thread(runnable);

        lockThread.start();
        // give lockThread time to lock
        Thread.sleep(100);

        try {
            lockManager.lock(key);
            fail("Other thread should have the lock");
        } catch (LockException e) {
            dbRowAssertion(key, "RUNNING");
        }

        assertEquals(key, lockManager.getLocks().iterator().next().getLockKey());
        assertEquals(lockThread.getName(), lockManager.getLocks().iterator().next().getLockThread());

        runnable.keepAlive = false;
        lockThread.join();

        assertEquals(0, lockManager.getLocks().size());
        dbRowAssertion(key, "FREE");
        // main thread can lock again
        lockManager.lock(key);
        assertEquals(key, lockManager.getLocks().iterator().next().getLockKey());
        assertEquals(Thread.currentThread().getName(), lockManager.getLocks().iterator().next().getLockThread());
    }


    @Test
    public void assert_clear_database_lock_manager_only_frees_locked_rows_for_which_it_is_the_owner() throws Exception {
        lockManager.lock("a");
        lockManager.lock("b");
        // insert manually non-owned rows

        addManualLockToDatabase("c", "otherNode", "otherThreadName", 60);
        addManualLockToDatabase("d", "otherNode", "otherThreadName", 60);

        dbRowAssertion("a", "RUNNING");
        dbRowAssertion("b", "RUNNING");
        dbRowAssertion("c", "RUNNING");
        dbRowAssertion("d", "RUNNING");

        lockManager.clear();

        dbRowAssertion("a", "FREE");
        dbRowAssertion("b", "FREE");
        dbRowAssertion("c", "RUNNING");
        dbRowAssertion("d", "RUNNING");

        if (dataSource != null) {
            // rows are kept by clear (and destroy) but fields are made empty
            try (Connection connection = dataSource.getConnection()) {
                final PreparedStatement selectStatement = connection.prepareStatement(SELECT_STATEMENT);
                selectStatement.setString(1, "a");
                ResultSet resultSet = selectStatement.executeQuery();
                if (resultSet.next()) {
                    assertEquals("FREE", resultSet.getString("status"));
                    assertNull(resultSet.getString("lockOwner"));
                    assertNull(resultSet.getString("lockThread"));
                    assertEquals(0L, resultSet.getLong("lockTime"));
                    assertEquals(0L, resultSet.getLong("expirationTime"));
                } else {
                    fail(String.format("A row with lockKey '%s' should exist", "a"));
                }
            }
        }
    }

    private void addManualLockToDatabase(final String key, final String clusterNodeId,
                                         final String threadName, final int refreshRateSeconds) throws LockException {
        if (dataSource != null) {
            Connection connection = null;
            boolean originalAutoCommit = false;
            try {
                connection = dataSource.getConnection();
                originalAutoCommit = connection.getAutoCommit();

                final PreparedStatement createStatement = connection.prepareStatement(CREATE_STATEMENT);
                connection.setAutoCommit(true);
                createStatement.setString(1, key);
                createStatement.setString(2, clusterNodeId);
                createStatement.setString(3, threadName);
                long lockTime = System.currentTimeMillis();
                createStatement.setLong(4, lockTime);
                createStatement.setLong(5, refreshRateSeconds);
                createStatement.setLong(6, lockTime + refreshRateSeconds * 1000);
                try {
                    createStatement.execute();
                } catch (SQLException e) {
                    throw new LockException(String.format("Cannot create lock row for '{}'", key), e);
                }
            } catch (SQLException e) {
                fail("Failed to delete rows : " + e.toString());
            } finally {
                close(connection, originalAutoCommit);
            }
        }
    }
}
