/**
 * Copyright 2015 Satago Ltd.
 *
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

import javax.persistence.PersistenceContext;

import net.satago.tapestry5.jpa.TransactionalUnit;

import org.apache.tapestry5.ioc.Invokable;
import org.apache.tapestry5.jpa.EntityManagerManager;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;

public class TransactionalUnitMethodAdvice implements MethodAdvice
{
    private EntityManagerManager manager;
    private PersistenceContext context;

    public TransactionalUnitMethodAdvice(
            EntityManagerManager manager, PersistenceContext annotation)
    {
        this.manager = manager;
        this.context = annotation;
    }

    @Override
    public void advise(final MethodInvocation invocation)
    {
        new TransactionalUnit<Object>(manager, context, new Invokable<Object>()
        {
            @Override
            public Object invoke()
            {
                return invocation.proceed();
            }
        }).invoke();
    }
}
