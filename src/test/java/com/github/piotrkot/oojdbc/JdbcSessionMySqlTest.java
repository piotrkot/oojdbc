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

import com.github.piotrkot.oojdbc.outcomes.StoredProcedureOutcome;
import com.github.piotrkot.oojdbc.statements.Args;
import com.github.piotrkot.oojdbc.statements.Exec;
import com.github.piotrkot.oojdbc.statements.Insert;
import com.github.piotrkot.oojdbc.statements.ProcCall;
import com.github.piotrkot.oojdbc.statements.Select;
import com.mysql.cj.jdbc.MysqlDataSource;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.DataSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;

/**
 * Integration case for {@link JdbcSession} on MySQL.
 *
 * @since 1.0
 * @checkstyle ClassDataAbstractionCoupling (2 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class JdbcSessionMySqlTest {

    /**
     * The database container.
     */
    @Rule
    public final MySQLContainer<?> mysql = new MySQLContainer<>(MySQLContainer.NAME);

    @Test
    public void worksWithExecute() throws Exception {
        final DataSource source = this.source();
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
    }

    @Test
    public void worksLastInsertId() throws Exception {
        new JdbcSession<>(
            new Exec(
                new Sql(
                    "CREATE TABLE IF NOT EXISTS foo (",
                    "id INT NOT NULL AUTO_INCREMENT, ",
                    "name VARCHAR(50), ",
                    "PRIMARY KEY (id)",
                    ")"
                )
            )
        ).using(this.source());
        MatcherAssert.assertThat(
            new JdbcSession<>(
                new Insert<>(
                    new Sql("INSERT INTO foo (name) VALUES (?)"),
                    new Args("test"),
                    Outcome.LAST_INSERT_ID
                )
            ).using(this.source()),
            Matchers.is(1L)
        );
    }

    @Test
    public void worksLastInsertIdAndTransaction() throws Exception {
        new JdbcSession<>(
            new Exec(
                new Sql(
                    "CREATE TABLE IF NOT EXISTS foo (",
                    "id INT NOT NULL AUTO_INCREMENT, ",
                    "name VARCHAR(50), ",
                    "PRIMARY KEY (id)",
                    ")"
                )
            )
        ).using(this.source());
        try {
            new JdbcSessionTx<>(
                conn -> {
                    new Exec(
                        new Sql("START TRANSACTION")
                    ).using(conn);
                    MatcherAssert.assertThat(
                        new Insert<>(
                            new Sql("INSERT INTO foo (name) VALUES (?)"),
                            new Args("test"),
                            Outcome.LAST_INSERT_ID
                        ).using(conn),
                        Matchers.is(1L)
                    );
                    throw new SQLException("forced");
                }
            ).using(this.source());
        } catch (final SQLException ignored) {
        }
        MatcherAssert.assertThat(
            new JdbcSession<>(
                new Select<>(
                    new Sql("SELECT name FROM foo WHERE name = 'foo'"),
                    Outcome.EMPTY
                )
            ).using(this.source()),
            Matchers.is(true)
        );
    }

    @Test
    public void callsFunctionWithOutParam() throws Exception {
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
                        "CREATE PROCEDURE proc(OUT username text, OUT day date) ",
                        "BEGIN SELECT name, CURDATE() INTO username, day ",
                        "FROM users; ",
                        "SELECT username, day; ",
                        "END"
                    )
                ).using(conn);
            }
        ).using(this.source());
        final Object[] result = new JdbcSession<>(
            new ProcCall<>(
                new Sql("CALL proc(?, ?)"),
                stmt -> {
                    ((CallableStatement) stmt).registerOutParameter(1, Types.VARCHAR);
                    ((CallableStatement) stmt).registerOutParameter(2, Types.DATE);
                },
                new StoredProcedureOutcome<Object[]>(1, 2)
            )
        ).using(this.source());
        MatcherAssert.assertThat(
            result,
            Matchers.arrayContaining(
                Matchers.is("Jeff Charles"),
                Matchers.notNullValue()
            )
        );
    }

    /**
     * Get data source.
     *
     * @return Source
     */
    private DataSource source() {
        final MysqlDataSource src = new MysqlDataSource();
        src.setUrl(this.mysql.getJdbcUrl());
        src.setUser(this.mysql.getUsername());
        src.setPassword(this.mysql.getPassword());
        return src;
    }
}
