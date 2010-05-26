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

package groovyx.gpars.samples.actors

import groovyx.gpars.actor.AbstractPooledActor

class ExitSignal {
    String reason
}

abstract class AbstractExitActor extends AbstractPooledActor {
    def links = []
    def exitSignal = new ExitSignal(reason: "EXIT")

    def sendToAllLinks(exitSig = exitSignal) {
        links.each {it ->
            it << exitSig
        }
        //Stop the actor???
    }

    def addLink(actor) {
        links << actor
    }

    def removeLink(actor) {
        links.remove(actor)
    }

    protected void act() {
        loop {
            react {e ->
                println "Got exit signal."
                if (this.respondsTo("handleExit"))
                    this.handleExit(e)
                else {
                    println "Forwarding exit to links"
                    throw new Exception(e.reason)
                }
            }
        }
    }

    void onException(e) {
        sendToAllLinks(new ExitSignal(reason: e.message))
    }
}

class ActorWhichWillDie extends AbstractExitActor {
    @Override
    protected void act() {
        throw new Exception("I died a little")
    }
}

class ActorWhichIsSystem extends AbstractExitActor {
    @Override
    protected void act() {
        loop {
            react {msg ->
                if (msg instanceof ExitSignal) {
                    handleExit msg
                    stop()
                }
                println msg
            }
        }
    }

    def handleExit(e) {
        println e.reason
    }
}

def actdie1 = new ActorWhichWillDie()
def actdie2 = new ActorWhichWillDie()
def actdie3 = new ActorWhichWillDie()
def sysact = new ActorWhichIsSystem()
sysact.start()
actdie1.addLink(sysact)
actdie2.addLink(sysact)
actdie3.addLink(sysact)
actdie1.start()
actdie2.start()
actdie3.start()

//sysact.stop()
sysact.join()
