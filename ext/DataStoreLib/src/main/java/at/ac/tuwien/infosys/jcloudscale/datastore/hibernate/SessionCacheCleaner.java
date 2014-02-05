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
package at.ac.tuwien.infosys.jcloudscale.datastore.hibernate;

import org.hibernate.Session;
import org.hibernate.action.AfterTransactionCompletionProcess;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;

/**
 * Created with IntelliJ IDEA.
 * User: Rene
 * Date: 31.01.13
 * Time: 18:27
 * To change this template use File | Settings | File Templates.
 */
public class SessionCacheCleaner {

        public void scheduleAuditDataRemoval(final Session session, final Object data) {
            ((EventSource) session).getActionQueue().registerProcess(new AfterTransactionCompletionProcess() {
                public void doAfterTransactionCompletion(boolean success, SessionImplementor session) {
                    if (!session.isClosed()) {
                        ((Session) session).evict(data);
                    }
                }
            });
        }
}
