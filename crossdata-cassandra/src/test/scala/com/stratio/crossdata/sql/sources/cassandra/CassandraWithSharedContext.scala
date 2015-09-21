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

package com.stratio.crossdata.sql.sources.cassandra

import com.datastax.driver.core.{Cluster, Session}
import org.apache.spark.Logging
import org.apache.spark.sql.crossdata.test.SharedXDContextTest
import org.scalatest.Suite

trait CassandraWithSharedContext extends SharedXDContextTest with CassandraDefaultTestConstants with Logging {
  this: Suite =>

  var clusterAndSession: Option[(Cluster, Session)] = None
  var isEnvironmentReady = false

  override protected def beforeAll() = {
    super.beforeAll()

    try {

      clusterAndSession = Some(prepareEnvironment())

      sql(
        s"""|CREATE TEMPORARY TABLE $Table
            |USING $SourceProvider
            |OPTIONS (
            |table '$Table',
            |keyspace '$Catalog',
            |cluster '$ClusterName',
            |pushdown "true",
            |spark_cassandra_connection_host '$CassandraHost'
            |)
      """.stripMargin.replaceAll("\n", " "))

    } catch {
      case e: Throwable => logError(e.getMessage)
    }

    isEnvironmentReady = clusterAndSession.isDefined
  }

  override protected def afterAll() = {
    super.afterAll()

    clusterAndSession.foreach { case (cluster, session) => cleanEnvironment(cluster, session) }

  }

  def prepareEnvironment(): (Cluster, Session) = {
    val (cluster, session) = createSession()
    saveTestData(session)
    (cluster, session)
  }

  def cleanEnvironment(cluster: Cluster, session: Session) = {
    cleanTestData(session)
    closeSession(cluster, session)
  }


  private def createSession(): (Cluster, Session) = {
    val cluster = Cluster.builder().addContactPoint(CassandraHost).build()
    (cluster, cluster.connect())
  }

  private def saveTestData(session: Session): Unit = {

    session.execute(s"CREATE KEYSPACE $Catalog WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}  AND durable_writes = true;")
    session.execute(s"CREATE TABLE $Catalog.$Table (id int PRIMARY KEY, age int,comment text, enrolled boolean, name text)")


    for (a <- 1 to 10) {
      session.execute("INSERT INTO " + Catalog + "." + Table + " (id, age, comment, enrolled, name) VALUES " +
        "(" + a + ", " + (10 + a) + ", 'Comment " + a + "', " + (a % 2 == 0) + ", 'Name " + a + "')")
    }
  }

  private def cleanTestData(session: Session): Unit = {
    session.execute(s"DROP KEYSPACE $Catalog")
  }

  private def closeSession(cluster: Cluster, session: Session): Unit = {
    session.close()
    cluster.close()
  }

}

sealed trait CassandraDefaultTestConstants {
  val ClusterName = "Test Cluster"
  val Catalog = "highschool"
  val Table = "students"
  val CassandraHost = "127.0.0.1"
  val SourceProvider = "com.stratio.crossdata.sql.sources.cassandra"
}