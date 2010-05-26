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

class ActorWhichWillStop extends AbstractExitActor {
    @Override
    protected void act() {
        throw new Exception("I need to stop")
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

def act1 = new ActorWhichWillStop()
def act2 = new ActorWhichWillStop()
def act3 = new ActorWhichWillStop()
def systemActor = new ActorWhichIsSystem()
systemActor.start()
act1.addLink(systemActor)
act2.addLink(systemActor)
act3.addLink(systemActor)
act1.start()
act2.start()
act3.start()

systemActor.join()
