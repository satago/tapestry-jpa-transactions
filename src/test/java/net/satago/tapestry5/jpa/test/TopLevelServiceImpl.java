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

import javax.persistence.EntityManager;

import net.satago.tapestry5.jpa.TransactionalUnit;
import net.satago.tapestry5.jpa.TransactionalUnits;
import net.satago.tapestry5.jpa.test.entities.ThingOne;

import org.apache.tapestry5.jpa.annotations.CommitAfter;

public class TopLevelServiceImpl implements TopLevelService {

	private final EntityManager em;
	private final NestedService nestedService;
	private final TransactionalUnits transactionalUnits;

	public TopLevelServiceImpl(EntityManager em, NestedService nestedService, TransactionalUnits transactionalUnits) {
		this.em = em;
		this.nestedService = nestedService;
		this.transactionalUnits = transactionalUnits;
	}

	@Override
	@CommitAfter
	public void createThingOneAndTwo(String nameOne, String nameTwo) {
		nestedService.createThingTwo(nameTwo);
		ThingOne thingOne = new ThingOne();
		thingOne.setName(nameOne);
		em.persist(thingOne);
	}

	@Override
	@CommitAfter
	public void createThingOneThenTwo(final String nameOne, final String nameTwo) {
		transactionalUnits.runInTransaction(new Runnable() {

			@Override
			public void run() {
				TransactionalUnit.registerAfterCommit(new Runnable() {
					@Override
					public void run() {
						nestedService.createThingTwo(nameTwo);
					}
				});
				ThingOne thingOne = new ThingOne();
				thingOne.setName(nameOne);
				em.persist(thingOne);

			}
		});

	}
}
