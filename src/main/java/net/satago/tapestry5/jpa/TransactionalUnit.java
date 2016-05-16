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

import net.satago.tapestry5.jpa.internal.PersistenceContextImpl;
import net.satago.tapestry5.jpa.internal.TransactionalUnitsImpl.VoidInvokable;
import org.apache.tapestry5.internal.jpa.CommitAfterMethodAdvice;
import org.apache.tapestry5.internal.jpa.JpaInternalUtils;
import org.apache.tapestry5.ioc.Invokable;
import org.apache.tapestry5.jpa.EntityManagerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import java.util.*;

/**
 * @see CommitAfterMethodAdvice
 * 
 * @author dmitrygusev
 *
 */
public class TransactionalUnit<T> implements Runnable, Invokable<T>
{
    private static final Logger logger = LoggerFactory.getLogger(TransactionalUnit.class);

    private final EntityManagerManager manager;
    private final PersistenceContext annotation;
    private final Invokable<T> invokable;

    private List<Invokable<Boolean>> beforeCommit;
    private List<Runnable> afterCommit;

    private static ThreadLocal<Stack<TransactionalUnit<?>>> currentUnit =
            new ThreadLocal<Stack<TransactionalUnit<?>>>()
            {
                @Override
                protected Stack<TransactionalUnit<?>> initialValue()
                {
                    return new Stack<TransactionalUnit<?>>();
                }
            };

    public TransactionalUnit(EntityManagerManager manager, PersistenceContext annotation, Invokable<T> invokable)
    {
        this.manager = manager;
        this.annotation = annotation;
        this.invokable = invokable;
    }

    @Override
    public void run()
    {
        invoke();
    }

    @Override
    public T invoke()
    {
        final boolean topLevel = currentUnit.get().isEmpty();

        currentUnit.get().push(this);

        if (!topLevel)
        {
            // TODO Check if this.annotation's PU name is the same as top-level's (disable the check via symbol?)

            if (logger.isWarnEnabled())
            {
                logger.warn("Nested transaction detected, current depth = " + currentUnit.get().size(), new Throwable());
            }
        }

        boolean popDone = false;

        try
        {
            final EntityTransaction transaction = getTransaction();

            if (transaction != null && !transaction.isActive())
            {
                transaction.begin();
            }

            T result = tryInvoke(transaction, invokable);

            if (topLevel)
            {
                // Success or checked exception:

                if (transaction != null && transaction.isActive())
                {
                    fireBeforeCommit(transaction);

                    //  TODO Check if some other transactional units were added from beforeCommit

                    //  In case if more units were added from beforeCommit,
                    //  or even other beforeCommit callbacks were added for current unit
                    //  they won't be invoked here, because it's too late
                }

                if (transaction != null && transaction.isActive())
                {
                    transaction.commit();

                    //  More transactional units could be added from afterCommit, so pop early
                    //  to let them become top-level units

                    pop();

                    popDone = true;

                    fireAfterCommit();
                }
            }

            return result;
        }
        finally
        {
            if (!popDone)
            {
                pop();
            }
        }
    }

    private void pop()
    {
        TransactionalUnit<?> unit = currentUnit.get().pop();

        //  If it's not a top-level unit we need to pop its beforeCommit/afterCommit
        //  callbacks and attach them to the new head-unit, because callback will only
        //  be invoked on the top-level unit

        if (!currentUnit.get().isEmpty())
        {
            TransactionalUnit topUnit = currentUnit.get().peek();

            if (unit.beforeCommit != null)
            {
                topUnit.addBeforeCommit(unit.beforeCommit);

                unit.beforeCommit.clear();
            }

            if (unit.afterCommit != null)
            {
                topUnit.addAfterCommit(unit.afterCommit);

                unit.afterCommit.clear();
            }
        }
    }

    //  TODO Deprecate in favor of a new instance method in `TransactionalUnits`
    public static void registerBeforeCommit(Invokable<Boolean> invokable)
    {
        if (currentUnit.get().isEmpty())
        {
            throw new IllegalStateException("Callback handler should be registered from inside of transactional unit");
        }

        currentUnit.get().peek().addBeforeCommit(Collections.singletonList(invokable));
    }

