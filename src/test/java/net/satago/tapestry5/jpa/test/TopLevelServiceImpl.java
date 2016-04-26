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
