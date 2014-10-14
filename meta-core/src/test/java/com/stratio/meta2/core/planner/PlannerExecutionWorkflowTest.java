/*
 * Licensed to STRATIO (C) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  The STRATIO (C) licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.stratio.meta2.core.planner;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.stratio.meta.common.connector.Operations;
import com.stratio.meta.common.exceptions.PlanningException;
import com.stratio.meta.common.executionplan.ExecutionPath;
import com.stratio.meta.common.executionplan.ExecutionWorkflow;
import com.stratio.meta.common.executionplan.StorageWorkflow;
import com.stratio.meta.common.logicalplan.Filter;
import com.stratio.meta.common.logicalplan.Join;
import com.stratio.meta.common.logicalplan.LogicalStep;
import com.stratio.meta.common.logicalplan.LogicalWorkflow;
import com.stratio.meta.common.logicalplan.Project;
import com.stratio.meta.common.logicalplan.Select;
import com.stratio.meta.common.logicalplan.UnionStep;
import com.stratio.meta.common.logicalplan.Window;
import com.stratio.meta.common.statements.structures.relationships.Operator;
import com.stratio.meta.common.statements.structures.relationships.Relation;
import com.stratio.meta.common.statements.structures.window.WindowType;
import com.stratio.meta.core.structures.InnerJoin;
import com.stratio.meta2.common.data.CatalogName;
import com.stratio.meta2.common.data.ClusterName;
import com.stratio.meta2.common.data.ColumnName;
import com.stratio.meta2.common.data.DataStoreName;
import com.stratio.meta2.common.data.TableName;
import com.stratio.meta2.common.metadata.ColumnType;
import com.stratio.meta2.common.metadata.ConnectorMetadata;
import com.stratio.meta2.common.metadata.TableMetadata;
import com.stratio.meta2.common.statements.structures.selectors.BooleanSelector;
import com.stratio.meta2.common.statements.structures.selectors.ColumnSelector;
import com.stratio.meta2.common.statements.structures.selectors.IntegerSelector;
import com.stratio.meta2.common.statements.structures.selectors.Selector;
import com.stratio.meta2.common.statements.structures.selectors.StringSelector;
import com.stratio.meta2.core.query.BaseQuery;
import com.stratio.meta2.core.query.StorageParsedQuery;
import com.stratio.meta2.core.query.StorageValidatedQuery;
import com.stratio.meta2.core.statements.InsertIntoStatement;
import com.stratio.meta2.core.statements.StorageStatement;



/**
 * Planner test concerning Execution workflow creation.
 */
public class PlannerExecutionWorkflowTest extends PlannerBaseTest {

    /**
     * Class logger.
     */
    private static final Logger LOG = Logger.getLogger(PlannerExecutionWorkflowTest.class);

    private PlannerWrapper plannerWrapper = new PlannerWrapper();

    private ConnectorMetadata connector1 = null;

    private ConnectorMetadata connector2 = null;

    private ClusterName clusterName = null;

    private TableMetadata table1 = null;
    private TableMetadata table2 = null;

    /**
     * Create a test Project operator.
     *
     * @param tableName Name of the table.
     * @param columns   List of columns.
     * @return A {@link com.stratio.meta.common.logicalplan.Project}.
     */
    public Project getProject(String tableName, ColumnName... columns) {
        Operations operation = Operations.PROJECT;
        Project project = new Project(operation, new TableName("demo", tableName), new ClusterName("TestCluster1"));
        for (ColumnName cn : columns) {
            project.addColumn(cn);
        }
        return project;
    }

    public Filter getFilter(Operations operation, ColumnName left, Operator operator, Selector right) {
        Relation relation = new Relation(new ColumnSelector(left), operator, right);
        Filter filter = new Filter(operation, relation);
        return filter;
    }

    public Select getSelect(ColumnName[] columns, ColumnType[] types) {
        Operations operation = Operations.SELECT_OPERATOR;
        Map<ColumnName, String> columnMap = new LinkedHashMap<>();
        Map<String, ColumnType> typeMap = new LinkedHashMap<>();

        for (int index = 0; index < columns.length; index++) {
            columnMap.put(columns[index], columns[index].getName());
            typeMap.put(columns[index].getName(), types[index]);
        }
        Select select = new Select(operation, columnMap, typeMap);
        return select;
    }

