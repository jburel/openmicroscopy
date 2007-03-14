/*
 *   $Id$
 * 
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

#ifndef LicensesAPI
#define LicensesAPI

#include <Ice/BuiltinSequences.ice>
#include <Error.ice>

module omero { 
  module licenses {     

    exception NoAvailableLicenseException extends omero::SessionCreationException {

    };

    interface ILicense
    {
      Ice::ByteSeq acquireLicense();
      long getAvailableLicenseCount();
      long getLicenseTimeout();
      long getTotalLicenseCount();
      bool releaseLicense(Ice::ByteSeq token);
      void resetLicenses();
    };

  };
};

#endif // LicensesAPI
