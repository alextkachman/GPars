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

package groovyx.gpars.dataflow

import groovyx.gpars.actor.Actors
import spock.lang.Specification

class AsyncSelectTest extends Specification {
    def "selecting from three df variables"() {
        given:
        def a = new DataFlowVariable()
        def b = new DataFlowVariable()
        def c = new DataFlowVariable()

        final def result = new DataFlows()
        def select = DataFlow.select(a, b, c)

        def actor
        actor = Actors.oldActor {
            result.res1 = receive()
            a << 30
            select(actor)
            result.res2 = receive()
        }
        select(actor)
        when:
        b << 10
        then:
        result.res1 == [1, 10] as SelectResult
        result.res2 == [0, 30] as SelectResult
    }

    def "selecting from three df streams"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        final def result = new DataFlows()
        def select = DataFlow.select(a, b, c)

        def actor
        actor = Actors.oldActor {
            result.res1 = receive()
            a << 30
            select(actor)
            result.res2 = receive()
        }

        when:
        b << 10
        select(actor)
        select(actor)
        then:
        result.res1 == [1, 10] as SelectResult
        result.res2 == [0, 30] as SelectResult
    }

    def "selecting from three df streams with a value being bound prior to selector creation"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        c << 20
        def result = new DataFlows()

        def select = DataFlow.select(a, b, c)
        def actor
        actor = Actors.oldActor {
            result.res1 = receive()
        }
        when:
        select(actor)
        then:
        result.res1 == [2, 20] as SelectResult
    }

    def "selecting preserves order within a single stream"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def result = new DataFlows()

        def select = DataFlow.select(a, b, c)
        def actor
        actor = Actors.oldActor {
            result.res1 = receive()
            result.res2 = receive()
            result.res3 = receive()
        }

        when:
        b << 10
        b << 20
        select(actor)
        select(actor)
        select(actor)
        b << 30
        then:
        result.res1 == [1, 10] as SelectResult
        result.res2 == [1, 20] as SelectResult
        result.res3 == [1, 30] as SelectResult
    }
}
