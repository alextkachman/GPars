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

package groovyx.gpars.dataflow;

import groovy.lang.Closure;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.group.PGroup;

/**
 * A helper class enabling the 'whenBound()' or 'getValAsync' functionality of a DataFlowVariable and DataFlowStream,
 * as well as 'sendAndContinue()' on actors.
 * A task that waits asynchronously on the DFV to be bound. Once the DFV is bound,
 * upon receiving the message the actor runs the supplied closure / code with the DFV value as a parameter.
 *
 * @author Vaclav Pech, Alex Tkachman
 *         Date: Sep 13, 2009
 */
public final class DataCallback extends MessageStream {
    private static final long serialVersionUID = 6512046150477794254L;
    private final Closure code;
    private final PGroup parallelGroup;

    /**
     * @param code   The closure to run
     * @param pGroup pGroup The parallel group to join
     */
    public DataCallback(final Closure code, final PGroup pGroup) {
        if (pGroup == null)
            throw new IllegalArgumentException("Cannot create a DataCallback without a parallelGroup parameter");
        this.parallelGroup = pGroup;
        this.code = code;
    }

    /**
     * Sends a message back to the DataCallback.
     * Will schedule processing the internal closure with the thread pool
     * Registers its parallel group with DataFlowExpressions for nested 'whenBound' handlers to use the same group.
     */
    @Override
    public MessageStream send(final Object message) {
        parallelGroup.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                DataFlow.activeParallelGroup.set(parallelGroup);
                try {
                    code.call(message);
                } finally {
                    DataFlow.activeParallelGroup.remove();
                }
            }
        });
        return this;
    }
}