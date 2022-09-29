package net.boerwi.ogaredit;

import java.io.InputStream;
import java.io.IOException;
import java.sql.*;

public class OE_DBBlobMgr implements OE_BlobMgr{
	private Connection conn;
	private PreparedStatement addPrep, getDataPrep, getPrep, removePrep, addDepPrep, remDepPrep;
	private static String
				getDataPrepString = "SELECT data FROM oeblob WHERE id = ?;";
	public OE_DBBlobMgr(Connection conn){
		this.conn = conn;
		try{
			addPrep = conn.prepareStatement("INSERT INTO oeblob (id, name, data) VALUES (?, ?, ?);");
			getDataPrep = conn.prepareStatement(getDataPrepString);
			getPrep = conn.prepareStatement("SELECT id, name, LENGTH(data) AS byteLen FROM oeblob WHERE id = ?;");
			removePrep = conn.prepareStatement("DELETE FROM oeblob WHERE id = ?;");
			addDepPrep = conn.prepareStatement("INSERT INTO oeblobusage (blobid, resourceid) VALUES (?, ?);");
			remDepPrep = conn.prepareStatement("DELETE FROM oeblobusage WHERE blobid = ? AND resourceid = ?;");
		}catch(SQLException e){
			assert false : "SQL Exception while preparing statements for blob manager: "+e;
		}
	}
	public void cleanup(){
		System.out.println("Cleaning up");
	}
	public void vacuumUnused(){
		try{
			Statement stmt = conn.createStatement();
			ResultSet res = stmt.executeQuery("SELECT id FROM oeblob WHERE id NOT IN (SELECT blobid FROM oeblobusage);");
			while(res.next()){
				long id = res.getLong("id");
				System.out.println("Removing unused: "+getBlob(id));
				removeBlob(id);
			}
			stmt.close();
		}catch(SQLException e){
			assert false : "Failed to get unused blobs";
		}
	}
	void addDep(long blobid, long resid){
		try{
			addDepPrep.setLong(1, blobid);
			addDepPrep.setLong(2, resid);
			addDepPrep.execute();
		}catch(SQLException e){
			// We aren't worried about this.
		}
	}
	void remDep(long blobid, long resid){
		try{
			remDepPrep.setLong(1, blobid);
			remDepPrep.setLong(2, resid);
			remDepPrep.execute();
		}catch(SQLException e){
			assert false : "Failed to add blob dependency";
		}
	}
	public long addBlob(InputStream data, String name){
		long id = OE_Resource.uidGenerator.next();
		try{
			addPrep.setLong(1, id);
			addPrep.setString(2, name);
			// JDBC allows for passing InputStream (which is what we want), but sqlite-jdbc does not.
			// addPrep.setBlob(3, data);
			try{
				addPrep.setBytes(3, data.readAllBytes());
			}catch(IOException e){
				assert false : "IOException while adding blob: "+e;
				return -500;
			}
		}catch(SQLException e){
			assert false : "SQL Exception while preparing to add blob: "+e;
			return -501;
		}
		try{
			addPrep.execute();
		}catch(SQLException e){
			assert false : "SQL Exception while adding blob: "+e+" "+e.getMessage();
			return -502;
		}
		return id;
	}
	public void removeBlob(long id){
		try{
			removePrep.setLong(1, id);
			removePrep.execute();
		}catch(SQLException e){
			assert false : "SQL Exception while removing blob #"+id+": "+e;
		}
	}
	public OE_Blob getBlob(long id){
		if(id == -1) return null;
		System.out.println(id);
		OE_Blob ret = null;
		try{
			getPrep.setLong(1, id);
			ResultSet res = getPrep.executeQuery();
			// test if the blob exists
			if(res.next()){
				ret = new DBBlob(res.getLong("id"), res.getString("name"), res.getLong("byteLen"), this);
			}else{
				System.out.println("Missing blob: "+id);
			}
			res.close();
		}catch(SQLException e){
			assert false : "SQL Exception: "+e;
		}
		return ret;
	}
	public class AsyncBlobGetter implements AsyncBlobServer{
		class GetterThread extends Thread{
			long id;
			AsyncBlobWaiter waiter;
			AsyncBlobGetter parent;
			GetterThread(long id, AsyncBlobWaiter waiter){
				this.id = id;
				this.waiter = waiter;
				this.parent = parent;
			}
			public void run(){
				byte[] ret = null;
				try{
					PreparedStatement getAsyncDataPrep = conn.prepareStatement(getDataPrepString);
					getAsyncDataPrep.setLong(1, id);
					ResultSet res = getAsyncDataPrep.executeQuery();
					ret = res.getBinaryStream("data").readAllBytes();
					res.close();
				}catch(SQLException e){
					assert false : "SQL Exception: "+e;
				}catch(IOException e){
					assert false : "IOException: "+e;
				}
				done(ret, id, waiter);
			}
		}
		
		long nextId;
		AsyncBlobWaiter nextWaiter = null;
		Object dataReceptionLock = new Object();
		public void getBlobDataAsync(long id, AsyncBlobWaiter waiter){
			synchronized(this){
				assert waiter != null : "AsyncBlobWaiter cannot be null";
				boolean ongoingFetch = (nextWaiter != null);
				nextId = id;
				nextWaiter = waiter;
				if(!ongoingFetch){
					(new GetterThread(nextId, nextWaiter)).start();
				}
			}
		}
		private void done(byte[] result, long forId, AsyncBlobWaiter forWaiter){
			// This outer synchronizer prevents multiple (potentially heavy) receiveBlobDatas from running concurrently. We delay determining if we are late until the last data reception is complete
			synchronized(dataReceptionLock){
				// Is this result what the most recent request was for?
				boolean isGood = false;
				synchronized(this){
					if(forId == nextId && forWaiter == nextWaiter){
						nextWaiter = null;
						isGood = true;
					}else{
						// our result is out-of-date. Start fetching a new result
						(new GetterThread(nextId, nextWaiter)).start();
					}
				}
				if(isGood){
					forWaiter.receiveBlobData(result);
				}
			}
		}
	}
	public AsyncBlobServer getAsyncBlobServer(){
		return new AsyncBlobGetter();
	}
	public InputStream getDataFor(OE_Blob target){
		assert target instanceof DBBlob && ((DBBlob)target).getOwner() == this;
		InputStream ret = null;
		try{
			getDataPrep.setLong(1, target.getId());
			ResultSet res = getDataPrep.executeQuery();
			ret = res.getBinaryStream("data");
			res.close();
		}catch(SQLException e){
			assert false : "SQL Exception: "+e;
		}
		return ret;
	}
}
class DBBlob implements OE_Blob{
	private long id;
	private String name;
	private long byteLen;
	private OE_DBBlobMgr owner;
	DBBlob(long id, String name, long byteLen, OE_DBBlobMgr owner){
		this.id = id;
		this.name = name;
		this.byteLen = byteLen;
		this.owner = owner;
	}
	public long getId(){
		return id;
	}
	public String getName(){
		return name;
	}
	public void addDep(OE_Resource res){
		owner.addDep(id, res.getId());
	}
	public void remDep(OE_Resource res){
		owner.remDep(id, res.getId());
	}
	public long getByteLen(){
		return byteLen;
	}
	public InputStream getData(){
		return owner.getDataFor(this);
	}
	public OE_DBBlobMgr getOwner(){
		return owner;
	}
	public String toString(){
		return String.format("DBBlob[%d, %s, %d bytes]", id, name, byteLen);
	}
	public boolean equals(DBBlob obj){
		return obj.id == id && obj.owner == owner;
	}
	public int hashCode(){
		return (int)id;
	}
}
