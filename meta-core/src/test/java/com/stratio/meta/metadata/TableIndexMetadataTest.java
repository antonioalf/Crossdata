package com.stratio.meta.metadata;

import com.datastax.driver.core.TableMetadata;
import com.stratio.meta.cassandra.BasicCassandraTest;
import com.stratio.meta.server.metadata.MetadataManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class TableIndexMetadataTest extends BasicCassandraTest{

    private MetadataManager _metadataManager = null;

    @BeforeClass
    @Override
    public void setUpBeforeClass(){
        setUpBeforeClass();
        loadTestData("demo", "demoKeyspace.cql");
        _metadataManager = new MetadataManager();
        _metadataManager.loadMetadata();
    }

    @Test
    public void testGetColumnIndexes(){

        String keyspace = "demo";
        String table = "users";
        String [] columns = {
                "age", "bool", "gender", //Two indexes, one on Cassandra and other in Lucene
                "animal", "food", "listz", "mapz", "name", "number", "phrase", "setz"};

        TableMetadata metadata = _metadataManager.getTableMetadata(keyspace, table);
        assertNotNull("Cannot retrieve table metadata", metadata);
        assertEquals("Retrieved table name does not match", table, metadata.getName());

        TableIndexMetadata tim = new TableIndexMetadata(metadata);
        assertNotNull("Cannot build wrapper for TableMetadata", tim);

        Map<String, List<CustomIndexMetadata>> indexes = tim.getColumnIndexes();
        assertEquals("Invalid number of indexes", 4, indexes.size());

        for(String column : columns){
            assertTrue("Column does not have an index", indexes.containsKey(column));
            if(column.equals("age") || column.equals("bool") || column.equals("gender")){
                assertEquals("Invalid number of index associated with a column",
                        2L, indexes.get(column).size());
            }else{
                assertEquals("Invalid number of index associated with a column",
                        1L, indexes.get(column).size());
            }
        }

    }
}
