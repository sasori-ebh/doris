// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.analysis;

import org.apache.doris.catalog.Env;
import org.apache.doris.catalog.FakeEnv;
import org.apache.doris.common.AnalysisException;
import org.apache.doris.common.UserException;
import org.apache.doris.mysql.privilege.Auth;
import org.apache.doris.mysql.privilege.MockedAuth;
import org.apache.doris.qe.ConnectContext;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ShowFunctionsStmtTest {
    @Mocked
    private Analyzer analyzer;
    private Env env;

    @Mocked
    private Auth auth;
    @Mocked
    private ConnectContext ctx;
    private FakeEnv fakeEnv;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {
        fakeEnv = new FakeEnv();
        env = AccessTestUtil.fetchAdminCatalog();
        MockedAuth.mockedAuth(auth);
        MockedAuth.mockedConnectContext(ctx, "root", "192.188.3.1");
        FakeEnv.setEnv(env);

        new Expectations() {
            {
                analyzer.getDefaultDb();
                minTimes = 0;
                result = "testDb";

                analyzer.getEnv();
                minTimes = 0;
                result = env;

                analyzer.getClusterName();
                minTimes = 0;
                result = "testCluster";
            }
        };
    }

    @Test
    public void testNormal() throws UserException {
        ShowFunctionsStmt stmt = new ShowFunctionsStmt(null, true, true, "%year%", null);
        stmt.analyze(analyzer);
        Assert.assertEquals("SHOW FULL BUILTIN FUNCTIONS FROM `testDb` LIKE `%year%`", stmt.toString());
    }

    @Test
    public void testUnsupportFilter() throws UserException {
        SlotRef slotRef = new SlotRef(null, "Signature");
        StringLiteral stringLiteral = new StringLiteral("year(DATETIME)");
        BinaryPredicate binaryPredicate = new BinaryPredicate(BinaryPredicate.Operator.EQ, slotRef, stringLiteral);
        ShowFunctionsStmt stmt = new ShowFunctionsStmt(null, true, true, null, binaryPredicate);
        expectedEx.expect(AnalysisException.class);
        expectedEx.expectMessage("Only support like 'function_pattern' syntax.");
        stmt.analyze(analyzer);
    }

}
