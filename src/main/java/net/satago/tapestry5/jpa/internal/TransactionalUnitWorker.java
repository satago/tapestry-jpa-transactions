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

import javax.persistence.PersistenceContext;

import org.apache.tapestry5.jpa.EntityManagerManager;
import org.apache.tapestry5.jpa.annotations.CommitAfter;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticMethod;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

public class TransactionalUnitWorker implements ComponentClassTransformWorker2
{
    private final MethodAdvice shared;

    private final EntityManagerManager manager;

    public TransactionalUnitWorker(EntityManagerManager manager)
    {
        this.manager = manager;

        shared = new TransactionalUnitMethodAdvice(manager, null);
    }

    @Override
    public void transform(PlasticClass plasticClass, TransformationSupport support, MutableComponentModel model)
    {
        for (final PlasticMethod method : plasticClass
                .getMethodsWithAnnotation(CommitAfter.class))
        {
            PersistenceContext annotation = method.getAnnotation(PersistenceContext.class);

            MethodAdvice advice = annotation == null ? shared : new TransactionalUnitMethodAdvice(manager, annotation);

            method.addAdvice(advice);
        }
    }
}
