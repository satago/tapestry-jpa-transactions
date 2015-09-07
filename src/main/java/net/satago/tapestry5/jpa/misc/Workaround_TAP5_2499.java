package net.satago.tapestry5.jpa.misc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.annotations.Advise;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.jpa.EntityManagerSource;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;

/**
 * Workaround for <a href="https://issues.apache.org/jira/browse/TAP5-2499">TAP5-2499</a>
 * until properly fixed in tapestry-jpa.
 *
 * <p>
 * To apply the workaround add this class as a {@link SubModule} to your application.
 *
 * @author dmitrygusev
 *
 */
public class Workaround_TAP5_2499
{
    @Advise(serviceInterface = EntityManagerSource.class)
    @Match("EntityManagerSource")
    public void synchronizeGetEntityManagerFactory(MethodAdviceReceiver receiver)
            throws NoSuchMethodException, SecurityException
    {
        MethodAdvice advice = new MethodAdvice()
        {
            private final Map<Object, Boolean> createdEMFs = new ConcurrentHashMap<>();

            @Override
            public void advise(MethodInvocation invocation)
            {
                Object persistenceUnitName = invocation.getParameter(0);

                if (!createdEMFs.containsKey(persistenceUnitName))
                {
                    synchronized (createdEMFs)
                    {
                        // This may be invoked more than once.

                        // It should be OK, because inside the real #getEntityManagerFactory
                        // there's a check that ensures that no extra EMF will be created if there's
                        // already one.

                        // Our goal here is to ensure that no more than one thread at a time
                        // can invoke underlying method if no corresponding EMF were created yet.

                        invocation.proceed();

                        createdEMFs.put(persistenceUnitName, Boolean.TRUE);
                    }
                }
                else
                {
                    invocation.proceed();
                }
            }
        };

        Class<?> intf = receiver.getInterface();
        // #create invokes #getEntityManagerFactory in its implementation
        receiver.adviseMethod(intf.getMethod("create", String.class), advice);
        receiver.adviseMethod(intf.getMethod("getEntityManagerFactory", String.class), advice);
    }
}
