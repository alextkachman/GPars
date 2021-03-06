// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.actor;

import groovy.lang.Closure;
import groovyx.gpars.actor.impl.DDAClosure;

/**
 * A pooled actor allowing for an alternative structure of the message handling code.
 * In general DynamicDispatchActor repeatedly scans for messages and dispatches arrived messages to one
 * of the onMessage(message) methods defined on the actor.
 * <pre>
 * final class MyActor extends DynamicDispatchActor {*      void onMessage(String message) {*          println 'Received string'
 * }*      void onMessage(Integer message) {*          println 'Received integer'
 * }*      void onMessage(Object message) {*          println 'Received object'
 * }*      void onMessage(NullObject nullMessage) {*          println 'Received null'
 * }*} </pre>
 * <p/>
 * Method when {...} provides an alternative way to define message handlers
 *
 * @author Vaclav Pech, Alex Tkachman, Dierk Koenig
 *         Date: Jun 26, 2009
 */

@SuppressWarnings({"ThisEscapedInObjectConstruction"})
public class DynamicDispatchActor extends AbstractLoopingActor {
    private static final long serialVersionUID = 2709208258556647529L;

    /**
     * Creates a new instance without any when handlers registered
     */
    public DynamicDispatchActor() {
        this(null);
    }

    /**
     * Creates an instance, processing all when{} calls in the supplied closure
     *
     * @param closure A closure to run against te actor, typically to register handlers
     */
    public DynamicDispatchActor(final Closure closure) {
        if (closure != null) {
            final Closure cloned = (Closure) closure.clone();
            cloned.setResolveStrategy(Closure.DELEGATE_FIRST);
            cloned.setDelegate(this);
            cloned.call();
        }

        initialize(new DDAClosure(this));
    }

    public final void when(final Closure closure) {
        DDAHelper.when(this, closure);
    }
}