    private ColumnName[] getColumnNames(TableMetadata table) {
        return table.getColumns().keySet().toArray(new ColumnName[table.getColumns().size()]);
    }

    public Join getJoin(String joinId, Relation ... relations){
        Join j = new Join(Operations.SELECT_INNER_JOIN, joinId);
        for(Relation r : relations) {
            j.addJoinRelation(r);
        }
        return j;
    }

    @BeforeClass
    public void setUp() {
        super.setUp();
        DataStoreName dataStoreName = createTestDatastore();

        //Connector with join.
        Set<Operations> operationsC1 = new HashSet<>();
        operationsC1.add(Operations.PROJECT);
        operationsC1.add(Operations.SELECT_OPERATOR);
        operationsC1.add(Operations.FILTER_PK_EQ);
        operationsC1.add(Operations.SELECT_INNER_JOIN);
        operationsC1.add(Operations.SELECT_INNER_JOIN_PARTIALS_RESULTS);

        //Streaming connector.
        Set<Operations> operationsC2 = new HashSet<>();
        operationsC2.add(Operations.PROJECT);
        operationsC2.add(Operations.SELECT_OPERATOR);
        operationsC2.add(Operations.SELECT_WINDOW);

        connector1 = createTestConnector("TestConnector1", dataStoreName, new HashSet<ClusterName>(),operationsC1, "actorRef1");
        connector2 = createTestConnector("TestConnector2", dataStoreName, new HashSet<ClusterName>(),operationsC2, "actorRef2");

        clusterName = createTestCluster("TestCluster1", dataStoreName, connector1.getName());
        CatalogName catalogName = createTestCatalog("demo");
        createTestTables();
    }

    public void createTestTables() {
        String[] columnNames1 = { "id", "user" };
        ColumnType[] columnTypes1 = { ColumnType.INT, ColumnType.TEXT };
        String[] partitionKeys1 = { "id" };
        String[] clusteringKeys1 = { };
        table1 = createTestTable(clusterName, "demo", "table1", columnNames1, columnTypes1, partitionKeys1,
                clusteringKeys1);

        String[] columnNames2 = { "id", "email" };
        ColumnType[] columnTypes2 = { ColumnType.INT, ColumnType.TEXT };
        String[] partitionKeys2 = { "id" };
        String[] clusteringKeys2 = { };
        table2 = createTestTable(clusterName, "demo", "table2", columnNames2, columnTypes2, partitionKeys2,
                clusteringKeys2);
    }

    /**
     * Simple workflow consisting on Project -> Select
     */
    @Test
    public void projectSelect() {
        // Build Logical WORKFLOW
        // Create initial steps (Projects)
        List<LogicalStep> initialSteps = new LinkedList<>();
        Project project = getProject("table1");

        ColumnName[] columns = { new ColumnName(table1.getName(), "id"), new ColumnName(table1.getName(), "user") };
        ColumnType[] types = { ColumnType.INT, ColumnType.TEXT };
        Select select = getSelect(columns, types);


        //Link the elements
        project.setNextStep(select);
        initialSteps.add(project);

        // Add initial steps
        LogicalWorkflow workflow = new LogicalWorkflow(initialSteps);

        //TEST
        ExecutionWorkflow executionWorkflow = null;
        try {
            executionWorkflow = planner.buildExecutionWorkflow("qid", workflow);
        } catch (PlanningException e) {
            LOG.error("connectorChoice test failed", e);
        }
        assertExecutionWorkflow(executionWorkflow, 1, new String[] { connector1.getActorRef().toString() });
    }

    @Test
    public void projectFilterSelect() {
        // Build Logical WORKFLOW
        // Create initial steps (Projects)
        List<LogicalStep> initialSteps = new LinkedList<>();
        Project project = getProject("table1");

        ColumnName[] columns = { new ColumnName(table1.getName(), "id"), new ColumnName(table1.getName(), "user") };
        ColumnType[] types = { ColumnType.INT, ColumnType.TEXT };
        Select select = getSelect(columns, types);

        Filter filter = getFilter(Operations.FILTER_PK_EQ, columns[0], Operator.EQ, new IntegerSelector(42));

        //Link the elements
        project.setNextStep(filter);
        filter.setNextStep(select);
        initialSteps.add(project);

        // Add initial steps
        LogicalWorkflow workflow = new LogicalWorkflow(initialSteps);

        //TEST

        ExecutionWorkflow executionWorkflow = null;
        try {
            executionWorkflow = planner.buildExecutionWorkflow("qid", workflow);
        } catch (PlanningException e) {
            LOG.error("connectorChoice test failed", e);
        }
        assertExecutionWorkflow(executionWorkflow, 1, new String[] { connector1.getActorRef().toString() });

    }

