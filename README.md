# tapestry-jpa-transactions
Advanced transaction support for tapestry JPA applications

This library extends functionality of official [`tapestry-jpa` integration](https://tapestry.apache.org/integrating-with-jpa.html).

# How to use (basics)

Choose a maven repository, private or public, and add the `nexusUsername`, `nexusPassword`, `nexusRepository` and `nexusSnapshotReository` values to your gradle.properties file

Then add the library as a dependency to your Tapestry5 project:

    compile 'net.satago:tapestry-jpa-transactions:1.0.3'

This library contains a [drop-in tapestry module](https://tapestry.apache.org/autoloading-modules.html), so you don't need to do anything else to make it work, except for when you need simple CDI support for JPA 2.1 entity listeners (read below).

# How it works
This library provides simple abstraction for code that needs to be run in transaction:

Consider this simple example:

```java
@Inject
private TransactionalUnits transactionalUnits;

@Inject
private UserDAO userDAO;

public User updateUser(final User user)
{
  return transactionalUnits.invokeInTransaction(new Invokable<User>()
  {
      public User invoke()
      {
          return userDAO.merge(user);
      }
  });
}
```

Here an instance of `org.apache.tapestry5.ioc.Invokable` - a _"unit"_ - will be wrapped with transaction.

Using `Invokable` you may return a value from a unit, if you don't need to return anything you may use `TransactionalUnits#runInTransaction(Runnable)`.

Transaction will be committed automatically after run.

## Nested transactional units
It is possible to nest transactional units.

Only top-most unit will trigger transaction commit.

In case if any nested transaction will be detected you will see `WARN`-level message in the log:

    Nested transaction detected, current depth = 2

with a stacktrace to this call.

You can disable this message by changing severity level for logger `net.satago.tapestry5.jpa.TransactionalUnit` to `ERROR`.

## `@CommitAfter`
This library overrides default behavior of [the `@CommitAfter` anotation](https://tapestry.apache.org/integrating-with-jpa.html#IntegratingwithJPA-Transactionmanagement).

Effectively, methods marked by `@CommitAfter` will now be invoked as transactional units.

Main difference here is that in case of nested method calls transaction will only be committed once -- at the top level of call hierarchy.

## Commit hooks
You may register commit hooks to be triggered right before or after transaction commit.

You may do this when creating a unit using builders from `TransactionalUnits.prepareRun` or `TransactionalUnits.prepareInvoke`:

```java
@Test
public void testBeforeCommit()
{
    transactional.prepareRun(new Runnable()
    {
        @Override
        public void run()
        {
            //  Change database state in a test,
            //  it will be rolled back at the end
        }
    }).beforeCommit(rollback()).runInTransaction();
}

private Invokable<Boolean> rollback()
{
    return new Invokable<Boolean>()
    {
        @Override
        public Boolean invoke()
        {
            // Rollback transaction
            return false;
        }
    };
}
```

You may also want to register a callback hook from inside the transactional unit without having access to the builder. Here's how you do this:

```java
//  ... somewhere inside the transactional unit

TransactionalUnit.registerAfterCommit(new Runnable()
{
    @Override
    public void run()
    {
        // Transaction just committed
    }
});
```

## Simple CDI support for JPA 2.1 entity listeners
Since version 2.1 of JPA it is possible to [`@Inject` CDI beans into entity listeners](http://hantsy.blogspot.ru/2013/12/jpa-21-cdi-support.html).

This library provides simple implementation of `javax.enterprise.inject.spi.BeanManager` that allows `@Inject`ing tapestry-ioc services into JPA entity listeners.

You need to configure your persistence unit to make it work.

Here's an example with Hibernate as a JPA provider:

```java
public PersistenceUnitConfigurer buildSatagoPUConfigurer(final ObjectLocator objectLocator)
{
    return new PersistenceUnitConfigurer()
    {
        public void configure(TapestryPersistenceUnitInfo unitInfo)
        {
            unitInfo.getProperties().put(
                    org.hibernate.jpa.AvailableSettings.CDI_BEAN_MANAGER,
                    objectLocator.autobuild(TapestryCDIBeanManagerForJPAEntityListeners.class));
        }
    };
}

@Contribute(EntityManagerSource.class)
public static void configurePersistenceUnitInfos(
        MappedConfiguration<String, PersistenceUnitConfigurer> cfg,
        @Service("SatagoPUConfigurer") PersistenceUnitConfigurer configurer)
{
    cfg.add("satago-pu", configurer);
}
```

# Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. *Create New Pull Request*
