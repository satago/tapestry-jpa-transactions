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

import net.satago.tapestry5.jpa.TransactionalUnit.TransactionalUnitBuilder;

import org.apache.tapestry5.ioc.Invokable;

public interface TransactionalUnits
{

    <T> T invokeInTransaction(Invokable<T> invokable);

    void runInTransaction(Runnable runnable);

    <T> TransactionalUnitBuilder<T> prepareInvoke(Invokable<T> invokable);

    <T> TransactionalUnitBuilder<T> prepareRun(Runnable runnable);

}
