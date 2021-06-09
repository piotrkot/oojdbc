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
package com.github.piotrkot.oojdbc.statements;

import com.github.piotrkot.oojdbc.Preparation;
import com.github.piotrkot.oojdbc.Utc;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import lombok.RequiredArgsConstructor;
import org.cactoos.iterable.IterableOf;
import org.cactoos.iterable.Joined;

/**
 * Arguments to SQL.
 * @since 1.0
 */
@RequiredArgsConstructor
public final class Args implements Preparation {
    /**
     * Arguments.
     */
    private final Iterable<?> arguments;

    /**
     * Ctor.
     * @param part First argument
     * @param parts Other arguments
     */
    public Args(final Object part, final Object... parts) {
        this(
            new Joined<>(
                part, new IterableOf<>(parts)
            )
        );
    }

    /**
     * Ctor.
     */
    Args() {
        this(new IterableOf<>());
    }

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public void prepare(final PreparedStatement stmt) throws SQLException {
        int pos = 1;
        for (final Object arg : this.arguments) {
            if (arg == null) {
                stmt.setNull(pos, Types.NULL);
            } else if (arg instanceof Long) {
                stmt.setLong(pos, (Long) arg);
            } else if (arg instanceof Boolean) {
                stmt.setBoolean(pos, (Boolean) arg);
            } else if (arg instanceof Date) {
                stmt.setDate(pos, (Date) arg);
            } else if (arg instanceof Integer) {
                stmt.setInt(pos, (Integer) arg);
            } else if (arg instanceof Utc) {
                ((Utc) arg).setTimestamp(stmt, pos);
            } else if (arg instanceof Float) {
                stmt.setFloat(pos, (Float) arg);
            } else if (arg instanceof byte[]) {
                stmt.setBytes(pos, (byte[]) arg);
            } else {
                stmt.setObject(pos, arg);
            }
            ++pos;
        }
    }
}
