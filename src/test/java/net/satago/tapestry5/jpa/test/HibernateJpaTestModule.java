/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.satago.tapestry5.jpa.test;

import net.satago.tapestry5.jpa.TapestryCDIBeanManagerForJPAEntityListeners;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.jpa.EntityManagerSource;
import org.apache.tapestry5.jpa.PersistenceUnitConfigurer;
import org.apache.tapestry5.jpa.TapestryPersistenceUnitInfo;
import org.hibernate.jpa.AvailableSettings;

import javax.persistence.spi.PersistenceUnitTransactionType;

public class HibernateJpaTestModule
{

    @Contribute(EntityManagerSource.class)
    public static void configurePersistenceUnit(
            MappedConfiguration<String, PersistenceUnitConfigurer> cfg,
            final ObjectLocator objectLocator)
    {
            PersistenceUnitConfigurer configurer = new PersistenceUnitConfigurer()
        {
            @Override
            public void configure(TapestryPersistenceUnitInfo unitInfo)
            {
                unitInfo.transactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL)
                        .persistenceProviderClassName("org.hibernate.jpa.HibernatePersistenceProvider")
                        .excludeUnlistedClasses(false)
                        .addProperty("javax.persistence.jdbc.user", "sa")
                        .addProperty("javax.persistence.jdbc.password", "sa")
                        .addProperty("javax.persistence.jdbc.driver", "org.h2.Driver")
                        .addProperty("javax.persistence.jdbc.url", "jdbc:h2:mem:jpatest_hibernate")
                        .addProperty("hibernate.hbm2ddl.auto", "update")
                        .addProperty("hibernate.show_sql", "true");

                unitInfo.getProperties().put(AvailableSettings.CDI_BEAN_MANAGER,
                        objectLocator.autobuild(TapestryCDIBeanManagerForJPAEntityListeners.class));
            }
        };
        cfg.add("jpatest", configurer);
    }

}
