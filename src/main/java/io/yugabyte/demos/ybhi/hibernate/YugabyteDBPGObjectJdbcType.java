package io.yugabyte.demos.ybhi.hibernate;


import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;

/**
 * @author Christian Beikov
 */
public abstract class YugabyteDBPGObjectJdbcType implements JdbcType {

    private static final CoreMessageLogger LOG = CoreLogging.messageLogger( YugabyteDBPGObjectJdbcType.class );
    private static final Constructor<Object> PG_OBJECT_CONSTRUCTOR;
    private static final Method TYPE_SETTER;
    private static final Method VALUE_SETTER;

    static {
        Constructor<Object> constructor = null;
        Method typeSetter = null;
        Method valueSetter = null;
        try {
            final Class<?> pgObjectClass = ReflectHelper.classForName(
                    "com.yugabyte.util.PGobject",
                    YugabyteDBPGObjectJdbcType.class
            );
            //noinspection unchecked
            constructor = (Constructor<Object>) pgObjectClass.getConstructor();
            typeSetter = ReflectHelper.setterMethodOrNull( pgObjectClass, "type", String.class );
            valueSetter = ReflectHelper.setterMethodOrNull( pgObjectClass, "value", String.class );
        }
        catch (Exception e) {
            // need to work around jboss logging dependency
            //LOG.warn( "PostgreSQL JDBC driver classes are inaccessible and thus, certain DDL types like JSONB, JSON, GEOMETRY can not be used!", e );
            System.out.println( "PostgreSQL JDBC driver classes are inaccessible and thus, certain DDL types like JSONB, JSON, GEOMETRY can not be used!" + e );
        }
        PG_OBJECT_CONSTRUCTOR = constructor;
        TYPE_SETTER = typeSetter;
        VALUE_SETTER = valueSetter;
    }

    private final String typeName;
    private final int sqlTypeCode;

    public YugabyteDBPGObjectJdbcType(String typeName, int sqlTypeCode) {
        this.typeName = typeName;
        this.sqlTypeCode = sqlTypeCode;
    }

    public static boolean isUsable() {
        return PG_OBJECT_CONSTRUCTOR != null;
    }

    @Override
    public int getJdbcTypeCode() {
        return Types.OTHER;
    }

    @Override
    public int getDefaultSqlTypeCode() {
        return sqlTypeCode;
    }

    protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) {
        return javaType.wrap( string, options );
    }

    protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
        return javaType.unwrap( value, String.class, options );
    }

    @Override
    public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
        // No literal support for now
        return null;
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
        return new BasicBinder<>( javaType, this ) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
                    throws SQLException {
                final String stringValue = ( (YugabyteDBPGObjectJdbcType) getJdbcType() ).toString(
                        value,
                        getJavaType(),
                        options
                );
                try {
                    Object holder = PG_OBJECT_CONSTRUCTOR.newInstance();
                    TYPE_SETTER.invoke( holder, typeName );
                    VALUE_SETTER.invoke( holder, stringValue );
                    st.setObject( index, holder );
                }
                catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    throw new IllegalArgumentException( e );
                }
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
                    throws SQLException {
                final String stringValue = ( (YugabyteDBPGObjectJdbcType) getJdbcType() ).toString(
                        value,
                        getJavaType(),
                        options
                );
                try {
                    Object holder = PG_OBJECT_CONSTRUCTOR.newInstance();
                    TYPE_SETTER.invoke( holder, typeName );
                    VALUE_SETTER.invoke( holder, stringValue );
                    st.setObject( name, holder );
                }
                catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    throw new IllegalArgumentException( e );
                }
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
        return new BasicExtractor<>( javaType, this ) {
            @Override
            protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
                return getObject( rs.getString( paramIndex ), options );
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                return getObject( statement.getString( index ), options );
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
                    throws SQLException {
                return getObject( statement.getString( name ), options );
            }

            private X getObject(String string, WrapperOptions options) throws SQLException {
                if ( string == null ) {
                    return null;
                }
                return ( (YugabyteDBPGObjectJdbcType) getJdbcType() ).fromString(
                        string,
                        getJavaType(),
                        options
                );
            }
        };
    }
}