    //  TODO Deprecate in favor of a new instance method in `TransactionalUnits`
    public static void registerAfterCommit(Runnable runnable)
    {
        if (currentUnit.get().isEmpty())
        {
            throw new IllegalStateException("Callback handler should be registered from inside of transactional unit");
        }

        currentUnit.get().peek().addAfterCommit(Collections.singletonList(runnable));
    }

    /* default */TransactionalUnit<T> addBeforeCommit(List<Invokable<Boolean>> callbacks)
    {
        if (callbacks == null || callbacks.isEmpty())
        {
            return this;
        }

        if (beforeCommit == null)
        {
            beforeCommit = new ArrayList<Invokable<Boolean>>();
        }

        beforeCommit.addAll(callbacks);

        return this;
    }

    /* default */TransactionalUnit<T> addAfterCommit(List<Runnable> callbacks)
    {
        if (callbacks == null || callbacks.isEmpty())
        {
            return this;
        }

        if (afterCommit == null)
        {
            afterCommit = new ArrayList<Runnable>();
        }

        afterCommit.addAll(callbacks);

        return this;
    }

    private void fireAfterCommit()
    {
        if (afterCommit == null)
        {
            return;
        }

        for (Runnable runnable : afterCommit)
        {
            runnable.run();
        }
    }

    private void fireBeforeCommit(final EntityTransaction transaction)
    {
        if (beforeCommit == null)
        {
            return;
        }

        for (Invokable<Boolean> callback : beforeCommit)
        {
            Boolean beforeCommitSucceeded = tryInvoke(transaction, callback);

            // Success or checked exception:

            if (beforeCommitSucceeded != null && !beforeCommitSucceeded)
            {
                rollbackTransaction(transaction);

                // Don't invoke further callbacks
                break;
            }
        }
    }

    private static <R> R tryInvoke(final EntityTransaction transaction, Invokable<R> invokable)
            throws RuntimeException
    {
        R result;

        try
        {
            result = invokable.invoke();
        }
        catch (final RuntimeException e)
        {
            if (transaction != null && transaction.isActive())
            {
                rollbackTransaction(transaction);
            }

            throw e;
        }

        return result;
    }

    private static void rollbackTransaction(EntityTransaction transaction)
    {
        try
        {
            transaction.rollback();
        }
        catch (Exception e)
        { // Ignore
        }
    }

    private EntityTransaction getTransaction()
    {
        EntityManager em = JpaInternalUtils.getEntityManager(manager, annotation);

        if (em == null)
        {
            return null;
        }

        return em.getTransaction();
    }

    // Builder

    public static class TransactionalUnitBuilder<T>
    {
        private final EntityManagerManager entityManagerManager;
        private final Invokable<T> invokable;

        private String persistenceUnitName;

        private List<Invokable<Boolean>> beforeCommit;
        private List<Runnable> afterCommit;

        public TransactionalUnitBuilder(EntityManagerManager entityManagerManager, Invokable<T> invokable)
        {
            this.entityManagerManager = entityManagerManager;
            this.invokable = invokable;
        }

        public TransactionalUnitBuilder<T> beforeCommit(Invokable<Boolean> beforeCommit)
        {
            if (this.beforeCommit == null)
            {
                this.beforeCommit = new ArrayList<Invokable<Boolean>>();
            }

            this.beforeCommit.add(beforeCommit);

            return this;
        }

        public TransactionalUnitBuilder<T> beforeCommit(Runnable beforeCommit)
        {
            beforeCommit(new VoidInvokable<Boolean>(beforeCommit));

            return this;
        }

        public TransactionalUnitBuilder<T> afterCommit(Runnable afterCommit)
        {
            if (this.afterCommit == null)
            {
                this.afterCommit = new ArrayList<Runnable>();
            }

            this.afterCommit.add(afterCommit);

            return this;
        }

        public TransactionalUnitBuilder<T> persistenceUnitName(String persistenceUnitName)
        {
            this.persistenceUnitName = persistenceUnitName;

            return this;
        }

        public TransactionalUnit<T> build()
        {
            final PersistenceContext annotation = persistenceUnitName != null
                    ? new PersistenceContextImpl(persistenceUnitName)
                    : null;

            return new TransactionalUnit<T>(entityManagerManager, annotation, invokable)
                    .addBeforeCommit(beforeCommit)
                    .addAfterCommit(afterCommit);
        }

        public void runInTransaction()
        {
            build().run();
        }

        public T invokeInTransaction()
        {
            return build().invoke();
        }
    }
}
