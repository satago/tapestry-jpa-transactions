package net.satago.tapestry5.jpa.test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;

import net.satago.tapestry5.jpa.TransactionalUnits;
import net.satago.tapestry5.jpa.test.entities.VersionedThing;

public class CommitCounter {

	@Inject
	private EntityManager entityManager;

    @Inject
    private TransactionalUnits transactionalUnits;

	@PostPersist
	@PostUpdate
	private void updateVersion(Object entity) {
        // EntityTransaction tx = entityManager.getTransaction();
        // if (!tx.isActive())
        // tx.begin();
        // VersionedThing versionedThing = new VersionedThing();
        // versionedThing.setId(1);
        // entityManager.persist(versionedThing);
        // tx.commit();

        transactionalUnits.runInTransaction(new Runnable()
        {
            @Override
            public void run()
            {
                VersionedThing versionedThing = new VersionedThing();
                versionedThing.setId(1);
                entityManager.merge(versionedThing);
            }
        });
	}

}
