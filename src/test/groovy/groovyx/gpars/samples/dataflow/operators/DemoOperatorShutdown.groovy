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

package groovyx.gpars.samples.dataflow.operators

import groovyx.gpars.dataflow.DataFlowStream
import groovyx.gpars.group.NonDaemonPGroup

/**
 * Dataflow operators and selectors can be stopped in two ways - calling the stop() method on all operators that need to be stopped
 * or by sending a poisson message. This demo shows the first approach. 
 */

final DataFlowStream a = new DataFlowStream()
final DataFlowStream b = new DataFlowStream()
final DataFlowStream c = new DataFlowStream()
final DataFlowStream d = new DataFlowStream()
final DataFlowStream e = new DataFlowStream()
final DataFlowStream f = new DataFlowStream()
final DataFlowStream out = new DataFlowStream()

final def group = new NonDaemonPGroup()

def op1 = group.operator(inputs: [a, b, c], outputs: [d, e]) {x, y, z -> }

def op2 = group.selector(inputs: [d], outputs: [f, out]) { }

def op3 = group.prioritySelector(inputs: [e, f], outputs: [b]) {value, index -> }

[op1, op2, op3]*.stop()
op1.join()
op2.join()
op3.join()

println "All operators have stopped."
group.shutdown()

