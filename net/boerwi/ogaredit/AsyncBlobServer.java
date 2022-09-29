package net.boerwi.ogaredit;

// This isn't part of OE_BlobMgr because you may want to have multiple servers (serving multiple files concurrently), rather than just one.
// This has the advantage of invalidating intermediate requests. For example, if you request five blobs in quick succession, then this may, for example, only return the first and the final blob
interface AsyncBlobServer{
	// This is _not_ guaranteed to return the blob to the blob waiter, if it gets called when another blob is being fetched, and this function gets called again before it finishes fetching
	void getBlobDataAsync(long id, AsyncBlobWaiter waiter);
}
