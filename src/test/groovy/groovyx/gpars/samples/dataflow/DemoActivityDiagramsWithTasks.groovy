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

package groovyx.gpars.samples.dataflow

import static groovyx.gpars.dataflow.DataFlow.task

task {
    println 'Activity 1'
    subtask {
        sleep 1000
        println 'Activity 2a'
    }
    subtask {
        sleep 500
        println 'Activity 2b'
        subtask {
            println 'Activity 2ba'
        }
        subtask {
            sleep 3000
            println 'Activity 2bb'
        }
    }
} >> {
    def t1 = task {
        subtask {
            println 'Activity 3a'
        }
        subtask {
            println 'Activity 3b'
        }
    }
    def c3 = task {
        println 'Activity 3c'
    }


    t1 >> {
        c3 >> {
            task {
                println 'Activity 4a'
            }
        }
    }

    t1 >> {
        task {
            println 'Activity 4b'
        }
    }
}