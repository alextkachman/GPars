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

package groovyx.gpars.samples.benchmarks

import groovyx.gpars.GParsPool

List items = []
for (i in 1..100000) {items << i}

final def numOfIterations = 1..10
final def numOfWarmupIterations = 1..10

meassureSequential(numOfWarmupIterations, items)
final long time = meassureSequential(numOfIterations, items)
println "Sequential $time"

meassureThreadPool(numOfWarmupIterations, items)
time = meassureThreadPool(numOfIterations, items)
println "ThreadPool $time"

measureForkJoinPool(numOfWarmupIterations, items)
time = measureForkJoinPool(numOfIterations, items)
println "ForkJoinPool $time"

long measureSequential(iterations, List list) {
    final long t1 = System.currentTimeMillis()
    for (i in iterations) {
        def result
        list.each {result = it}
        def elements = list.collect {it}
        result = elements[-1]
    }
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}

long measureThreadPool(iterations, List list) {
    final long t1 = System.currentTimeMillis()
    groovyx.gpars.GParsExecutorsPool.withPool(30) {
        for (i in iterations) {
            int result
            list.eachParallel {result = it}
            def elements = list.collectParallel {it}
            result = elements[-1]
        }
    }
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}

long measureForkJoinPool(iterations, List list) {
    final long t1 = System.currentTimeMillis()
    GParsPool.withPool(30) {
        for (i in iterations) {
            int result
            list.eachParallel {result = it}
            def elements = list.collectParallel {it}
            result = elements[-1]
        }
    }
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}
