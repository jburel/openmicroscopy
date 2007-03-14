#include <OMERO/client.h>
#include <ObjectFactoryRegistrar.h>

using namespace std;

namespace OMERO {
  
  client::client(int& argc, char* argv[]) {
    ic = Ice::initialize(argc, argv);
    ObjectFactoryPtr of = new ObjectFactory();
    of->registerObjectFactory(ic);
  }
  
  client::~client(){
    if (sf) {
      try {
	sf->close();
      } catch (const Ice::Exception& ex) {
	cerr << "Caught Ice exception while closing session." << endl;
	cerr << ex << endl;
      }
    }
    if (ic) {
      try {
	ic->destroy();
      } catch (const Ice::Exception& ex) {
	cerr << "Caught Ice exception while destroying communicator." << endl;
	cerr << ex << endl;
      }	
    }
  }
  
  void client::createSession() {
    
    string username = getProperty("OMERO.username");
    string password = getProperty("OMERO.password");

    Ice::RouterPrx prx = ic->getDefaultRouter();
    Glacier2::RouterPrx router = Glacier2::RouterPrx::checkedCast(prx);
    Glacier2::SessionPrx session;
    session = router->createSession(username, password);
    sf = omero::api::ServiceFactoryPrx::checkedCast(session);

  }
  
  omero::api::IAdminPrx client::getAdminService(const ::Ice::Context& ctx) {
    return sf->getAdminService(ctx);
  }
  
  omero::api::IConfigPrx client::getConfigService(const ::Ice::Context& ctx) {
    return sf->getConfigService(ctx);
  }
  
  omero::api::IPixelsPrx client::getPixelsService(const ::Ice::Context& ctx) {
    return sf->getPixelsService(ctx);
  }
  
  omero::api::IPojosPrx client::getPojosService(const ::Ice::Context& ctx) {
    return sf->getPojosService(ctx);
  }
  
  omero::api::IQueryPrx client::getQueryService(const ::Ice::Context& ctx) {
    return sf->getQueryService(ctx);
  }
  
  omero::api::ITypesPrx client::getTypesService(const ::Ice::Context& ctx) {
    return sf->getTypesService(ctx);
  }
  
  omero::api::IUpdatePrx client::getUpdateService(const ::Ice::Context& ctx) {
    return sf->getUpdateService(ctx);
  }
  
  omero::api::RawFileStorePrx client::createRawFileStore(const ::Ice::Context& ctx) {
    return sf->createRawFileStore(ctx);
  }
  
  omero::api::RawPixelsStorePrx client::createRawPixelsStore(const ::Ice::Context& ctx) {
    return sf->createRawPixelsStore(ctx);
  }
  
  omero::api::RenderingEnginePrx client::createRenderingEngine(const ::Ice::Context& ctx) {
    return sf->createRenderingEngine(ctx);
  }
  
  omero::api::ThumbnailStorePrx client::createThumbnailStore(const ::Ice::Context& ctx) {
    return sf->createThumbnailStore(ctx);
  }

  Ice::ObjectPrx client::getByName(const string& name, const ::Ice::Context& ctx) {
    return sf->getByName(name, ctx);
  }

  void client::setCallback(const ::omero::api::SimpleCallbackPrx& cb, const ::Ice::Context& ctx) {
    sf->setCallback(cb, ctx);
  }
 
  void client::close(const ::Ice::Context& ctx) {
    sf->close(ctx);
  }

}