    //
    // Internal methods.
    //

    @Test
    public void defineExecutionPath() {
        List<LogicalStep> initialSteps = new LinkedList<>();
        Project project = getProject("table1");

        ColumnName[] columns = { new ColumnName(table1.getName(), "id"), new ColumnName(table1.getName(), "user") };
        ColumnType[] types = { ColumnType.INT, ColumnType.TEXT };
        Select select = getSelect(columns, types);

        Filter filter = getFilter(Operations.FILTER_PK_EQ, columns[0], Operator.EQ, new IntegerSelector(42));

        //Link the elements
        project.setNextStep(filter);
        filter.setNextStep(select);
        initialSteps.add(project);

        List<ConnectorMetadata> availableConnectors = new ArrayList<>();
        availableConnectors.add(connector1);
        availableConnectors.add(connector2);

        ExecutionPath path = null;
        try {
            path = plannerWrapper.defineExecutionPath(project, availableConnectors);
        } catch (PlanningException e) {
            fail("Not expecting Planning Exception", e);
        }

        assertEquals(path.getInitial(), project, "Invalid initial step");
        assertEquals(path.getLast(), select, "Invalid last step");

        assertEquals(path.getAvailableConnectors().size(), 1, "Invalid size");
        assertEquals(path.getAvailableConnectors().get(0), connector1, "Invalid connector selected");
    }

    @Test
    public void defineExecutionSelectPathNotAvailable() {
        List<LogicalStep> initialSteps = new LinkedList<>();
        Project project = getProject("table1");

        ColumnName[] columns = { new ColumnName(table1.getName(), "id"), new ColumnName(table1.getName(), "user") };
        ColumnType[] types = { ColumnType.INT, ColumnType.TEXT };
        Select select = getSelect(columns, types);

        Filter filter = getFilter(Operations.FILTER_PK_EQ, columns[0], Operator.EQ, new IntegerSelector(42));

        //Link the elements
        project.setNextStep(filter);
        filter.setNextStep(select);
        initialSteps.add(project);

        List<ConnectorMetadata> availableConnectors = new ArrayList<>();
        availableConnectors.add(connector2);

        try {
            ExecutionPath path = plannerWrapper.defineExecutionPath(project, availableConnectors);
            fail("Planning exception expected");
        } catch (PlanningException e) {
            assertNotNull(e, "Expecting Planning exception");
        }

    }

    @Test
    public void mergeExecutionPathsSimpleQuery(){
        List<LogicalStep> initialSteps = new LinkedList<>();
        Project project = getProject("table1");

        ColumnName [] columns = {new ColumnName(table1.getName(), "id"), new ColumnName(table1.getName(), "user")};
        ColumnType [] types = {ColumnType.INT, ColumnType.TEXT};
        Select select = getSelect(columns, types);

        Filter filter = getFilter(Operations.FILTER_PK_EQ, columns[0], Operator.EQ, new IntegerSelector(42));

        //Link the elements
        project.setNextStep(filter);
        filter.setNextStep(select);
        initialSteps.add(project);

        List<ConnectorMetadata> availableConnectors = new ArrayList<>();
        availableConnectors.add(connector1);
        ExecutionPath path = new ExecutionPath(project, select, availableConnectors);

        ExecutionWorkflow executionWorkflow = null;
        try {
            executionWorkflow = plannerWrapper.mergeExecutionPaths(
                    "qid", Arrays.asList(path),
                    new HashMap<UnionStep, Set<ExecutionPath>>());
        } catch (PlanningException e) {
            fail("Not expecting Planning Exception", e);
        }

        assertNotNull(executionWorkflow, "Null execution workflow received");
        assertExecutionWorkflow(executionWorkflow, 1,
                new String [] {connector1.getActorRef().toString()});

    }

