package net.satago.tapestry5.jpa.test;

import javax.persistence.EntityManager;

import net.satago.tapestry5.jpa.test.entities.ThingTwo;

import org.apache.tapestry5.jpa.annotations.CommitAfter;

public class NestedServiceImpl implements NestedService {
	private final EntityManager em;

	public NestedServiceImpl(EntityManager em) {
		this.em = em;
	}

	@CommitAfter
	public void createThingTwo(String nameTwo) {
		ThingTwo thingTwo = new ThingTwo();
		thingTwo.setName(nameTwo);
		em.persist(thingTwo);
	}

}
