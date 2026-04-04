package hp.soft.config;

import io.r2dbc.spi.ConnectionFactory;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.transaction.ReactiveTransactionManager;

@Configuration
public class JooqConfig {

    @Bean
    public DSLContext dslContext(ConnectionFactory connectionFactory) {
        Settings settings = new Settings()
                .withRenderNameCase(RenderNameCase.LOWER);

        return DSL.using(
                new TransactionAwareConnectionFactoryProxy(connectionFactory),
                SQLDialect.POSTGRES,
                settings
        );
    }

    @Bean
    @Primary
    public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}
