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

import groovyx.gpars.group.PGroup;
import groovyx.gpars.scheduler.ResizeablePool;

/**
 * Groups all dataflow threads, tasks and operators.
 * DataFlow leverages a resizeable pool of non-daemon threads.
 * DataFlowPGroup can be used directly to create and group dataflow actors (threads)
 * <pre>
 * DataFlowPGroup group = new DataFlowPGroup()
 * group.actor {
 *     ....
 * }
 * </pre>
 *
 * @author Vaclav Pech, Alex Tkachman
 *         Date: Jun 21, 2009
 */
public final class DataFlowPGroup extends PGroup {
    /**
     * Creates a default group for dataflow tasks and operators. The actors will share a common non-daemon thread pool.
     *
     * @param poolSize The initial size of the underlying thread pool
     */
    public DataFlowPGroup(final int poolSize) {
        super(new ResizeablePool(false, poolSize));
    }
}
