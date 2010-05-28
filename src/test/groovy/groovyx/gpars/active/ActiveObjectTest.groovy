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

package groovyx.gpars.active

import groovyx.gpars.GParsPool
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.dataflow.DataFlows

class ActiveObjectTest extends GroovyTestCase {
    public void testOriginalObject() {
        final Original original = new Original()
        assert original.size() == 0
        assertFalse original.contains('foo')
    }

    public void testActiveObject() {
        final Original original = new Original()
        ActiveObjects.activate original
        assert original.size() == 0
        assertFalse original.contains('Joe')

        original.add 'Joe'
        original.add 'Dave'
        assert original.size() == 2
        assertTrue original.contains('Joe')
        assertTrue original.contains('Dave')

        original.add(['Alice', 'Susan'])
        assert original.size() == 4
        assertTrue original.contains('Alice')
        assertTrue original.contains('Susan')
        assertTrue original.contains('Dave')

        original.add('James', 'Jim')
        assert original.size() == 6
        assertTrue original.contains('Alice')
        assertTrue original.contains('James')
        assertTrue original.contains('Jim')
    }

    public void testAsynchronousMethods() {
        final Original original = new Original()
        ActiveObjects.activate original, true  //set to asynchronous

        assert original.size() instanceof DataFlowVariable
        assert original.size().val == 0
        assertFalse original.contains('Joe').val

        original.add 'Joe'
        original.add 'Dave'
        assert original.size() instanceof DataFlowVariable
        assert original.size().val == 2
        assertTrue original.contains('Joe').val
        assertTrue original.contains('Dave').val

        //todo make a sample out of these
        original.add(['Alice', 'Susan'])
        assert original.size().val == 4
        assert (
        [
                original.contains('Alice'),
                original.contains('Susan'),
                original.contains('Dave')
        ]*.val.inject(true) {a, b -> a && b})
        GParsPool.withPool {
            assert (
            [
                    original.contains('Alice'),
                    original.contains('Susan'),
                    original.contains('Dave')
            ].parallel.map {it.val}.reduce {a, b -> a && b})
        }

        original.add('James', 'Jim')
        assert original.size().val == 6
        assertTrue original.contains('Alice').val
        assertTrue original.contains('James').val
        assertTrue original.contains('Jim').val
    }

    public void testProperties() {
        final Original original = new Original()
        ActiveObjects.activate original
        assert original.size() == 0
        assertFalse original.contains('Joe')
        assert original.state.isEmpty()

        original.add 'Joe'
        original.add 'Dave'
        assert original.state.size() == 2
        assertTrue original.state.contains('Joe')
        assertTrue original.state.contains('Dave')

        original.state = []
        assert original.size() == 0
        assertFalse original.contains('Joe')
        assert original.state.isEmpty()
    }

    public void testList() {
        final List original = [1, 2, 3, 4, 5]
        ActiveObjects.activate original
        assert original.size() == 5
        assertFalse original.contains(6)
        assert original.contains(4)
        assertFalse original.isEmpty()

        original.add 6
        original.add 7
        assert original.size() == 7
        assertTrue original[0] == 1
        assertTrue original[-1] == 7

        assert [1, 2, 3, 4, 5, 6, 7] == original.collect {it}
        assert [1, 2, 3, 4, 5] == original.collect {it}[0..-3]
        assert 1 == original.iterator().next()
    }

    public void testAsyncList() {
        final List original = [1, 2, 3, 4, 5]
        ActiveObjects.activate original, true
        assert original.size().val == 5
        assertFalse original.contains(6).val
        assert original.contains(4).val
        assertFalse original.isEmpty().val

        original.add 6
        original.add 7
        assert original.size().val == 7
        assertTrue original[0].val == 1
        assertTrue original[-1].val == 7

        assert [1, 2, 3, 4, 5, 6, 7] == original.collect {it}.val
        assert [1, 2, 3, 4, 5] == original.collect {it}.val[0..-3]
        assert 1 == original.iterator().val.next()
    }

