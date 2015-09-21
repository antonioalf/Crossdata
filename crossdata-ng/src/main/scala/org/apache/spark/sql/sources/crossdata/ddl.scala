package org.apache.spark.sql.sources.crossdata

import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.expressions.Row
import org.apache.spark.sql.catalyst.plans.logical.{Command, LogicalPlan}
import org.apache.spark.sql.execution.RunnableCommand


private [crossdata] case class ImportCatalogUsingWithOptions(provider: String, opts: Map[String, String])
  extends LogicalPlan with RunnableCommand {
  override def run(sqlContext: SQLContext): Seq[Row] = ??? //TODO: Implement the command action
}
