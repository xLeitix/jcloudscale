/*
   Copyright 2013 Rene Nowak 

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
package at.ac.tuwien.infosys.jcloudscale.datastore.configuration;

import at.ac.tuwien.infosys.jcloudscale.configuration.JCloudScaleConfiguration;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

public class ClassLoaderServiceImpl implements ClassLoaderService {

    private Logger log;

    public ClassLoaderServiceImpl() {
        this.log = JCloudScaleConfiguration.getLogger(this);
    }

    @Override
    public InputStream locateResourceStream(String name) {
        ClassLoader aggregatedClassLoader = SaxParser.class.getClassLoader();
        // first we try name as a URL
        try {
            log.info( "trying via [new URL(\"" + name + "\")]");
            return new URL( name ).openStream();
        }
        catch ( Exception ignore ) {
        }

        try {
            log.info( "trying via [ClassLoader.getResourceAsStream(\"" + name + "\")]");
            final InputStream stream =  aggregatedClassLoader.getResourceAsStream( name );
            if ( stream != null ) {
                return stream;
            }
        }
        catch ( Exception ignore ) {
        }

        final String stripped = name.startsWith( "/" ) ? name.substring( 1 ) : null;

        if ( stripped != null ) {
            try {
                log.info( "trying via [new URL(\"" + stripped + "\")]");
                return new URL( stripped ).openStream();
            }
            catch ( Exception ignore ) {
            }

            try {
                log.info( "trying via [ClassLoader.getResourceAsStream(\"" + stripped + "\")]");
                final InputStream stream = aggregatedClassLoader.getResourceAsStream( stripped );
                if ( stream != null ) {
                    return stream;
                }
            }
            catch ( Exception ignore ) {
            }
        }

        return null;
    }
}
