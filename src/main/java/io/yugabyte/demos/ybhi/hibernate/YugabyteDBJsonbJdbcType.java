package io.yugabyte.demos.ybhi.hibernate;


import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;


public class YugabyteDBJsonbJdbcType extends YugabyteDBPGObjectJdbcType {

    public static final YugabyteDBJsonbJdbcType INSTANCE = new YugabyteDBJsonbJdbcType();

    public YugabyteDBJsonbJdbcType() {
        super( "jsonb", SqlTypes.JSON );
    }

    @Override
    protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) {
        return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().fromString(
                string,
                javaType,
                options
        );
    }

    @Override
    protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
        return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().toString(
                value,
                javaType,
                options
        );
    }
}
