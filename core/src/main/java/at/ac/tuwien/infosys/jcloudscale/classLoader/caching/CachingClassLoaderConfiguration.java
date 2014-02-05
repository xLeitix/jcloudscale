/*
   Copyright 2013 Philipp Leitner

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package at.ac.tuwien.infosys.jcloudscale.classLoader.caching;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.infosys.jcloudscale.classLoader.AbstractClassLoaderConfiguration;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache.CacheConfiguration;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache.CacheType;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.cache.ICacheManager;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors.FileBasedFileCollector;
import at.ac.tuwien.infosys.jcloudscale.classLoader.caching.fileCollectors.FileCollectorAbstract;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class CachingClassLoaderConfiguration extends AbstractClassLoaderConfiguration implements Cloneable
{
    private static final long serialVersionUID = 1L;
    String requestQueue = "CS_ClassRequest";
    String responseQueue = "CS_ClassResponse";
    private FileCollectorAbstract fileCollector = null;

    private CacheConfiguration cacheConfiguration = CacheConfiguration
            .createCacheConfig(CacheType.FileCache, true);

    boolean localFirst = true;

    public CachingClassLoaderConfiguration(){}

    public void setRequestQueue(String requestQueue) {
        this.requestQueue = requestQueue;
    }
    public void setResponseQueue(String responseQueue) {
        this.responseQueue = responseQueue;
    }
    public synchronized void setFileCollector(FileCollectorAbstract fileCollector) {
        this.fileCollector = fileCollector;
    }
    public void setCacheType(CacheType cacheType, boolean shareCache){
        cacheConfiguration = CacheConfiguration.createCacheConfig(cacheType, shareCache);
    }

    public CacheConfiguration getCacheConfiguration()
    {
        return cacheConfiguration;
    }

    ICacheManager getCacheManager()
    {
        return cacheConfiguration.getCacheManager();
    }

    public synchronized FileCollectorAbstract getFileCollector()
    {
        if(this.fileCollector == null)
            this.fileCollector = new FileBasedFileCollector();

        return this.fileCollector;
    }

    @Override
    public ClassLoader createClassLoader()
    {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return new RemoteClassLoader(CachingClassLoaderConfiguration.this);
            }});
    }

    @Override
    public Closeable createClassProvider()
    {
        return new RemoteClassProvider(this);
    }

    public void setLocalFirst(boolean localFirst) {
        this.localFirst = localFirst;
    }

    //---------------------------------------------------

    /**
     * Specifies the request and response queue names that should be used for classloading.
     * @param requestQueue The name of the queue where class loading requests will be sent and received.
     * @param responseQueue The name of the queue where class loading responses will be sent and received
     * @return The current instance of <b>CachingClassLoaderConfiguration</b> to continue configuration.
     */
    public CachingClassLoaderConfiguration with(String requestQueue, String responseQueue)
    {
        this.requestQueue = requestQueue;
        this.responseQueue = responseQueue;
        return this;
    }

    /**
     * Specifies the cache configuration to use for optimizing amount of requests.
     * @param cacheType The type of the cache that server should use to not issue same requests multiple times.
     * @param shareCache The boolean flag that indicates whether the cache should be shared between
     * different requests (<b>true</b>) or not (<b>false</b>)
     * @return The current instance of <b>CachingClassLoaderConfiguration</b> to continue configuration.
     */
    public CachingClassLoaderConfiguration with(CacheType cacheType, boolean shareCache)
    {
        setCacheType(cacheType, shareCache);
        return this;
    }

    /**
     * Specifies the file collector to use for determining what should be provided to the server.
     * @param fileCollector The instance of the class that implements <b>FileCollectorAbstract</b> class and allows
     * determining what should be provided as a response to a specific request.
     * @return The current instance of <b>CachingClassLoaderConfiguration</b> to continue configuration.
     */
    public CachingClassLoaderConfiguration with(FileCollectorAbstract fileCollector)
    {
        setFileCollector(fileCollector);
        return this;
    }


    @Override
    public CachingClassLoaderConfiguration clone()
    {
        return (CachingClassLoaderConfiguration) super.clone();
    }

    //--------------------------SERIALIZATION HELPERS-------------------------------------

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        //this field is not necessary on the server side and can be cleaned to avoid classloading issues.
        FileCollectorAbstract fileCollector = this.fileCollector;
        this.fileCollector = null;

        try
        {
            out.defaultWriteObject();
        }
        finally
        {
            this.fileCollector = fileCollector;
        }
    }
}
