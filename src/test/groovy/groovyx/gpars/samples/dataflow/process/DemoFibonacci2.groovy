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

package groovyx.gpars.samples.dataflow.process

import groovyx.gpars.dataflow.DataFlowChannel
import groovyx.gpars.dataflow.DataFlowStream
import groovyx.gpars.group.DefaultPGroup

group = new DefaultPGroup()

def fib(DataFlowChannel out) {
    group.task {
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def d = new DataFlowStream()
        [new Prefix(d, a, 0), new Prefix(c, d, 1), new Copy(a, b, out), new Pairs(b, c)].each { group.task it}
    }
}

final DataFlowStream ch = new DataFlowStream()
group.task new Print('Fibonnaci numbers', ch)
fib(ch)

sleep 10000