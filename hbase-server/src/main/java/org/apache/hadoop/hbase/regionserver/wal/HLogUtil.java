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

import java.io.IOException;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.regionserver.wal.HLog.Reader;
import org.apache.hadoop.hbase.regionserver.wal.HLog.Writer;
import org.apache.hadoop.hbase.util.FSUtils;

import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.util.Bytes;


public class HLogUtil {
    static final Log LOG = LogFactory.getLog(HLogUtil.class);
    
    public static final byte [] METAFAMILY = Bytes.toBytes("METAFAMILY");
    
    /** File Extension used while splitting an HLog into regions (HBASE-2312) */
    public static final String SPLITTING_EXT = "-splitting";
    public static final boolean SPLIT_SKIP_ERRORS_DEFAULT = false; 
    
    /*
     * Name of directory that holds recovered edits written by the wal log
     * splitting code, one per region
     */
    static final String RECOVERED_EDITS_DIR = "recovered.edits";
    static final Pattern EDITFILES_NAME_PATTERN =
      Pattern.compile("-?[0-9]+");
    static final String RECOVERED_LOG_TMPFILE_SUFFIX = ".temp";
    
    /**
     * @param family
     * @return true if the column is a meta column
     */
    public static boolean isMetaFamily(byte [] family) {
      return Bytes.equals(METAFAMILY, family);
    }
    
    @SuppressWarnings("unchecked")
    public static Class<? extends HLogKey> getKeyClass(Configuration conf) {
       return (Class<? extends HLogKey>)
         conf.getClass("hbase.regionserver.hlog.keyclass", HLogKey.class);
    }

    public static HLogKey newKey(Configuration conf) throws IOException {
      Class<? extends HLogKey> keyClass = getKeyClass(conf);
      try {
        return keyClass.newInstance();
      } catch (InstantiationException e) {
        throw new IOException("cannot create hlog key");
      } catch (IllegalAccessException e) {
        throw new IOException("cannot create hlog key");
      }
    }
    
    /**
     * Pattern used to validate a HLog file name
     */
    private static final Pattern pattern = Pattern.compile(".*\\.\\d*");
    
    /**
     * @param filename name of the file to validate
     * @return <tt>true</tt> if the filename matches an HLog, <tt>false</tt>
     *         otherwise
     */
    public static boolean validateHLogFilename(String filename) {
      return pattern.matcher(filename).matches();
    }
    
    /**
     * Construct the HLog directory name
     *
     * @param serverName Server name formatted as described in {@link ServerName}
     * @return the relative HLog directory name, e.g. <code>.logs/1.example.org,60030,12345</code>
     * if <code>serverName</code> passed is <code>1.example.org,60030,12345</code>
     */
    public static String getHLogDirectoryName(final String serverName) {
      StringBuilder dirName = new StringBuilder(HConstants.HREGION_LOGDIR_NAME);
      dirName.append("/");
      dirName.append(serverName);
      return dirName.toString();
    }
        
    /**
     * @param regiondir This regions directory in the filesystem.
     * @return The directory that holds recovered edits files for the region
     * <code>regiondir</code>
     */
    public static Path getRegionDirRecoveredEditsDir(final Path regiondir) {
      return new Path(regiondir, RECOVERED_EDITS_DIR);
    }
    
    /**
     * Returns sorted set of edit files made by wal-log splitter, excluding files
     * with '.temp' suffix.
     * @param fs
     * @param regiondir
     * @return Files in passed <code>regiondir</code> as a sorted set.
     * @throws IOException
     */
    public static NavigableSet<Path> getSplitEditFilesSorted(final FileSystem fs,
                                    final Path regiondir)
                                    throws IOException {
        NavigableSet<Path> filesSorted = new TreeSet<Path>();
        Path editsdir = HLogUtil.getRegionDirRecoveredEditsDir(regiondir);
        if (!fs.exists(editsdir)) return filesSorted;
        FileStatus[] files = FSUtils.listStatus(fs, editsdir, new PathFilter() {
            @Override
            public boolean accept(Path p) {
                boolean result = false;
                try {
                    // Return files and only files that match the editfile names pattern.
                    // There can be other files in this directory other than edit files.
                    // In particular, on error, we'll move aside the bad edit file giving
                    // it a timestamp suffix.  See moveAsideBadEditsFile.
                    Matcher m = HLogUtil.EDITFILES_NAME_PATTERN.matcher(p.getName());
                    result = fs.isFile(p) && m.matches();
                    // Skip the file whose name ends with RECOVERED_LOG_TMPFILE_SUFFIX,
                    // because it means splithlog thread is writting this file.
                    if (p.getName().endsWith(HLogUtil.RECOVERED_LOG_TMPFILE_SUFFIX)) {
                        result = false;
                    }
                } catch (IOException e) {
                    LOG.warn("Failed isFile check on " + p);
                }
                return result;
            }
        });
        if (files == null) return filesSorted;
        for (FileStatus status: files) {
            filesSorted.add(status.getPath());
        }
        return filesSorted;
    }
    
    /**
     * Move aside a bad edits file.
     * @param fs
     * @param edits Edits file to move aside.
     * @return The name of the moved aside file.
     * @throws IOException
     */
    public static Path moveAsideBadEditsFile(final FileSystem fs,
        final Path edits)
    throws IOException {
      Path moveAsideName = new Path(edits.getParent(), edits.getName() + "." +
        System.currentTimeMillis());
      if (!fs.rename(edits, moveAsideName)) {
        LOG.warn("Rename failed from " + edits + " to " + moveAsideName);
      }
      return moveAsideName;
    }

}
