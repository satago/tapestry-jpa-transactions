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

import org.apache.tapestry5.internal.InternalConstants;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.services.ApplicationDefaults;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.apache.tapestry5.jpa.JpaEntityPackageManager;
import org.apache.tapestry5.jpa.JpaTransactionAdvisor;

public class JpaTestModule
{

    public static void bind(final ServiceBinder binder)
    {
        binder.bind(TopLevelService.class);
        binder.bind(NestedService.class);
    }

    @Contribute(SymbolProvider.class)
    @ApplicationDefaults
    public static void defaultsSymbols(MappedConfiguration<String, Object> configuration)
    {
        configuration.add(InternalConstants.TAPESTRY_APP_PACKAGE_PARAM, JpaTestModule.class.getPackage().getName());
        // configuration.add(InternalSymbols.APP_PACKAGE_PATH, "org/tynamo/model/jpa");
    }

    @Contribute(JpaEntityPackageManager.class)
    public static void addPackages(Configuration<String> configuration)
    {
        configuration.add(JpaTestModule.class.getPackage().getName());
    }

    @Match({ "*Service" })
    public static void adviseTransactionally(JpaTransactionAdvisor advisor, MethodAdviceReceiver receiver)
    {
        advisor.addTransactionCommitAdvice(receiver);
    }

}
