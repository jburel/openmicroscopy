-- Copyright (C) 2012-4 Glencoe Software, Inc. All rights reserved.
-- Use is subject to license terms supplied in LICENSE.txt
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation; either version 2 of the License, or
-- (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License along
-- with this program; if not, write to the Free Software Foundation, Inc.,
-- 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
--

---
--- OMERO5 release upgrade from OMERO5.3__0 to OMERO5.4DEV__0.
---

BEGIN;


--
-- check OMERO database version
--

CREATE OR REPLACE FUNCTION omero_assert_db_version(expected_version VARCHAR, expected_patch INTEGER) RETURNS void AS $$

DECLARE
    current_version VARCHAR;
    current_patch INTEGER;

BEGIN
    SELECT currentversion, currentpatch INTO STRICT current_version, current_patch
        FROM dbpatch ORDER BY id DESC LIMIT 1;

    IF current_version <> expected_version OR current_patch <> expected_patch THEN
        RAISE EXCEPTION 'wrong OMERO database version for this upgrade script';
    END IF;

END;$$ LANGUAGE plpgsql;

SELECT omero_assert_db_version('OMERO5.3', 0);
DROP FUNCTION omero_assert_db_version(varchar, int);


--
-- check PostgreSQL server version and database encoding
--

CREATE OR REPLACE FUNCTION db_pretty_version(version INTEGER) RETURNS TEXT AS $$

BEGIN
    RETURN (version/10000)::TEXT || '.' || ((version/100)%100)::TEXT || '.' || (version%100)::TEXT;

END;$$ LANGUAGE plpgsql;


CREATE FUNCTION assert_db_server_prerequisites(version_prereq INTEGER) RETURNS void AS $$

DECLARE
    version_num INTEGER;
    char_encoding TEXT;

BEGIN
    SELECT CAST(setting AS INTEGER) INTO STRICT version_num
        FROM pg_settings WHERE name = 'server_version_num';
    SELECT pg_encoding_to_char(encoding) INTO STRICT char_encoding
        FROM pg_database WHERE datname = current_database();

    IF version_num < version_prereq THEN
        RAISE EXCEPTION 'PostgreSQL database server version % is less than OMERO prerequisite %',
            db_pretty_version(version_num), db_pretty_version(version_prereq);
    END IF;

    IF char_encoding != 'UTF8' THEN
        RAISE EXCEPTION 'OMERO database character encoding must be UTF8, not %', char_encoding;
    ELSE
        SET client_encoding = 'UTF8';
    END IF;

END;$$ LANGUAGE plpgsql;

SELECT assert_db_server_prerequisites(90400);

DROP FUNCTION assert_db_server_prerequisites(INTEGER);
DROP FUNCTION db_pretty_version(INTEGER);


--
-- Actual upgrade
--

INSERT INTO dbpatch (currentVersion, currentPatch, previousVersion, previousPatch)
             VALUES ('OMERO5.4DEV',  0,            'OMERO5.3',      0);

-- ... up to patch 0:

INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'Chgrp';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'Chown';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'DeleteFile';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'DeleteManagedRepo';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'DeleteOwned';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'DeleteScriptRepo';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'ModifyGroup';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'ModifyGroupMembership';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'ModifyUser';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'ReadSession';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'Sudo';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'WriteFile';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'WriteManagedRepo';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'WriteOwned';
INSERT INTO adminprivilege (id, permissions, value) SELECT ome_nextval('seq_adminprivilege'), -52, 'WriteScriptRepo';


--
-- FINISHED
--

UPDATE dbpatch SET message = 'Database updated.', finished = clock_timestamp()
    WHERE id IN (SELECT id FROM dbpatch ORDER BY id DESC LIMIT 1);

SELECT E'\n\n\nYOU HAVE SUCCESSFULLY UPGRADED YOUR DATABASE TO VERSION ' ||
       currentversion || '__' || currentpatch || E'\n\n\n' AS Status FROM dbpatch
    WHERE id IN (SELECT id FROM dbpatch ORDER BY id DESC LIMIT 1);

COMMIT;
