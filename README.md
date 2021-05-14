
[![Build Status](https://travis-ci.org/piotrkot/oojdbc.svg?branch=master)](https://travis-ci.org/piotrkot/oojdbc)
[![Coverage Status](https://coveralls.io/repos/github/piotrkot/oojdbc/badge.svg?branch=master)](https://coveralls.io/github/piotrkot/oojdbc?branch=main)

# Object-oriented wrapper for JDBC

Simple object-oriented wrapper around JDBC. This is a fork of [jcabi-jdbc](https://jdbc.jcabi.com/).

## SQL queries

Single value queries

```java
String name = new JdbcSession<>(
    new Select<>(
        new Sql("SELECT name FROM users"),
        new SingleOutcome<>(String.class)
    )
).using(datasource);
```

Collection queries

```java
List<String> names = new JdbcSession<>(
    new Select<>(
        new Sql(
            "SELECT name FROM users",
            "WHERE age > ?"
        ),
        new Args(20),
        new ColumnOutcome<>(String.class)
    )
).using(datasource);
```

Custom collection queries

```java
List<User> kids = new JdbcSession<>(
    new Select<>(
        new Sql(
            "SELECT name, age FROM users",
            "WHERE age < ?"
        ),
        new Args(10),
        new ListOutcome<>(
            rset -> new User(
                rset.getString("name"),
                rset.getInt("age")
            );
        )
    )
).using(datasource);

@RequiredArgsConstructor
class User {
    private final String name;
    private final int age;
}
```

## Insert/Update statements

Single table inserts

```java
long id = new JdbcSession<>(
    new Insert<>(
        new Sql(
            "INSERT INTO users (name, age)",
            "VALUES (?, ?)"
        ),
        new Args("Mark", 32),
        Outcome.LAST_INSERT_ID
    )
).using(datasource);
```

Multiple (transactional) table updates

```java
new JdbcSessionTx<>(
    conn -> {
        long id = new Insert<>(
            new Sql(
                "INSERT INTO users (name, age)",
                "VALUES (?, ?)"
            ),
            new Args("Mark", 32),
            Outcome.LAST_INSERT_ID
        ).using(conn);
        return new Insert<>(
            new Sql(
                "INSERT INTO family (user)",
                "VALUES (?)"
            ),
            new Args(id),
            Outcome.VOID
        ).using(conn);
    }
).using(datasource);
```

## Other statements

Table creation

```java
new JdbcSession<>(
    new Exec(
        new Sql(
            "CREATE TABLE IF NOT EXISTS foo (name VARCHAR(50))"
        )
    )
).using(datasource);
```

Procedure creation and call

```java
final Object[] result = new JdbcSession<>(
    conn -> {
        new Exec(
            new Sql(
                "CREATE PROCEDURE proc(OUT username text, OUT day date) ",
                "BEGIN SELECT name, CURDATE() INTO username, day ",
                "FROM users; ",
                "SELECT username, day; ",
                "END"
            )
        ).using(conn);
        return new ProcCall<>(
            new Sql("CALL proc(?, ?)"),
            stmt -> {
                ((CallableStatement) stmt).registerOutParameter(1, Types.VARCHAR);
                ((CallableStatement) stmt).registerOutParameter(2, Types.DATE);
            },
            new StoredProcedureOutcome<Object[]>(1, 2)
        ).using(conn);
    }
).using(datasource);
```

Please, note the library is still in early development and it's API can
frequently change.

To get started, add dependency to your project:
```xml
<dependency>
    <groupId>com.github.piotrkot</groupId>
    <artifactId>oojdbc</artifactId>
    <version>1.2</version>
</dependency>
```

Feel free to fork me on GitHub, report bugs or post comments.

For Pull Requests, please run `mvn clean package`, first.

Tests depend on [testcontainers](https://www.testcontainers.org/). To successfully
run them, you must have Docker installed. Since the Docker daemon always runs as
the root user it may be tempting to create and give access to the 'docker' group.
This however, may give [permanent and non-password protected root access](https://fosterelli.co/privilege-escalation-via-docker.html).
Instead, it's better to run `sudo mvn clean package -Dmaven.repo.local=/home/user/.m2/repository`. 
