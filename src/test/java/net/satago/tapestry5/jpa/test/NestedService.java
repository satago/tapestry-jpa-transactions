package net.satago.tapestry5.jpa.test;

import org.apache.tapestry5.jpa.annotations.CommitAfter;

public interface NestedService {

	@CommitAfter
	public void createThingTwo(String nameTwo);

}