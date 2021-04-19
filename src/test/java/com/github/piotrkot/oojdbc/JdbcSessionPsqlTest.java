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

import com.github.piotrkot.oojdbc.outcomes.ColumnOutcome;
import com.github.piotrkot.oojdbc.outcomes.StoredProcedureOutcome;
import com.github.piotrkot.oojdbc.statements.Args;
import com.github.piotrkot.oojdbc.statements.Exec;
import com.github.piotrkot.oojdbc.statements.Insert;
import com.github.piotrkot.oojdbc.statements.ProcCall;
import com.github.piotrkot.oojdbc.statements.Select;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import javax.sql.DataSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration case for {@link JdbcSession}.
 * @since 1.0
 * @checkstyle ClassDataAbstractionCoupling (2 lines)
 */
public final class JdbcSessionPsqlTest {

    /**
     * The database container.
     */
    @Rule
    public final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(PostgreSQLContainer.IMAGE);

    /**
     * JdbcSession can do PostgreSQL manipulations.
     *
     * @throws Exception If there is some problem inside
     */
    @Test
    public void manipulatesPostgresql() throws Exception {
        final DataSource source = this.source();
        new JdbcSessionTx<>(
            conn -> {
                new Exec(
                    new Sql(
                        "CREATE TABLE IF NOT EXISTS foo (name VARCHAR(50))"
                    )
                ).using(conn);
                return new Insert<>(
                    new Sql(
                        "INSERT INTO foo (name) VALUES (?)"
                    ),
                    new Args(
                        "Jeff Lebowski"
                    ),
                    Outcome.VOID
                ).using(conn);
            }
        ).using(source);
    }

    /**
     * JdbcSession can change transaction isolation level.
     *
     * @throws Exception If there is some problem inside
     */
    @Test
    public void changesTransactionIsolationLevel() throws Exception {
        final DataSource source = this.source();
        new JdbcSession<>(
            new Exec(
                new Sql("VACUUM")
            )
        ).using(source);
    }

    /**
     * JdbcSession can run a function (stored procedure) with
     * output parameters.
     *
     * @throws Exception If something goes wrong
     */
    @Test
    public void callsFunctionWithOutParam() throws Exception {
        final DataSource source = this.source();
        new JdbcSessionTx<>(
            conn -> {
                new Exec(
                    new Sql("CREATE TABLE IF NOT EXISTS users (name VARCHAR(50))")
                ).using(conn);
                new Insert<>(
                    new Sql("INSERT INTO users (name) VALUES (?)"),
                    new Args("Jeff Charles"),
                    Outcome.VOID
                ).using(conn);
                return new Exec(
                    new Sql(
                        "CREATE OR REPLACE FUNCTION fetchUser(username OUT text,",
                        "day OUT date)",
                        "AS $$ BEGIN SELECT name, CURRENT_DATE INTO username, day",
                        "FROM users; END; $$ LANGUAGE plpgsql;"
                    )
                ).using(conn);
            }
        ).using(source);
        final Object[] result = new JdbcSessionTx<>(
            new ProcCall<>(
                new Sql(
                    "{call fetchUser(?, ?)}"
                ),
                stmt -> {
                    final CallableStatement cstmt = (CallableStatement) stmt;
                    cstmt.registerOutParameter(1, Types.VARCHAR);
                    cstmt.registerOutParameter(2, Types.DATE);
                },
                new StoredProcedureOutcome<Object[]>(1, 2)
            )
        ).using(source);
        MatcherAssert.assertThat(result.length, Matchers.is(2));
        MatcherAssert.assertThat(
            result[0].toString(),
            Matchers.containsString("Charles")
        );
        MatcherAssert.assertThat(
            (Date) result[1],
            Matchers.notNullValue()
        );
    }

    /**
     * JdbcSession can run a function (stored procedure) with
     * input and output parameters.
     *
     * @throws Exception If something goes wrong
     */
    @Test
    public void callsFunctionWithInOutParam() throws Exception {
        final DataSource source = this.source();
        new JdbcSessionTx<>(
            conn -> {
                new Exec(
                    new Sql(
                        "CREATE TABLE IF NOT EXISTS",
                        "usersids (id INTEGER, name VARCHAR(50))"
                    )
                ).using(conn);
                new Insert<>(
                    new Sql(
                        "INSERT INTO usersids (id, name) VALUES (?, ?)"
                    ),
                    new Args(1, "Marco Polo"),
                    Outcome.VOID
                ).using(conn);
                return new Exec(
                    new Sql(
                        "CREATE OR REPLACE FUNCTION fetchUserById(uid IN INTEGER,",
                        "usrnm OUT text) AS $$ BEGIN",
                        "SELECT name INTO usrnm FROM usersids WHERE id=uid;",
                        "END; $$ LANGUAGE plpgsql;"
                    )
                ).using(conn);
            }
        ).using(source);
        final Object[] result = new JdbcSessionTx<>(
            new ProcCall<>(
                new Sql("{call fetchUserById(?, ?)}"),
                new Args(1),
                stmt -> ((CallableStatement) stmt)
                    .registerOutParameter(2, Types.VARCHAR),
                new StoredProcedureOutcome<Object[]>(2)
            )
        ).using(source);
        MatcherAssert.assertThat(result.length, Matchers.is(1));
        MatcherAssert.assertThat(
            result[0].toString(),
            Matchers.containsString("Polo")
        );
    }

    /**
     * JdbcSession can rollback transaction on exception.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void rollbacksTransactionOnException() throws Exception {
        final DataSource source = this.source();
        new JdbcSessionTx<>(
            new Exec(
                new Sql("CREATE TABLE employee (name VARCHAR(30))")
            )
        ).using(source);
        try {
            new JdbcSessionTx<>(
                conn -> {
                    new Insert<>(
                        new Sql("INSERT INTO employee VALUES ('Jeff Igorowski')"),
                        Outcome.VOID
                    ).using(conn);
                    return new Insert<>(
                        new Sql("INSERT INTO employee VALUES (?)"),
                        new Args(this.exceptionForValue()),
                        Outcome.VOID
                    ).using(conn);
                }
            ).using(source);
            MatcherAssert.assertThat("shall not pass", false);
        } catch (final SQLException ignored) {
        }
        MatcherAssert.assertThat(
            new JdbcSessionTx<>(
                new Select<>(
                    new Sql("SELECT name FROM employee"),
                    new ColumnOutcome<>(String.class)
                )
            ).using(source),
            Matchers.empty()
        );
    }

    /**
     * Throws exception instead of providing a value.
     * @return Value.
     * @throws IOException Exception thrown.
     */
    private String exceptionForValue() throws IOException {
        throw new IOException();
    }

    /**
     * Get data source.
     *
     * @return Source
     */
    private DataSource source() {
        final HikariDataSource src = new HikariDataSource();
        src.setDriverClassName(this.postgres.getDriverClassName());
        src.setJdbcUrl(this.postgres.getJdbcUrl());
        src.setUsername(this.postgres.getUsername());
        src.setPassword(this.postgres.getPassword());
        return src;
    }

}
