package net.boerwi.ogaredit;

import java.io.InputStream;

public interface OE_BlobMgr{
	void cleanup();
	void vacuumUnused();
	long addBlob(InputStream data, String name);
	void removeBlob(long id);
	OE_Blob getBlob(long id);
	AsyncBlobServer getAsyncBlobServer();
}