    @Test
    public void mergeExecutionPathsJoin(){

        ColumnName [] columns1 = getColumnNames(table1);
        ColumnName [] columns2 = getColumnNames(table2);

        Project project1 = getProject("table1", columns1);
        Project project2 = getProject("table2", columns2);

        ColumnType [] types = {ColumnType.INT, ColumnType.TEXT};
        Select select = getSelect(columns1, types);

        Filter filter = getFilter(Operations.FILTER_PK_EQ, columns1[0], Operator.EQ, new IntegerSelector(42));

        Join join = getJoin("joinId");

        //Link the elements
        project1.setNextStep(filter);
        filter.setNextStep(join);
        project2.setNextStep(join);
        join.setNextStep(select);

        List<ConnectorMetadata> availableConnectors = new ArrayList<>();
        availableConnectors.add(connector1);
        availableConnectors.add(connector2);

        ExecutionPath path1 = new ExecutionPath(project1, filter, availableConnectors);
        ExecutionPath path2 = new ExecutionPath(project2, project2, availableConnectors);

        HashMap<UnionStep, Set<ExecutionPath>> unions = new HashMap<>();
        Set<ExecutionPath> paths = new HashSet<>();
        paths.add(path1);
        paths.add(path2);
        unions.put(join, paths);

        ExecutionWorkflow executionWorkflow = null;
        try {
            executionWorkflow = plannerWrapper.mergeExecutionPaths(
                    "qid", new ArrayList<>(paths),
                    unions);
        } catch (PlanningException e) {
            fail("Not expecting Planning Exception", e);
        }

        assertNotNull(executionWorkflow, "Null execution workflow received");
        assertExecutionWorkflow(executionWorkflow, 1,
                new String [] {connector1.getActorRef().toString()});

    }

    @Test
    public void mergeExecutionPathsPartialJoin(){

        ColumnName [] columns1 = getColumnNames(table1);
        ColumnName [] columns2 = getColumnNames(table2);

        Project project1 = getProject("table1", columns1);
        Project project2 = getProject("table2", columns2);

        ColumnType [] types = {ColumnType.INT, ColumnType.TEXT};
        Select select = getSelect(columns1, types);

        Filter filter = getFilter(Operations.FILTER_PK_EQ, columns1[0], Operator.EQ, new IntegerSelector(42));

        Relation r = new Relation(new ColumnSelector(columns1[0]), Operator.EQ,
                new ColumnSelector(columns2[0]));

        Join join = getJoin("joinId", r);

        //Link the elements
        project1.setNextStep(filter);
        filter.setNextStep(join);
        project2.setNextStep(join);
        join.setNextStep(select);

        List<ConnectorMetadata> availableConnectors1 = new ArrayList<>();
        availableConnectors1.add(connector1);

        List<ConnectorMetadata> availableConnectors2 = new ArrayList<>();
        availableConnectors2.add(connector2);

        ExecutionPath path1 = new ExecutionPath(project1, filter, availableConnectors1);
        ExecutionPath path2 = new ExecutionPath(project2, project2, availableConnectors2);

        HashMap<UnionStep, Set<ExecutionPath>> unions = new HashMap<>();
        Set<ExecutionPath> paths = new HashSet<>();
        paths.add(path1);
        paths.add(path2);
        unions.put(join, paths);

        ExecutionWorkflow executionWorkflow = null;
        try {
            executionWorkflow = plannerWrapper.mergeExecutionPaths(
                    "qid", new ArrayList<>(paths),
                    unions);
        } catch (PlanningException e) {
            fail("Not expecting Planning Exception", e);
        }

        assertNotNull(executionWorkflow, "Null execution workflow received");
        assertExecutionWorkflow(executionWorkflow, 2,
                new String [] {connector2.getActorRef().toString(), connector1.getActorRef().toString()});

    }

