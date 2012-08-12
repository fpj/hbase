/**
 * Copyright 2011 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.regionserver.wal;

import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;

/**
 * An Utility testcase that returns the number of log files that
 * were rolled to be accessed from outside packages.
 * 
 * This class makes available methods that are package protected.
 *  This is interesting for test only.
 */
public class HLogUtilsForTests {
  
  /**
   * 
   * @param log
   * @return
   */
  public static int getNumLogFiles(HLog log) {
    return ((FSHLog) log).getNumLogFiles();
  }

  public static int getNumEntries(HLog log) {
    return ((FSHLog) log).getNumEntries();
  }
}