    public void testMap() {
        final Map original = [1: '1', 2: '2', 3: '3', 4: '4', 5: '5']
        ActiveObjects.activate original
        assert original.size() == 5
        assertFalse original.containsKey(6)
        assert original.containsKey(4)
        assertFalse original.isEmpty()

        original[6] = '6'
        original[7] = '7'
        assert original.size() == 7
        assertTrue original[1] == '1'
        assertTrue original[7] == '7'

        assert original.values().size() == 7
        assert ['1', '2', '3', '4', '5', '6', '7'] == original.collect {k, v -> v}
        assert ['1', '2', '3', '4', '5'] == original.collect {k, v -> v}[0..-3]
        assert 1 == original.iterator().next().key
        final def iter = original.iterator()
        iter.next()
        iter.next()
        assert '3' == iter.next().value
    }

    public void testAsyncMap() {
        final Map original = [1: '1', 2: '2', 3: '3', 4: '4', 5: '5']
        ActiveObjects.activate original, true
        assert original.size().val == 5
        assertFalse original.containsKey(6).val
        assert original.containsKey(4).val
        assertFalse original.isEmpty().val

        original[6] = '6'
        original[7] = '7'
        assert original.size().val == 7
        assertTrue original[1].val == '1'
        assertTrue original[7].val == '7'

        assert original.values().val.size() == 7
        assert ['1', '2', '3', '4', '5', '6', '7'] == original.collect {k, v -> v}.val
        assert ['1', '2', '3', '4', '5'] == original.collect {k, v -> v}.val[0..-3]
        assert 1 == original.iterator().val.next().key
        final def iter = original.iterator().val
        iter.next()
        iter.next()
        assert '3' == iter.next().value
    }

    public void testNonExistentMethod() {
        final Original original = new Original()
        original.metaClass.methodMissing = {String methodName, args -> 'checked'}
        ActiveObjects.activate original
        assert 'checked' == original.foo()

        original.metaClass.methodMissing = {String methodName, args -> 'checked again'}
        assert 'checked again' == original.foo()
    }

    public void testNonExistentMethodAsync() {
        final Original original = new Original()
        original.metaClass.methodMissing = {String methodName, args -> 'checked'}
        ActiveObjects.activate original, true
        assert 'checked' == original.foo().val

        original.metaClass.methodMissing = {String methodName, args -> 'checked again'}
        assert 'checked again' == original.foo().val
    }

    public void testNonExistentProperty() {
        final Original original = new Original()
        original.metaClass.propertyMissing = {String name -> 'checked'}
        ActiveObjects.activate original
        assert 'checked' == original.foo
        assert 'checked' == original.foo
    }

    public void testNonExistentPropertyAsync() {
        final Original original = new Original()
        original.metaClass.propertyMissing = {String name -> 'checked'}
        ActiveObjects.activate original, true
        assert 'checked' == original.foo
        assert 'checked' == original.foo
    }

    public void testNonExistentPropertySetter() {
        final DataFlows df = new DataFlows()

        final Original original = new Original()
        original.metaClass.propertyMissing = {String name, newValue -> df.result1 = newValue}
        ActiveObjects.activate original
        original.foo = 'checked'
        assert 'checked' == df.result1
    }

    public void testNonExistentPropertySetterAsync() {
        final DataFlows df = new DataFlows()

        final Original original = new Original()
        original.metaClass.propertyMissing = {String name, newValue -> df.result1 = newValue}
        ActiveObjects.activate original, true
        original.foo = 'checked'
        assert 'checked' == df.result1
    }

    public void testErrors() {
        final DataFlows df = new DataFlows()

        final Original original = new Original()
        original.metaClass.invalidMethod = {throw new RuntimeException('test ' + it)}
        ActiveObjects.activate original, true

        assertFalse original.hasActiveErrors()
        assert 0 == original.activeErrors.size()

        original.invalidMethod('call1')
        assert original.hasActiveErrors()
        def errors = original.activeErrors
        assert 1 == errors.size()

        assertFalse original.hasActiveErrors()
        assert 0 == original.activeErrors.size()
    }
}

class Original {
    List state = []

    public void add(item) {
        state << item
    }

    public void add(List items) {
        state.addAll items
    }

    public void add(item1, item2) {
        state.addAll([item1, item2])
    }

    public void remove(item) {
        state.remove item
    }

    public boolean contains(item) {
        state.contains(item)
    }

    public int size() {
        state.size()
    }
}
