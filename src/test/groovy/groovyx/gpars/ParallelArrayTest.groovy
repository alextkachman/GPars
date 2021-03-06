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

package groovyx.gpars

import java.util.concurrent.ConcurrentHashMap
import jsr166y.forkjoin.Ops.Mapper
import jsr166y.forkjoin.Ops.Predicate
import jsr166y.forkjoin.Ops.Reducer

/**
 * @author Vaclav Pech
 * Date: Nov 6, 2009
 */

public class ParallelArrayTest extends GroovyTestCase {

    public void testReduce() {
        GParsPool.withPool(5) {
            assertEquals 15, [1, 2, 3, 4, 5].parallelArray.withMapping({it} as Mapper).reduce({a, b -> a + b} as Reducer, null)
            assertEquals 'abc', 'abc'.parallelArray.withMapping({it} as Mapper).reduce({a, b -> a + b} as Reducer, null)
        }
    }

    @SuppressWarnings("GroovyMethodWithMoreThanThreeNegations")
    public void testFilterOperations() {
        GParsPool.withPool(5) {
            assertEquals 'aa', 'abcde'.parallelArray.withFilter({it != 'e'} as Predicate).withMapping({it * 2} as Mapper).all().withFilter({it != 'cc'} as Predicate).min()
        }
    }

    public void testNestedMap() {
        GParsPool.withPool(5) {
            assertEquals 65, [1, 2, 3, 4, 5].parallelArray.withMapping({it} as Mapper).withMapping({it + 10} as Mapper).reduce({a, b -> a + b} as Reducer, null)
        }
    }

    public void testReduceThreads() {
        final ConcurrentHashMap map = new ConcurrentHashMap()

        GParsPool.withPool(5) {
            assertEquals 55, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].parallelArray.withMapping({it} as Mapper).reduce({a, b ->
                Thread.sleep 200
                map[Thread.currentThread()] = ''
                a + b
            } as Reducer, null)
            assert map.keys().size() > 1
        }
    }

    public void testMinMax() {
        GParsPool.withPool(5) {
            assertEquals 1, [1, 2, 3, 4, 5].parallelArray.withMapping({it} as Mapper).min({a, b -> a - b} as Comparator)
            assertEquals 5, [1, 2, 3, 4, 5].parallelArray.withMapping({it} as Mapper).max({a, b -> a - b} as Comparator)
            assertEquals 'a', 'abc'.parallelArray.withMapping({it} as Mapper).min()
            assertEquals 'c', 'abc'.parallelArray.withMapping({it} as Mapper).max()
        }
    }

    public void testSort() {
        GParsPool.withPool(5) {
            final List sortedNums = [1, 2, 3, 4, 5]
            final def pa = [1, 2, 3, 4, 5].parallelArray.withMapping({it} as Mapper).all()
            pa.sort({a, b -> a - b} as Comparator)
            assertEquals(sortedNums, pa.all().asList())
        }
    }

}