# Yugabyte Driver Issue(s) with Hibernate 6+

## Issue(s) with "Shadowed" PGObject

In Hibernate 6+ the default PostgreSQL dialect (org.hibernate.dialect.PostgreSQLDialect) now dynamically loads PGobject
under certain conditions (JSONB, JSON, GEOMETRY, etc.).  Because the Yugabyte JDBC Smart Driver attempts to avoid naming
collisions with the vanilla Postgres driver by "shadowing" (renaming) core driver classes, this dynamic class loading
fails.

You may see this warning during the application startup:

```
2023-05-16T11:08:03.538-04:00  WARN 358640 --- [           main] o.h.dialect.PostgreSQLPGObjectJdbcType   : PostgreSQL JDBC driver classes are inaccessible and thus, certain DDL types like JSONB, JSON, GEOMETRY can not be used!

java.lang.ClassNotFoundException: org.postgresql.util.PGobject
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641) ~[na:na]
	at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188) ~[na:na]
```

The simple workaround for this was to just include the Postgres JDBC driver along with the Yugabyte JDBC driver.

This however, fails when the application actually tries to use types just JSONB can fail:

```
2023-05-16T11:05:33.367-04:00  WARN 357789 --- [nio-8080-exec-1] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 07006
2023-05-16T11:05:33.367-04:00 ERROR 357789 --- [nio-8080-exec-1] o.h.engine.jdbc.spi.SqlExceptionHelper   : Can't infer the SQL type to use for an instance of org.postgresql.util.PGobject. Use setObject() with an explicit Types value to specify the type to use.
2023-05-16T11:05:33.374-04:00 ERROR 357789 --- [nio-8080-exec-1] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: org.springframework.dao.InvalidDataAccessResourceUsageException: could not insert: [io.yugabyte.demos.ybhi.accounts.Account]; SQL [insert into account (created_at, details, email, is_active, last_access_at, user_name, id) values (?, ?, ?, ?, ?, ?, ?)]] with root cause

com.yugabyte.util.PSQLException: Can't infer the SQL type to use for an instance of org.postgresql.util.PGobject. Use setObject() with an explicit Types value to specify the type to use.
	at com.yugabyte.jdbc.PgPreparedStatement.setObject(PgPreparedStatement.java:1005) ~[jdbc-yugabytedb-42.3.5-yb-3.jar:42.3.5-yb-3]
	at com.zaxxer.hikari.pool.HikariProxyPreparedStatement.setObject(HikariProxyPreparedStatement.java) ~[HikariCP-5.0.1.jar:na]
	at org.hibernate.dialect.PostgreSQLPGObjectJdbcType$1.doBind(PostgreSQLPGObjectJdbcType.java:113) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.type.descriptor.jdbc.BasicBinder.bind(BasicBinder.java:63) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.type.AbstractStandardBasicType.nullSafeSet(AbstractStandardBasicType.java:221) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.type.AbstractStandardBasicType.nullSafeSet(AbstractStandardBasicType.java:217) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.type.AbstractSingleColumnStandardBasicType.nullSafeSet(AbstractSingleColumnStandardBasicType.java:34) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.persister.entity.AbstractEntityPersister.dehydrate(AbstractEntityPersister.java:3252) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.persister.entity.AbstractEntityPersister.insert(AbstractEntityPersister.java:3435) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.persister.entity.AbstractEntityPersister.insert(AbstractEntityPersister.java:4073) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.action.internal.EntityInsertAction.execute(EntityInsertAction.java:103) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.engine.spi.ActionQueue.executeActions(ActionQueue.java:612) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.engine.spi.ActionQueue.lambda$executeActions$1(ActionQueue.java:483) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at java.base/java.util.LinkedHashMap.forEach(LinkedHashMap.java:721) ~[na:na]
	at org.hibernate.engine.spi.ActionQueue.executeActions(ActionQueue.java:480) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.event.internal.AbstractFlushingEventListener.performExecutions(AbstractFlushingEventListener.java:329) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.event.internal.DefaultFlushEventListener.onFlush(DefaultFlushEventListener.java:39) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.event.service.internal.EventListenerGroupImpl.fireEventOnEachListener(EventListenerGroupImpl.java:107) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.internal.SessionImpl.doFlush(SessionImpl.java:1425) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.internal.SessionImpl.managedFlush(SessionImpl.java:477) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.internal.SessionImpl.flushBeforeTransactionCompletion(SessionImpl.java:2234) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.internal.SessionImpl.beforeTransactionCompletion(SessionImpl.java:1930) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl.beforeTransactionCompletion(JdbcCoordinatorImpl.java:439) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl.beforeCompletionCallback(JdbcResourceLocalTransactionCoordinatorImpl.java:183) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl$TransactionDriverControlImpl.commit(JdbcResourceLocalTransactionCoordinatorImpl.java:281) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	at org.hibernate.engine.transaction.internal.TransactionImpl.commit(TransactionImpl.java:101) ~[hibernate-core-6.1.7.Final.jar:6.1.7.Final]
	...
```

This is caused because the Hibernate 6+ dialect is successfully loading the `org.postgresql.util.PGobject` version of
the object (and not `com.yugabyte.util.PGobject`).

This project has a proof-of-concept Yugabyte dialect for Hibernate 6+ that works to solve this problem.

## Reproduction

Start the application "as-is" and you'll see the warning `java.lang.ClassNotFoundException: org.postgresql.util.PGobject`.

If you attempt to POST a new Account to the Accounts controller:

```shell
http POST :8080/api/accounts email="samus@nitendo.com" userName="Samus Aran" details:="{\"age\":37}"
```

You will get a mapping/casting error:

```
2023-05-16T11:22:35.502-04:00 ERROR 360284 --- [nio-8080-exec-1] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: org.springframework.dao.InvalidDataAccessResourceUsageException: could not execute statement; SQL [n/a]] with root cause

com.yugabyte.util.PSQLException: ERROR: column "details" is of type jsonb but expression is of type character varying
  Hint: You will need to rewrite or cast the expression.
  Position: 104
```

This really can't be worked around as far as I have been able to tell.

## Add the Vanilla PostgreSQL JDBC Driver

In the `build.gradle` uncomment/add:

```groovy
    runtimeOnly 'org.postgresql:postgresql'
```

Update and re-run and the original warning will go away.  Additionally, you can even succefully
retrieve records, but you can't add or update rows with the JSONB colum:

```groovy
http POST :8080/api/accounts email="samus@nitendo.com" userName="Samus Aran" details:="{\"age\":37}"
```

Now returns the `Can't infer the SQL type` error.

If you change to using the PostgreSQL JDBC driver directly in the `application.yml` file, this operation works
successfully.

## Custom YugabyteDB Dialect

This project includes a conceptual YugabyteDB specific dialect.  If you revert back to using the Yugabyte JDBC
driver and uncomment the `hibernate.properties` dialect from the `application.yml` and rerun the scenarios above,
everything should complete successfully (as if just using the vanilla driver).  You can also remove the PostgreSQL
driver from the dependencies as well.