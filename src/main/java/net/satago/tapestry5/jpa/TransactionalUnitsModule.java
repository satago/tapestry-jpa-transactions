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
package net.satago.tapestry5.jpa;

import net.satago.tapestry5.jpa.internal.TransactionalUnitJpaTransactionAdvisor;
import net.satago.tapestry5.jpa.internal.TransactionalUnitWorker;

import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Local;
import org.apache.tapestry5.ioc.services.ServiceOverride;
import org.apache.tapestry5.jpa.JpaTransactionAdvisor;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;

public class TransactionalUnitsModule
{

    public static void bind(ServiceBinder binder)
    {
        binder.bind(EntityTransactionManager.class);
        binder.bind(JpaTransactionAdvisor.class, TransactionalUnitJpaTransactionAdvisor.class)
                .withId(TransactionalUnitJpaTransactionAdvisor.class.getSimpleName());
    }

    @Contribute(ServiceOverride.class)
    public static void overrideJpaTransactionAdvisor(
            MappedConfiguration<Class<?>, Object> configuration,
            @Local JpaTransactionAdvisor override)
    {
        configuration.add(JpaTransactionAdvisor.class, override);
    }

    @Contribute(ComponentClassTransformWorker2.class)
    public static void provideClassTransformWorkers(OrderedConfiguration<ComponentClassTransformWorker2> configuration)
    {
        // Using TransactionalUnits for @CommitAfter gives few advantages:
        // 1. Ability to register callbacks: before commit & after successful commit
        // 2. Nested @CommitAfter annotations will be ignored,
        // i.e. transaction will only be committed around top-level annotation
        configuration.overrideInstance("JPACommitAfter", TransactionalUnitWorker.class, "after:Log");
    }

}
