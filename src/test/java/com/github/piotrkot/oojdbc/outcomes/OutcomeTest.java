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
package com.github.piotrkot.oojdbc.outcomes;

import com.github.piotrkot.oojdbc.H2Source;
import com.github.piotrkot.oojdbc.JdbcSession;
import com.github.piotrkot.oojdbc.Outcome;
import com.github.piotrkot.oojdbc.Sql;
import com.github.piotrkot.oojdbc.statements.Args;
import com.github.piotrkot.oojdbc.statements.Exec;
import com.github.piotrkot.oojdbc.statements.Insert;
import com.github.piotrkot.oojdbc.statements.Update;
import javax.sql.DataSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Outcome}.
 * @since 1.0
 */
final class OutcomeTest {

    /**
     * Outcome can fetch last insert id.
     * @throws Exception If there is some problem inside
     */
    @Test
    void fetchesLastInsertId() throws Exception {
        final DataSource source = new H2Source("trrto98");
        final Long num = new JdbcSession<>(
            conn -> {
                new Exec(
                    new Sql(
                        "CREATE TABLE foo (id INT auto_increment, name VARCHAR(50))"
                    )
                ).using(conn);
                return new Insert<>(
                    new Sql("INSERT INTO foo (name) VALUES (?)"),
                    new Args("Jeff Lebowski"),
                    Outcome.LAST_INSERT_ID
                ).using(conn);
            }
        ).using(source);
        MatcherAssert.assertThat(num, Matchers.equalTo(1L));
    }

    /**
     * Outcome can count updates.
     * @throws Exception If there is some problem inside
     */
    @Test
    void countsUpdates() throws Exception {
        final DataSource source = new H2Source("xogaa98");
        final int num = new JdbcSession<>(
            conn -> {
                new Exec(
                    new Sql(
                        "CREATE TABLE boo (id INT auto_increment, name VARCHAR(50))"
                    )
                ).using(conn);
                new Insert<>(
                    new Sql("INSERT INTO boo (name) VALUES (?)"),
                    new Args("Jeff Brown"),
                    Outcome.LAST_INSERT_ID
                ).using(conn);
                return new Update<>(
                    new Sql("UPDATE boo SET name = ? WHERE id = 1"),
                    new Args("Mark Smith"),
                    Outcome.UPDATE_COUNT
                ).using(conn);
            }
        ).using(source);
        MatcherAssert.assertThat(num, Matchers.equalTo(1));
    }
}
