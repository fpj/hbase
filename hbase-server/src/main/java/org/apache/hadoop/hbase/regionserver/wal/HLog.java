/**
 * Copyright 2010 The Apache Software Foundation
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.NavigableSet;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.regionserver.wal.HLogMetrics.Metric;
import org.apache.hadoop.hbase.util.Bytes;

public interface HLog {   
    
    public interface Reader {
        void init(FileSystem fs, Path path, Configuration c) throws IOException;
        void close() throws IOException;
        Entry next() throws IOException;
        Entry next(Entry reuse) throws IOException;
        void seek(long pos) throws IOException;
        long getPosition() throws IOException;
      }

      public interface Writer {
        void init(FileSystem fs, Path path, Configuration c) throws IOException;
        void close() throws IOException;
        void sync() throws IOException;
        void append(Entry entry) throws IOException;
        long getLength() throws IOException;
      }
      
      /**
       * Utility class that lets us keep track of the edit with it's key
       * Only used when splitting logs
       */
      public static class Entry implements Writable {
        private WALEdit edit;
        private HLogKey key;

        public Entry() {
          edit = new WALEdit();
          key = new HLogKey();
        }

        /**
         * Constructor for both params
         * @param edit log's edit
         * @param key log's key
         */
        public Entry(HLogKey key, WALEdit edit) {
          super();
          this.key = key;
          this.edit = edit;
        }
        /**
         * Gets the edit
         * @return edit
         */
        public WALEdit getEdit() {
          return edit;
        }
        /**
         * Gets the key
         * @return key
         */
        public HLogKey getKey() {
          return key;
        }

        /**
         * Set compression context for this entry.
         * @param compressionContext Compression context
         */
        public void setCompressionContext(CompressionContext compressionContext) {
          edit.setCompressionContext(compressionContext);
          key.setCompressionContext(compressionContext);
        }

        @Override
        public String toString() {
          return this.key + "=" + this.edit;
        }

        @Override
        public void write(DataOutput dataOutput) throws IOException {
          this.key.write(dataOutput);
          this.edit.write(dataOutput);
        }

        @Override
        public void readFields(DataInput dataInput) throws IOException {
          this.key.readFields(dataInput);
          this.edit.readFields(dataInput);
        }
      }
          
      public void registerWALActionsListener(final WALActionsListener listener);
      public boolean unregisterWALActionsListener(final WALActionsListener listener);
      public long getFilenum();
      public void setSequenceNumber(final long newvalue);
      public long getSequenceNumber();
      public byte [][] rollWriter() throws FailedLogCloseException, IOException;
      public byte [][] rollWriter(boolean force) throws FailedLogCloseException, IOException;
      public void close() throws IOException;
      public void closeAndDelete() throws IOException;
      public long append(HRegionInfo regionInfo, HLogKey logKey, WALEdit logEdit,
                                      HTableDescriptor htd, boolean doSync)
      throws IOException;
      public void append(HRegionInfo info, byte [] tableName, WALEdit edits,
                                      final long now, HTableDescriptor htd)
      throws IOException;
      public long appendNoSync(HRegionInfo info, byte [] tableName, WALEdit edits, 
                                      UUID clusterId, final long now, HTableDescriptor htd)
      throws IOException;
      public long append(HRegionInfo info, byte [] tableName, WALEdit edits, 
                                      UUID clusterId, final long now, HTableDescriptor htd)
      throws IOException;
      public void hsync() throws IOException;
      public void hflush() throws IOException;
      public void sync() throws IOException;
      public void sync(long txid) throws IOException;
      public long startCacheFlush(final byte[] encodedRegionName);
      public void completeCacheFlush(final byte [] encodedRegionName,
                                      final byte [] tableName, final long logSeqId, final boolean isMetaRegion)
      throws IOException;
      public void abortCacheFlush(byte[] encodedRegionName);
      public WALCoprocessorHost getCoprocessorHost();
}
