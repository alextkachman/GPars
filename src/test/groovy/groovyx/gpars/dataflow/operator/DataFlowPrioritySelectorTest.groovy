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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.dataflow.DataFlowStream
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup
import java.util.concurrent.CyclicBarrier
import static groovyx.gpars.dataflow.DataFlow.prioritySelector

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class DataFlowPrioritySelectorTest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(1)
    }

    protected void tearDown() {
        group.shutdown()
    }

    public void testSelector() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final DataFlowStream e = new DataFlowStream()

        def op = group.prioritySelector(inputs: [a, b, c], outputs: [d, e]) {x ->
            bindOutput 0, x
            bindOutput 1, 2 * x
        }

        a << 5
        sleep 500
        b << 20
        sleep 500
        c << 40
        sleep 500
        b << 50

        assert [d.val, d.val, d.val, d.val] == [5, 20, 40, 50]
        assert [e.val, e.val, e.val, e.val] == [10, 40, 80, 100]

        op.stop()
    }

    public void testSelectorWithValuesBoundBeforeCreation() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final DataFlowStream e = new DataFlowStream()

        a << 5
        b << 20
        c << 40
        b << 50

        def op = group.prioritySelector(inputs: [a, b, c], outputs: [d, e]) {x ->
            bindOutput 0, x
            bindOutput 1, 2 * x
        }

        assert [d.val, d.val, d.val, d.val] == [5, 20, 50, 40]
        assert [e.val, e.val, e.val, e.val] == [10, 40, 100, 80]

        op.stop()
    }

    public void testSelectorNotResubscribedOnDFVs() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        def op = group.prioritySelector(inputs: [a, b, c], outputs: [d]) {x ->
            bindOutput 0, x
        }

        a << 5
        sleep 1000
        b << 20
        sleep 1000

        c << 40
        c << 50
        c << 60

        assert [d.val, d.val, d.val, d.val, d.val] == [5, 20, 40, 50, 60]

        op.stop()
    }

    public void testDefaultCopySelector() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final DataFlowStream e = new DataFlowStream()

        a << 5
        b << 20
        c << 40

        def op = group.prioritySelector(inputs: [a, b, c], outputs: [d, e])

        sleep 3000
        b << 50

        assert [d.val, d.val, d.val, d.val] == [5, 20, 40, 50]
        assert [e.val, e.val, e.val, e.val] == [5, 20, 40, 50]

        op.stop()
    }

    public void testSelectorWithIndex() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final DataFlowStream e = new DataFlowStream()

        a << 5
        b << 20
        c << 40

        def op = group.prioritySelector(inputs: [a, b, c], outputs: [d, e]) {x, index ->
            bindOutput 0, x
            bindOutput 1, index
        }

        sleep 3000
        b << 50
        sleep 500
        c << 60

        assert [d.val, d.val, d.val, d.val, d.val] == [5, 20, 40, 50, 60]
        assert [e.val, e.val, e.val, e.val, e.val] == [0, 1, 2, 1, 2]

        op.stop()
    }

    public void testOperatorWithDoubleWaitOnChannel() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final CyclicBarrier barrier = new CyclicBarrier(2)

        def op = group.prioritySelector(inputs: [a, a], outputs: [b]) {x ->
            bindOutput 0, x
            barrier.await()
        }

        a << 1
        barrier.await()
        a << 2
        barrier.await()
        a << 3
        barrier.await()
        a << 4
        barrier.await()

        assert [b.val, b.val, b.val, b.val] == [1, 2, 3, 4]

        op.stop()
    }

    public void testStop() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final CyclicBarrier barrier1 = new CyclicBarrier(2)
        final CyclicBarrier barrier2 = new CyclicBarrier(2)
        volatile int counter = 0

        def op1 = group.prioritySelector(inputs: [a, b], outputs: [c]) {x ->
            barrier1.await()
            counter++
            barrier2.await()

        }
        a << 'Delivered'
        barrier1.await()
        a << 'Never delivered'
        op1.stop()
        barrier2.await()
        op1.join()
        assert counter == 1
    }

    public void testInterrupt() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        volatile boolean flag = false

        def op1 = group.prioritySelector(inputs: [a], outputs: [b]) {v ->
            Thread.currentThread().interrupt()
            flag = true
            bindOutput 'a'
        }
        op1.actor.metaClass.onInterrupt = {}
        assertFalse flag
        a << 'Message'
        assertEquals 'a', b.val
        assertTrue flag
        op1.stop()
        op1.join()
    }

    public void testEmptyInputs() {
        final DataFlowStream b = new DataFlowStream()
        volatile boolean flag = false

        shouldFail(IllegalArgumentException) {
            def op1 = group.prioritySelector(inputs: [], outputs: [b]) {->
                flag = true
                stop()
            }
            op1.join()
        }
        assert !flag
    }

    public void testOutputs() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        volatile boolean flag = false

        def op1 = group.prioritySelector(inputs: [a], outputs: [b, c]) {
            flag = (output == b) && (outputs[0] == b) && (outputs[1] == c)
            stop()
        }
        a << null
        op1.join()
        assert flag
        assert (op1.output == b) && (op1.outputs[0] == b) && (op1.outputs[1] == c)
        assert (op1.getOutput() == b) && (op1.getOutputs(0) == b) && (op1.getOutputs(1) == c)
    }

    public void testEmptyOutputs() {
        final DataFlowStream b = new DataFlowStream()
        volatile boolean flag = false

        def op1 = group.prioritySelector(inputs: [b], outputs: []) {
            flag = (output == null)
            stop()
        }
        b << null
        op1.join()
        assert flag
        assert op1.output == null
    }

    public void testInputNumber() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        group.prioritySelector(inputs: [a, b], outputs: [d]) {}.stop()
        group.prioritySelector(inputs: [a, b], outputs: [d]) {x ->}.stop()
        group.prioritySelector(inputs: [a, b], outputs: [d]) {x, y ->}.stop()

        shouldFail(IllegalArgumentException) {
            def op1 = group.prioritySelector(inputs: [a, b, c], outputs: [d]) {x, y, z -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = group.prioritySelector(inputs: [], outputs: [d]) { }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = group.prioritySelector(inputs: [a], outputs: [d]) {-> }
        }

        def op1 = group.prioritySelector(inputs: [a], outputs: [d]) { }
        op1.stop()

        op1 = group.prioritySelector(inputs: [a], outputs: [d]) {x -> }
        op1.stop()

        op1 = group.prioritySelector(inputs: [a, b], outputs: [d]) {x, y -> }
        op1.stop()
    }

    public void testOutputNumber() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        def selector1 = group.prioritySelector(inputs: [a], outputs: []) {v -> stop()}
        def selector2 = group.prioritySelector(inputs: [a]) {v -> stop()}
        def selector3 = group.prioritySelector(inputs: [a], mistypedOutputs: [d]) {v -> stop()}

        a << 'value'
        a << 'value'
        a << 'value'
        [selector1, selector2, selector3]*.stop()
        [selector1, selector2, selector3]*.join()
    }

    public void testMissingChannels() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        shouldFail(IllegalArgumentException) {
            def op1 = group.prioritySelector(outputs: [d]) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = group.prioritySelector([:]) {v -> }
        }
    }

    public void testException() {
        final DataFlowStream stream = new DataFlowStream()
        final DataFlowVariable a = new DataFlowVariable()

        def op = group.prioritySelector(inputs: [stream], outputs: []) {
            throw new RuntimeException('test')
        }
        op.metaClass.reportError = {Throwable e ->
            a << e
            stop()
        }
        stream << 'value'
        assert a.val instanceof RuntimeException
    }

    public void testExceptionWithDefaultHandler() {
        final DataFlowStream stream = new DataFlowStream()
        final DataFlowVariable a = new DataFlowVariable()

        def op = prioritySelector(inputs: [stream], outputs: []) {
            if (it == 'invalidValue') throw new RuntimeException('test')
        }
        stream << 'value'
        stream << 'invalidValue'
        op.join()
    }

    public void testPriority() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final CyclicBarrier barrier = new CyclicBarrier(2)

        def op1 = group.prioritySelector(inputs: [a, b], outputs: [c, d]) {x, index ->
            bindOutput 0, x
            bindOutput 1, index
            barrier.await()

        }
        a << 1
        a << 2
        b << 3
        b << 4
        a << 5
        4.times {barrier.await()}
        assert [c.val, c.val, c.val, c.val, c.val] == [1, 2, 5, 3, 4]
        assert [d.val, d.val, d.val, d.val, d.val] == [0, 0, 0, 1, 1]
        barrier.await()
        op1.stop()
        op1.join()
    }
}