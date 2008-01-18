#
#   $Id$
#
#   Copyright 2007 Glencoe Software, Inc. All rights reserved.
#   Use is subject to license terms supplied in LICENSE.txt
#

import exceptions
import Ice, Glacier2
import api
import model
import util
from omero_ext import pysys
import omero_Constants_ice

class client(object):
    """
    Central blitz entry point

    Typical usage includes:
    client = omero.client()    # Uses --Ice.Config argument or ICE_CONFIG variable
    """

    def __init__(self, args = pysys.argv, id = Ice.InitializationData()):

        self.ic = None
        ic = Ice.initialize(args,id)
        if not ic:
            raise ClientError("Improper initialization")
        self.of = ObjectFactory()
        self.of.registerObjectFactory(ic)
        self.ic = ic

    def __del__(self):
        if self.ic:
            try:
                self.ic.destroy()
            except (), msg:
                pysys.stderr.write("Ice exception while destroying communicator:")
                pysys.stderr.write(msg)

    def getCommunicator(self):
        return self.ic

    def getSession(self):
        return self.sf

    def getProperties(self):
        return self.ic.getProperties()

    def getProperty(self,key):
        return self.getProperties().getProperty(key)

    def createSession(self, username=None, password=None):
        if not username:
            username = self.getProperty("omero.user")
        if len(username) == 0:
            raise ClientError("No username specified")
        if not password:
            password = self.getProperty("omero.pass")
        if len(password) == 0:
            raise ClientError("No password specified")

        prx = self.ic.getDefaultRouter()
        if not prx:
            raise ClientError("No default router found.")
        router = Glacier2.RouterPrx.checkedCast(prx)
        if not router:
            raise ClientError("Error obtaining Glacier2 router.")
        session = router.createSession(username, password)
        self.sf = api.ServiceFactoryPrx.checkedCast(session)
        if not self.sf:
            raise ClientError("No session obtained.")
        return self.sf

    def sha1(self, filename):
        """
        Calculates the local sha1 for a file.
        """
        import sha
        digest = sha.new()
        file = open(filename, 'rb')
        try:
            while True:
                block = file.read(1024)
                if not block:
                    break
                digest.update(block)
        finally:
            file.close()
        return digest.hexdigest()

    def upload(self, filename, name = None, path = None, type = None, ofile = None):
        """
        Utility method to upload a file to the server.
        """
        if not self.sf:
            raise ClientError("No session. Use createSession first.")

        import os, types
        if not filename or not isinstance(filename, types.StringType):
            raise ClientError("Non-null filename must be provided")

        if not os.path.exists(filename):
            raise ClientError("File does not exist: " + filename)

        file = open(filename, 'rb')
        try:
            from omero_model_OriginalFileI import OriginalFileI
            from omero_model_FormatI import FormatI
            import omero

            if not ofile:
                ofile = OriginalFileI()

            ofile.size = omero.RLong(os.path.getsize(file.name))
            ofile.sha1 = omero.RString(self.sha1(file.name))

            if not ofile.name:
               ofile.name = omero.RString(file.name)

            if not ofile.path:
               ofile.path = omero.RString(os.path.abspath(file.name))

            if not ofile.format:
                if not type:
                    # ofile.format = FormatI("unknown")
                    # Or determine type from file ending
                    raise ClientError("no format given")
                else:
                    ofile.format = FormatI(type)

            up = self.sf.getUpdateService()
            ofile = up.saveAndReturnObject(ofile)

            prx = self.sf.createRawFileServie()
            prx.setFileId(ofile.id.val)
            offset = 0
            while True:
                block = file.read(1024)
                if not block:
                    break
                prx.write(offset, block)
                offset += len(block)
            prx.close()
        finally:
            file.close()

        return ofile

    def closeSession(self):
        # If 'sf' does not exist we don't have a session at all
        if not hasattr(self, 'sf'):
            return
        # But even if we do have 'sf', the connection may have been lost and 'close' will fail
        try:
            self.sf.close()
        except:
            pass
        # Now destroy the actual session, which will always trigger an exception, regardless of
        # actually being connected or not
        prx = self.ic.getDefaultRouter()
        router = Glacier2.RouterPrx.checkedCast(prx)
        try:
            router.destroySession()
        except Ice.ConnectionLostException:
            pass

def script(name, description = None, **kwargs):
    """
    Entry point for all script engine scripts.

    Typical usage consists of:

      client = omero.script("name","description", param1="long", param2="bool")

    where the returned client is created via the empty constructor to omero.client
    using only --Ice.Config or ICE_CONFIG, and the function arguments are taken
    as metdata about the current script. With this information, all script
    consumers should be able to determine the required types for execution.

    Possible types are all those defined in the blitz slice definitions, including:

      * long
      * int
      * bool
      * string
      * omero::model::<Type>

    Any type suffixed with "*" is optional and can be safely left null.

    """
    c = client()
    if "true" == c.getProperty("omero.script.parse"): # Add to omero/Constants.ice
        print "Name:       ", name
        print "Description:", description
        print "Parameters:\n",kwargs
        return kwargs
    else:
        return c

import util.FactoryMap
class ObjectFactory(Ice.ObjectFactory):
    """
    Responsible for instantiating objects during deserialization.
    """

    def __init__(self, map = util.FactoryMap.map()):
        self.__m = map

    def registerObjectFactory(self, ic):
        for key in self.__m:
            if not ic.findObjectFactory(key):
                ic.addObjectFactory(self,key)

    def create(self, type):
        generator = self.__m[type]
        if generator == None:
            raise ClientError("Unknown type:"+type)
        return generator.next()

    def destroy(self):
        # Nothing to do
        pass

class ClientError(exceptions.Exception):
    """
    Top of client exception hierarchy.
    """
    pass

class UnloadedEntityException(ClientError):
    pass

class UnloadedCollectionException(ClientError):
    pass

