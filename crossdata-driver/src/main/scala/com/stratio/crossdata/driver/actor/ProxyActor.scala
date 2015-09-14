/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.stratio.crossdata.driver.actor

import akka.actor.{Props, ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.contrib.pattern.ClusterClient
import com.stratio.crossdata.driver.Driver

object ProxyActor {
  val SERVER_PATH = "/crossdata-server/"

  def props(clusterClientActor: ActorRef, driver: Driver): Props =
    Props(new ProxyActor(clusterClientActor, driver))

}

class ProxyActor(clusterClientActor: ActorRef, driver: Driver) extends Actor {
  override def receive: Receive = {
    case message: String => {
      clusterClientActor forward ClusterClient.Send(ProxyActor.SERVER_PATH, message, false)
    }
  }
}
