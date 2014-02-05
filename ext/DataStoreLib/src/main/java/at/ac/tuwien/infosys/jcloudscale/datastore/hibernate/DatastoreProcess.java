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

import at.ac.tuwien.infosys.jcloudscale.datastore.hibernate.work.Work;
import org.hibernate.Session;
import org.hibernate.action.BeforeTransactionCompletionProcess;
import org.hibernate.engine.SessionImplementor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DatastoreProcess implements BeforeTransactionCompletionProcess {

    private List<Work> workers = new ArrayList<Work>();

    @Override
    public void doBeforeTransactionCompletion(SessionImplementor session) {
        executeInSession((Session) session);
        session.flush();
    }

    private void executeInSession(Session session) {
        Iterator<Work> iterator = workers.iterator();
        while (iterator.hasNext()) {
            iterator.next().doWork(session);
            iterator.remove();
        }
    }

    public void addWork(Work work) {
        workers.add(work);
    }
}
