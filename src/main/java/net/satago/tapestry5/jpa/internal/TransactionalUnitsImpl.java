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
package net.satago.tapestry5.jpa.internal;

import net.satago.tapestry5.jpa.TransactionalUnit.TransactionalUnitBuilder;
import net.satago.tapestry5.jpa.TransactionalUnits;
import org.apache.tapestry5.ioc.Invokable;
import org.apache.tapestry5.jpa.EntityManagerManager;

import javax.inject.Inject;

public class TransactionalUnitsImpl implements TransactionalUnits
{
    @Inject
    private EntityManagerManager entityManagerManager;

    @Override
    public <T> T invokeInTransaction(Invokable<T> invokable)
    {
        return prepareInvoke(invokable).invokeInTransaction();
    }

    @Override
    public <T> TransactionalUnitBuilder<T> prepareInvoke(Invokable<T> invokable)
    {
        return new TransactionalUnitBuilder<T>(entityManagerManager, invokable);
    }

    public static class VoidInvokable<T> implements Invokable<T>
    {
        private final Runnable runnable;

        public VoidInvokable(Runnable runnable)
        {
            this.runnable = runnable;
        }

        @Override
        public T invoke()
        {
            runnable.run();

            return null;
        }
    }

    @Override
    public void runInTransaction(Runnable runnable)
    {
        prepareRun(runnable).runInTransaction();
    }

    @Override
    public <T> TransactionalUnitBuilder<T> prepareRun(final Runnable runnable)
    {
        return prepareInvoke(new VoidInvokable<T>(runnable));
    }
}
