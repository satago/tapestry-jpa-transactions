/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.satago.tapestry5.jpa.test;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import java.util.Date;

public class CommitCounter
{
    public static final ThreadLocal<VersionedThing> versionedThing = new ThreadLocal<VersionedThing>()
    {
        @Override
        protected VersionedThing initialValue()
        {
            return new VersionedThing();
        }
    };

    @PostPersist
    @PostUpdate
    private void updateVersion(Object entity)
    {
        versionedThing.get().updateVersion(entity);
    }

    public static class VersionedThing
    {
        private int version;

        private Date lastTouched;

        private VersionedThing()
        {

        }

        public int getVersion()
        {
            return version;
        }

        public Date getLastTouched()
        {
            return lastTouched;
        }

        public synchronized void updateVersion(Object entity)
        {
            this.lastTouched = new Date();
            this.version++;
        }

        public synchronized void reset()
        {
            this.lastTouched = null;
            this.version = 0;
        }
    }
}
