/*
 * Copyright (c) 2012-2018, jcabi.com
 * Copyright (c) 2021, github.com/piotrkot
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.piotrkot.oojdbc;

import com.github.piotrkot.oojdbc.outcomes.ListOutcome;
import com.github.piotrkot.oojdbc.statements.Args;
import com.github.piotrkot.oojdbc.statements.Exec;
import com.github.piotrkot.oojdbc.statements.Insert;
import com.github.piotrkot.oojdbc.statements.Select;
import java.io.IOException;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test case for {@link JdbcSession}.
 * @since 1.0
 * @checkstyle ClassDataAbstractionCoupling (2 lines)
 */
public final class JdbcSessionH2Test {

    /**
     * JdbcSession can do SQL manipulations.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void sendsSqlManipulationsToJdbcDriver() throws Exception {
        final DataSource source = new H2Source("tiu78");
        new JdbcSessionTx<>(
            conn -> {
                new Exec(
                    new Sql("CREATE TABLE foo (name VARCHAR(50))")
                ).using(conn);
                return new Insert<>(
                    new Sql("INSERT INTO foo (name) VALUES (?)"),
                    new Args("Jeff Lebowski"),
                    Outcome.VOID
                ).using(conn);
            }
        ).using(source);
        final String name = new JdbcSession<>(
            new Select<>(
                new Sql("SELECT name FROM foo WHERE name = 'Jeff Lebowski'"),
                (rset, stmt) -> {
                    rset.next();
                    return rset.getString(1);
                }
            )
        ).using(source);
        MatcherAssert.assertThat(name, Matchers.startsWith("Jeff"));
    }

    /**
     * JdbcSession can execute SQL.
     * @throws Exception If there is some problem inside
     * @since 1.0
     */
    @Test
    public void executesSql() throws Exception {
        final DataSource source = new H2Source("tpl98");
        new JdbcSessionTx<>(
            conn -> {
                new Exec(
                    new Sql("CREATE TABLE foo5 (name VARCHAR(30))")
                ).using(conn);
                return new Exec(
                    new Sql("DROP TABLE foo5")
                ).using(conn);
            }
        ).using(source);
    }

    /**
     * JdbcSession can automatically commit.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void automaticallyCommitsByDefault() throws Exception {
        final DataSource source = new H2Source("tt8u");
        final String name = new JdbcSession<>(
            conn -> {
                new Exec(
                    new Sql("CREATE TABLE foo16 (name VARCHAR(50))")
                ).using(conn);
                new Insert<>(
                    new Sql("INSERT INTO foo16 (name) VALUES (?)"),
                    new Args("Walter"),
                    Outcome.VOID
                ).using(conn);
                return new Select<>(
                    new Sql("SELECT name FROM foo16 WHERE name = 'Walter'"),
                    (rset, stmt) -> {
                        rset.next();
                        return rset.getString(1);
                    }
                ).using(conn);
            }
        ).using(source);
        MatcherAssert.assertThat(name, Matchers.startsWith("Wa"));
    }

    /**
     * JdbcSession can release connections from the pool.
     * @throws Exception If there is some problem inside
     * @since 1.0
     */
    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void releasesConnectionsFromThePool() throws Exception {
        final DataSource source = new H2Source("t445p");
        new JdbcSession<>(
            new Exec(
                new Sql("CREATE TABLE foo776 (name VARCHAR(30))")
            )
        ).using(source);
        // @checkstyle MagicNumber (1 line)
        for (int idx = 0; idx < 10; ++idx) {
            new JdbcSession<>(
                new Insert<>(
                    new Sql("INSERT INTO foo776 VALUES ('hello, world!')"),
                    Outcome.VOID
                )
            ).using(source);
        }
    }

    /**
     * JdbcSession can execute SQL in parallel threads.
     * @throws Exception If there is some problem inside
     * @since 1.0
     */
    @Test
    public void executesSqlInParallelThreads() throws Exception {
        final DataSource source = new H2Source("til87");
        new JdbcSession<>(
            new Exec(
                new Sql("CREATE TABLE foo99 (name VARCHAR(30))")
            )
        ).using(source);
        this.insert(source, "foo99");
    }

    /**
     * JdbcSession can rollback transaction.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void rollbacksTransaction() throws Exception {
        final DataSource source = new H2Source("t228x");
        new JdbcSession<>(
            conn -> {
                new Exec(
                    new Sql("CREATE TABLE t228x (name VARCHAR(30))")
                ).using(conn);
                return new Insert<>(
                    new Sql("INSERT INTO t228x VALUES ('foo')"),
                    Outcome.VOID
                ).using(conn);
            }
        ).using(source);
        try {
            new JdbcSessionTx<>(
                conn -> {
                    new Insert<>(
                        new Sql("INSERT INTO t228x VALUES ('bar')"),
                        Outcome.VOID
                    ).using(conn);
                    throw new IOException();
                }
            ).using(source);
        } catch (final SQLException ignored) {
        }
        MatcherAssert.assertThat(
            new JdbcSession<>(
                new Select<>(
                    new Sql("SELECT * FROM t228x"),
                    new ListOutcome<>(rset -> rset.getString("name"))
                )
            ).using(source),
            Matchers.contains("foo")
        );
    }

    /**
     * Insert a row into a table.
     * @param src Data source
     * @param table Name of the table to INSERT into
     * @throws Exception If there is some problem inside
     * @since 1.0
     */
    private void insert(final DataSource src, final String table)
        throws Exception {
        new JdbcSession<>(
            new Insert<>(
                new Sql(
                    String.format("INSERT INTO %s VALUES ('hey')", table)
                ),
                Outcome.VOID
            )
        ).using(src);
    }

}
