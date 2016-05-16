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

import org.apache.tapestry5.jpa.EntityManagerManager;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import java.lang.annotation.Annotation;

public class PersistenceContextImpl implements PersistenceContext
{
    /**
     * So far unitName is the only attribute of PersistenceContext used by the tapestry-jpa.
     *
     * @see org.apache.tapestry5.internal.jpa.JpaInternalUtils#getEntityManager(EntityManagerManager, PersistenceContext)
      */
    private final String unitName;

    public PersistenceContextImpl(String unitName)
    {
        this.unitName = unitName;
    }

    @Override
    public String name()
    {
        return "";
    }

    @Override
    public String unitName()
    {
        return unitName;
    }

    @Override
    public PersistenceContextType type()
    {
        return PersistenceContextType.TRANSACTION;
    }

    @Override
    public PersistenceProperty[] properties()
    {
        return new PersistenceProperty[0];
    }

    @Override
    public Class<? extends Annotation> annotationType()
    {
        return PersistenceContext.class;
    }
}
