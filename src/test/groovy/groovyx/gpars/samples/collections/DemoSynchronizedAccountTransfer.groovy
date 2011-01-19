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

package groovyx.gpars.samples.collections

import static groovyx.gpars.GParsPool.withPool

/**
 * Demo the classic use of synchronized with ordered lock acquisition to
 * implement atomic updates when transferring money.
 * @author Dierk König
 */
class Account {
    int balance = 0

    synchronized void credit(int add) {
        this.@balance += add
    }

    /** Syncs on this and target */
    void transferTo(Account target, int amount) {
        def locks = [this,target].sort{ System.identityHashCode it }
        synchronized (locks[0]) {
            synchronized (locks[1]) {
                credit(-amount)
                target.credit amount
            }
        }
    }
}

def a = new Account()
def b = new Account()

withPool(50) {
    (1..1000).eachParallel {
        a.transferTo b, it   // even mutual transfer does not lead to deadlocks
        b.transferTo a, it
    }
}
assert [0, 0] == [a, b].balance