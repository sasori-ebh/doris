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

suite("regression_test_dynamic_table", "dynamic_table"){
    // prepare test table

    def load_json_data = {table_name, vec_flag, format_flag, read_flag, file_name, expect_success ->
        // load the json data
        streamLoad {
            table "${table_name}"

            // set http request header params
            set 'enable_vectorized_engine', vec_flag
            set 'read_json_by_line', read_flag
            set 'format', format_flag
            set 'read_json_by_line', read_flag

            file file_name // import json file
            time 10000 // limit inflight 10s

            // if declared a check callback, the default check condition will ignore.
            // So you must check all condition

            check { result, exception, startTime, endTime ->
                if (exception != null) {
                        throw exception
                }
                logger.info("Stream load ${file_name} result: ${result}".toString())
                def json = parseJson(result)
                if (expect_success == "false") {
                    assertEquals("fail", json.Status.toLowerCase())
                } else {
                    assertEquals("success", json.Status.toLowerCase())
                    assertEquals(json.NumberTotalRows, json.NumberLoadedRows + json.NumberUnselectedRows)
                    assertTrue(json.NumberLoadedRows > 0 && json.LoadBytes > 0)
                }
            }
        }
    }

    def real_alter_res = "true"
    def res = "null"
    def wait_for_alter_finish = "null"
    def check_time = 30
    def json_load = {src_json, table_name ->
        //create table
        sql "DROP TABLE IF EXISTS ${table_name}"
        sql """
            CREATE TABLE IF NOT EXISTS ${table_name} (
                id bigint,
                ...
            )
            DUPLICATE KEY(`id`)
            DISTRIBUTED BY RANDOM BUCKETS 5 
            properties("replication_num" = "1");
        """

        //stream load src_json
        load_json_data.call(table_name, 'true', 'json', 'true', src_json, 'true')
        sleep(1000)
    }
    def json_load_nested = {src_json, table_name ->
        //create table
        sql "DROP TABLE IF EXISTS ${table_name}"
        sql """
            CREATE TABLE IF NOT EXISTS ${table_name} (
                qid bigint,
		creationdate datetime,
                `answers.date` array<datetime>,
                `title` string,
		INDEX creation_date_idx(`creationdate`) USING INVERTED COMMENT 'creationdate index',
 		INDEX title_idx(`title`) USING INVERTED PROPERTIES("parser"="standard") COMMENT 'title index',
		...
            )
            DUPLICATE KEY(`qid`)
            DISTRIBUTED BY RANDOM BUCKETS 5 
            properties("replication_num" = "1");
        """

        //stream load src_json
        load_json_data.call(table_name, 'true', 'json', 'true', src_json, 'true')
        sleep(1000)
    }
    json_load("btc_transactions.json", "test_btc_json")
    json_load("ghdata_sample.json", "test_ghdata_json")
    json_load("nbagames_sample.json", "test_nbagames_json")
    json_load_nested("es_nested.json", "test_es_nested_json")
}
