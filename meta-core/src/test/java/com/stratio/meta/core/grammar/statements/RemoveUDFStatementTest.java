/*
 * Stratio Meta
 *
 * Copyright (c) 2014, Stratio, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.stratio.meta.core.grammar.statements;

import com.stratio.meta.core.grammar.ParsingTest;
import org.testng.annotations.Test;

public class RemoveUDFStatementTest extends ParsingTest {

    //REMOVE UDF
    @Test
    public void removeUDF() {
        String inputText = "REMOVE UDF \"jar.name\";";
        testRegularStatement(inputText, "removeUDF");
    }


    @Test
    public void remove_udf_not_expected_word() {
        String inputText = "REMOVE UDF \"jar.name\" NOW;";
        testParseFails(inputText, "remove_udf_not_expected_word");
    }
}