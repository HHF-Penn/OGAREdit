PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

CREATE TABLE oeversion (
 version INTEGER
);
-- Version of DB
INSERT INTO oeversion (version) VALUES (2);

CREATE TABLE oeblob (
 id INTEGER PRIMARY KEY,
 name TEXT,
 data BLOB NOT NULL
);
CREATE TABLE oeblobusage (
 blobid INTEGER,
 resourceid INTEGER,
 FOREIGN KEY (blobid) references oeblob(id),
-- FOREIGN KEY (resourceid) references oeresource(id), -- We don't enforce this because oeresources may be deleted transiently or not added on time
 PRIMARY KEY (blobid, resourceid)
);

-- Full Directory Path. Inefficient, but minimal impact
CREATE TABLE oedirectory (
 dpath TEXT PRIMARY KEY,
 rtype TEXT,
 FOREIGN KEY (rtype) REFERENCES oetype(rtype)
);

-- Default types
CREATE TABLE oetype (
 rtype TEXT PRIMARY KEY
);
INSERT INTO oetype (rtype) VALUES
 ('Image'),
 ('Text'),
 ('Label Style'),
 ('Matte Style'),
 ('Audio'),
 ('Floorplan'),
 ('Gallery');


CREATE TABLE oeresource (
 id INTEGER PRIMARY KEY,
 parent TEXT,
 rtype TEXT,
 jsondata TEXT,
 FOREIGN KEY (rtype) REFERENCES oetype(rtype),
 FOREIGN KEY (parent) REFERENCES oedirectory(dpath)
);

COMMIT;
