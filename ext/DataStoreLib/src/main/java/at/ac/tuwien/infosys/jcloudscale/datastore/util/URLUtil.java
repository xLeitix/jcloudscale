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
package at.ac.tuwien.infosys.jcloudscale.datastore.util;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.Datastore;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.DatastoreProperties;
import at.ac.tuwien.infosys.jcloudscale.datastore.rest.Request;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

public final class URLUtil {

    //Hide default constructor
    private URLUtil() {}

    /**
     * Returns a URL created from the given params
     *
     * @param urlParams given params
     * @return URL with the given params
     */
    public static String buildURLForParams(String... urlParams) {
        String url = "/";
        for(String param : urlParams) {
            url += param + "/";
        }
        return url;
    }

    /**
     * Returns a URL created from the given params and with the given prefix
     *
     * @param prefix given prefix
     * @param urlParams given params
     * @return URL with the given params and prefix
     */
    public static String buildURLForParamsWithPrefix(String prefix, String... urlParams) {
        String url = "/" + prefix + buildURLForParams(urlParams);
        return url;
    }

    /**
     * Returns the URL for a given Request
     *
     * @param request given Request
     * @return the URL for the given Request
     */
    public static String buildURLForRequest(Request request) {
        if(request == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(request.getProtocolType().toString().toLowerCase());
        stringBuilder.append("://");
        stringBuilder.append(request.getHost());
        stringBuilder.append(":");
        stringBuilder.append(request.getPort());
        stringBuilder.append(request.getUrl());
        return stringBuilder.toString();
    }

    /**
     * Returns a URL created from the given params without postfix
     *
     * @param urlParams given params
     * @return URL with the given params
     */
    public static String buildURLForParamsWithoutPostfix(String... urlParams) {
        String url = buildURLForParams(urlParams);
        return StringUtils.removeEnd(url, "/");
    }

    /**
     * Get the URL to save objects for a given datastore
     *
     * @param datastore given datastore
     * @return the URL to save objects
     */
    public static String getSaveURL(Datastore datastore) {
        String url = datastore.getProperty(DatastoreProperties.URL_SAVE);
        return formatMessage(url, datastore.getDataUnit());
    }

    /**
     * Get the URL to save objects with a given id in the datastore
     *
     * @param datastore given datastore
     * @param id given id
     * @return the URL to save objects with the given id
     */
    public static String getSaveWithIdURL(Datastore datastore, String id) {
        String url = datastore.getProperty(DatastoreProperties.URL_SAVE_ID);
        return formatMessage(url, datastore.getDataUnit(), id);
    }

    /**
     * Get the URL to find objects in a given datastore
     *
     * @param datastore given datastore
     * @param id id of the object to find
     * @return the URL to find the object
     */
    public static String getFindURL(Datastore datastore, String id) {
        String url = datastore.getProperty(DatastoreProperties.URL_FIND);
        return formatMessage(url, datastore.getDataUnit(), id);
    }

    /**
     * Get the URL to find attachments in a CouchDB datastore
     *
     * @param datastore given CouchDB datastore
     * @param id id of the object containing the attachment
     * @param attachmentId id of the attachment
     * @return the URL to find the attachment
     */
    public static String getCouchDBAttachmentURL(Datastore datastore, String id, String attachmentId) {
        String url = datastore.getProperty(DatastoreProperties.URL_FIND_COUCHDB_ATTACHMENT);
        return formatMessage(url, datastore.getDataUnit(), id, attachmentId);
    }

    /**
     * Get the URL to delete objects from a given datastore
     *
     * @param datastore given datastore
     * @param id id of the object to delete
     * @param revision the revision to delete (null if not required)
     * @return the URL to delete the object
     */
    public static String getDeleteURL(Datastore datastore, String id, String revision) {
        String url = datastore.getProperty(DatastoreProperties.URL_DELETE);
        return formatMessage(url, datastore.getDataUnit(), id, revision);
    }

    /**
     * Get the URL to get the current revision of a document in a given CouchDB datastore
     *
     * @param datastore given CouchDB datstore
     * @param id id of the object
     * @return the current revision of the object
     */
    public static String getCouchDBCurrentRevision(Datastore datastore, String id) {
        String url = datastore.getProperty(DatastoreProperties.URL_COUCHDB_CURRENT_REVISION);
        return formatMessage(url, datastore.getDataUnit(), id);
    }

    /**
     * Get the URL to update objects in a given datastore
     *
     * @param datastore given datastore
     * @param id id of the object to update
     * @return the URL to update the object
     */
    public static String getUpdateURL(Datastore datastore, String id) {
        String url = datastore.getProperty(DatastoreProperties.URL_UPDATE);
        return formatMessage(url, datastore.getDataUnit(), id);
    }

    private static String formatMessage(String message, Object... args) {
        MessageFormat messageFormat = new MessageFormat(message);
        return messageFormat.format(args);
    }
}
