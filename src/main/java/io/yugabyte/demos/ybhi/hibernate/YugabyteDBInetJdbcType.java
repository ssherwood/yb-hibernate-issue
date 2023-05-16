package io.yugabyte.demos.ybhi.hibernate;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

public class YugabyteDBInetJdbcType extends YugabyteDBPGObjectJdbcType {

    public static final YugabyteDBInetJdbcType INSTANCE = new YugabyteDBInetJdbcType();

    public YugabyteDBInetJdbcType() {
        super( "inet", SqlTypes.INET );
    }

    @Override
    protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) {
        final String host;
        if ( string == null ) {
            host = null;
        }
        else {
            // The default toString representation of the inet type adds the subnet mask
            final int slashIndex = string.lastIndexOf( '/' );
            if ( slashIndex == -1 ) {
                host = string;
            }
            else {
                host = string.substring( 0, slashIndex );
            }
        }
        return javaType.wrap( host, options );
    }
}