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

import static org.testng.AssertJUnit.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import net.satago.tapestry5.jpa.TransactionalUnitsModule;
import net.satago.tapestry5.jpa.test.entities.ThingOne;
import net.satago.tapestry5.jpa.test.entities.ThingTwo;

import org.apache.tapestry5.internal.test.PageTesterContext;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.apache.tapestry5.jpa.EntityManagerManager;
import org.apache.tapestry5.jpa.modules.JpaModule;
import org.apache.tapestry5.modules.TapestryModule;
import org.apache.tapestry5.services.ApplicationGlobals;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class JpaTest
{

	private static Registry registry;
	private EntityManagerManager entityManagerManager;
	private TopLevelService topLevelService;

	@BeforeSuite
	public final void setupRegistry()
	{
		RegistryBuilder builder = new RegistryBuilder();
		builder.add(TapestryModule.class);
		builder.add(JpaModule.class);
		builder.add(TransactionalUnitsModule.class);
		builder.add(JpaTestModule.class);

		registry = builder.build();
		// set PageTesterContext, otherwise T5 tries to load classpath assets
		ApplicationGlobals globals = registry.getObject(ApplicationGlobals.class, null);
		globals.storeContext(new PageTesterContext(""));
		registry.performRegistryStartup();

		entityManagerManager = registry.getService(EntityManagerManager.class);
		topLevelService = registry.getService(TopLevelService.class);

	}

	private EntityManager getEntityManager() {
		return entityManagerManager.getEntityManagers().values().iterator().next();
	}

	@AfterSuite
	public final void shutdownRegistry()
	{
		registry.shutdown();
		registry = null;
	}

	@BeforeMethod
	public final void beginTransaction()
	{
		EntityTransaction tx = getEntityManager().getTransaction();
		if (!tx.isActive()) tx.begin();
	}

	@AfterMethod
	public void rollbackLastTransactionAndClean() throws SQLException {
		EntityTransaction transaction = getEntityManager().getTransaction();
		if (transaction.isActive()) transaction.rollback();
		clearDatabase();
		getEntityManager().clear();
	}

	// based on http://www.objectpartners.com/2010/11/09/unit-testing-your-persistence-tier-code/
	private void clearDatabase() throws SQLException {
		EntityManager em = getEntityManager();
		EntityTransaction transaction = em.getTransaction();
		if (!transaction.isActive()) transaction.begin();
		Connection c = em.unwrap(Connection.class);
		Statement s = c.createStatement();
		s.execute("SET REFERENTIAL_INTEGRITY FALSE");
		Set<String> tables = new HashSet<String>();
		ResultSet rs = s.executeQuery("select table_name " + "from INFORMATION_SCHEMA.tables "
			+ "where table_type='TABLE' and table_schema='PUBLIC'");
		while (rs.next()) {
			// if we don't skip over the sequence table, we'll start getting "The sequence table information is not complete"
			// exceptions
			if (!rs.getString(1).startsWith("DUAL_") && !rs.getString(1).equals("SEQUENCE")) {
				tables.add(rs.getString(1));
			}
		}
		rs.close();
		for (String table : tables) {
			s.executeUpdate("DELETE FROM " + table);
		}
		transaction.commit();
		s.execute("SET REFERENTIAL_INTEGRITY TRUE");
		s.close();
	}

	private <T> List<T> getInstances(final Class<T> type) {
		EntityManager em = getEntityManager();
		CriteriaBuilder qb = em.getCriteriaBuilder();
		CriteriaQuery<T> query = qb.createQuery(type);
		return em.createQuery(query).getResultList();
	}

	@Test
	public void commitBothInNestedTransaction() {
		topLevelService.createThingOneAndTwo("one", "two");
		assertEquals(1, getInstances(ThingOne.class).size());
		assertEquals(1, getInstances(ThingTwo.class).size());
	}

	@Test(expectedExceptions = RollbackException.class)
	public void rollbackTopFails() {
		topLevelService.createThingOneAndTwo("one", null);
	}

	@Test(expectedExceptions = RollbackException.class)
	public void rollbackNestedFails() {
		topLevelService.createThingOneAndTwo(null, "two");
	}

	@Test
	public void sequentialCommitUsingRegisterAfterCommit() {
		topLevelService.createThingOneThenTwo("one", "two");
		// TODO how to best assert that this was the result of two separate commits?
		assertEquals(1, getInstances(ThingOne.class).size());
		assertEquals(1, getInstances(ThingTwo.class).size());
	}

	@Test
	public void sequentialRollbackAndAbortUsingRegisterAfterCommit() {
		try {
			topLevelService.createThingOneThenTwo(null, "two");
		} catch (RollbackException e) {
		}
		assertEquals(0, getInstances(ThingOne.class).size());
		assertEquals(0, getInstances(ThingTwo.class).size());
	}

}
