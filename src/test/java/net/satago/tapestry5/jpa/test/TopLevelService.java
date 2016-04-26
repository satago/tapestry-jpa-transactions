package net.satago.tapestry5.jpa.test;

import org.apache.tapestry5.jpa.annotations.CommitAfter;

public interface TopLevelService {

	@CommitAfter
	void createThingOneAndTwo(String nameOne, String nameTwo);

	void createThingOneThenTwo(final String nameOne, final String nameTwo);

}