    @Test
    public void mergeExecutionPathsPartialStreamingJoin(){

        ColumnName [] columns1 = getColumnNames(table1);
        ColumnName [] columns2 = getColumnNames(table2);

        Project project1 = getProject("table1", columns1);
        Project project2 = getProject("table2", columns2);

        ColumnType [] types = {ColumnType.INT, ColumnType.TEXT};
        Select select = getSelect(columns1, types);

        Filter filter = getFilter(Operations.FILTER_PK_EQ, columns1[0], Operator.EQ, new IntegerSelector(42));

        Relation r = new Relation(new ColumnSelector(columns1[0]), Operator.EQ,
                new ColumnSelector(columns2[0]));

        Window streamingWindow = new Window(Operations.SELECT_WINDOW, WindowType.NUM_ROWS);
        streamingWindow.setNumRows(10);

        Join join = getJoin("joinId", r);

        //Link the elements
        project1.setNextStep(filter);
        filter.setNextStep(join);
        project2.setNextStep(streamingWindow);
        streamingWindow.setNextStep(join);
        join.setNextStep(select);

        List<ConnectorMetadata> availableConnectors1 = new ArrayList<>();
        availableConnectors1.add(connector1);

        List<ConnectorMetadata> availableConnectors2 = new ArrayList<>();
        availableConnectors2.add(connector2);

        ExecutionPath path1 = new ExecutionPath(project1, filter, availableConnectors1);
        ExecutionPath path2 = new ExecutionPath(project2, streamingWindow, availableConnectors2);

        HashMap<UnionStep, Set<ExecutionPath>> unions = new HashMap<>();
        Set<ExecutionPath> paths = new HashSet<>();
        paths.add(path1);
        paths.add(path2);
        unions.put(join, paths);

        ExecutionWorkflow executionWorkflow = null;
        try {
            executionWorkflow = plannerWrapper.mergeExecutionPaths(
                    "qid", new ArrayList<>(paths),
                    unions);
        } catch (PlanningException e) {
            fail("Not expecting Planning Exception", e);
        }

        assertNotNull(executionWorkflow, "Null execution workflow received");
        assertExecutionWorkflow(executionWorkflow, 2,
                new String [] {connector2.getActorRef().toString(), connector1.getActorRef().toString()});

    }



    @Test
    public void storageWorkflowTest() {
        DataStoreName dataStoreName = createTestDatastore();
        Set<Operations> operations = new HashSet<>();
        operations.add(Operations.INSERT);
        ConnectorMetadata connectorMetadata = createTestConnector("cassandraConnector", dataStoreName,new HashSet<ClusterName>(), operations,
                "1");
        createTestCluster("cluster", dataStoreName, connectorMetadata.getName());

        String[] columnNames = { "name", "gender", "age", "bool", "phrase", "email" };
        ColumnType[] columnTypes = { ColumnType.TEXT, ColumnType.TEXT, ColumnType.INT, ColumnType.BOOLEAN,
                ColumnType.TEXT,
                ColumnType.TEXT };
        String[] partitionKeys = { "name", "age" };
        String[] clusteringKeys = { "gender" };
        createTestTable(new ClusterName("cluster"), "demo", "users", columnNames, columnTypes, partitionKeys,
                clusteringKeys);
        String query = "Insert into demo.users(name,gender,age,bool,phrase,email) values ('pepe','male',23,true,'this is the phrase','mail@mail.com';";
        List<ColumnName> columns = new ArrayList<>();
        List<Selector> values = new ArrayList<>();
        columns.add(new ColumnName(new TableName("demo", "users"), "name"));
        columns.add(new ColumnName(new TableName("demo", "users"), "gender"));
        columns.add(new ColumnName(new TableName("demo", "users"), "age"));
        columns.add(new ColumnName(new TableName("demo", "users"), "bool"));
        columns.add(new ColumnName(new TableName("demo", "users"), "phrase"));
        columns.add(new ColumnName(new TableName("demo", "users"), "email"));

        values.add(new StringSelector("'pepe'"));
        values.add(new StringSelector("'male'"));
        values.add(new IntegerSelector(23));
        values.add(new BooleanSelector(true));
        values.add(new StringSelector("'this is the phrase'"));
        values.add(new StringSelector("'mail@mail.com'"));

        StorageStatement insertIntoStatement = new InsertIntoStatement(new TableName("demo", "users"), columns, values,
                true);

        BaseQuery baseQuery = new BaseQuery("insertId", query, new CatalogName("system"));

        StorageParsedQuery parsedQuery = new StorageParsedQuery(baseQuery, insertIntoStatement);
        StorageValidatedQuery storageValidatedQuery = new StorageValidatedQuery(parsedQuery);

        Planner planner = new Planner();
        try {
            ExecutionWorkflow storageWorkflow = planner.buildExecutionWorkflow(storageValidatedQuery);
            Assert.assertEquals(((StorageWorkflow) storageWorkflow).getClusterName().getName(), "cluster");
            Assert.assertEquals(((StorageWorkflow) storageWorkflow).getTableMetadata().getName().getName(), "users");
        } catch (PlanningException e) {
            Assert.fail(e.getMessage());
        }

    }

}