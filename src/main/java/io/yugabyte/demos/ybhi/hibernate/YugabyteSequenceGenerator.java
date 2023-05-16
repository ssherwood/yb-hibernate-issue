package io.yugabyte.demos.ybhi.hibernate;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.StandardNamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

import static org.hibernate.cfg.AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY;
import static org.hibernate.internal.log.IncubationLogger.INCUBATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;

/**
 * TODO WIP on creating a custom Sequence Generator that can span multiple sequences
 */
public class YugabyteSequenceGenerator extends SequenceStyleGenerator {

    public static final String SEQUENCE_MAX_BUCKETS = "sequence_max_buckets";

    // TODO get this working again!!!


    /**
     * Determine the name of the sequence (or table if this resolves to a physical table)
     * to use.
     * <p/>
     * Called during {@linkplain #configure configuration}.
     *
     * @param params The params supplied in the generator config (plus some standard useful extras).
     * @param dialect The dialect in effect
     * @param jdbcEnv The JdbcEnvironment
     * @return The sequence name
     */
    @SuppressWarnings("UnusedParameters")
    protected QualifiedName determineSequenceName(
            Properties params,
            Dialect dialect,
            JdbcEnvironment jdbcEnv,
            ServiceRegistry serviceRegistry) {
        final Identifier catalog = jdbcEnv.getIdentifierHelper().toIdentifier(
                ConfigurationHelper.getString( CATALOG, params )
        );
        final Identifier schema =  jdbcEnv.getIdentifierHelper().toIdentifier(
                ConfigurationHelper.getString( SCHEMA, params )
        );

        final String sequenceName = ConfigurationHelper.getString(
                SEQUENCE_PARAM,
                params,
                () -> ConfigurationHelper.getString( ALT_SEQUENCE_PARAM, params )
        );

        if ( StringHelper.isNotEmpty( sequenceName ) ) {
            // we have an explicit name, use it
            if ( sequenceName.contains( "." ) ) {
                return QualifiedNameParser.INSTANCE.parse( sequenceName );
            }
            else {
                return new QualifiedNameParser.NameParts(
                        catalog,
                        schema,
                        jdbcEnv.getIdentifierHelper().toIdentifier( sequenceName )
                );
            }
        }

        // otherwise, determine an implicit name to use
        return determineImplicitName( catalog, schema, params, serviceRegistry );
    }

    private QualifiedName determineImplicitName(
            Identifier catalog,
            Identifier schema,
            Properties params,
            ServiceRegistry serviceRegistry) {
        final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );

        final String namingStrategySetting = coalesceSuppliedValues(
                () -> {
                    final String localSetting = ConfigurationHelper.getString( ID_DB_STRUCTURE_NAMING_STRATEGY, params );
                    if ( localSetting != null ) {
                        INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
                    }
                    return localSetting;
                },
                () -> {
                    final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
                    final String globalSetting = ConfigurationHelper.getString( ID_DB_STRUCTURE_NAMING_STRATEGY, configurationService.getSettings() );
                    if ( globalSetting != null ) {
                        INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
                    }
                    return globalSetting;
                },
                StandardNamingStrategy.class::getName
        );

        final ImplicitDatabaseObjectNamingStrategy namingStrategy = strategySelector.resolveStrategy(
                ImplicitDatabaseObjectNamingStrategy.class,
                namingStrategySetting
        );

        return namingStrategy.determineSequenceName( catalog, schema, params, serviceRegistry );
    }

    /**
     * Determine the name of the sequence (or table if this resolves to a physical table)
     * to use.
     * <p/>
     * Called during {@link #configure configuration}.
     *
     * @param params  The params supplied in the generator config (plus some standard useful extras).
     * @param dialect The dialect in effect
     * @param jdbcEnv The JdbcEnvironment
     * @return The sequence name
     */
    /*
    @SuppressWarnings({"UnusedParameters", "WeakerAccess"})
    @Override
    protected QualifiedName determineSequenceName(
            Properties params,
            Dialect dialect,
            JdbcEnvironment jdbcEnv,
            ServiceRegistry serviceRegistry) {
        final String sequencePerEntitySuffix = ConfigurationHelper.getString(CONFIG_SEQUENCE_PER_ENTITY_SUFFIX, params, DEF_SEQUENCE_SUFFIX);

        String fallbackSequenceName = DEF_SEQUENCE_NAME;
        final Boolean preferGeneratorNameAsDefaultName = serviceRegistry.getService(ConfigurationService.class)
                .getSetting(AvailableSettings.PREFER_GENERATOR_NAME_AS_DEFAULT_SEQUENCE_NAME, StandardConverters.BOOLEAN, true);
        if (preferGeneratorNameAsDefaultName) {
            final String generatorName = params.getProperty(IdentifierGenerator.GENERATOR_NAME);
            if (StringHelper.isNotEmpty(generatorName)) {
                fallbackSequenceName = generatorName;
            }
        }

        // JPA_ENTITY_NAME value honors <class ... entity-name="..."> (HBM) and @Entity#name (JPA) overrides.
        final String defaultSequenceName = ConfigurationHelper.getBoolean(CONFIG_PREFER_SEQUENCE_PER_ENTITY, params, false)
                ? params.getProperty(JPA_ENTITY_NAME) + sequencePerEntitySuffix
                : fallbackSequenceName;

        String sequenceName = ConfigurationHelper.getString(SEQUENCE_PARAM, params, defaultSequenceName);

        final int maxSequenceBuckets = ConfigurationHelper.getInt(SEQUENCE_MAX_BUCKETS, params, 0);

        // if maxSequenceBuckets > 0 then update the sequence name using a random bucket from 0-maxSequenceBuckets
        if (maxSequenceBuckets > 0) {
            Random rand = new Random();
            // assumes the sequenceName has a %d in the string itself to replace with the bucket number
            sequenceName = String.format(sequenceName, rand.nextInt(maxSequenceBuckets));
        }

        if (sequenceName.contains(".")) {
            return QualifiedNameParser.INSTANCE.parse(sequenceName);
        } else {
            // todo : need to incorporate implicit catalog and schema names
            final Identifier catalog = jdbcEnv.getIdentifierHelper().toIdentifier(
                    ConfigurationHelper.getString(CATALOG, params)
            );
            final Identifier schema = jdbcEnv.getIdentifierHelper().toIdentifier(
                    ConfigurationHelper.getString(SCHEMA, params)
            );
            return new QualifiedNameParser.NameParts(
                    catalog,
                    schema,
                    jdbcEnv.getIdentifierHelper().toIdentifier(sequenceName)
            );
        }
    }

     */
}
