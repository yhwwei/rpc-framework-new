package rpc.provider;


public interface ServiceProvider {


    /**
     * @param rpcServiceName rpc service name
     * @return service object
     */
    Object getService(String rpcServiceName);


    void addService(String interfaceName,Object obj);
}
