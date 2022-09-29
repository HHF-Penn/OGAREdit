package net.boerwi.ogaredit;

import java.io.InputStream;

public interface OE_Blob{
	String getName();
	long getId();
	void addDep(OE_Resource res);
	void remDep(OE_Resource res);
	long getByteLen();
	InputStream getData();
}
