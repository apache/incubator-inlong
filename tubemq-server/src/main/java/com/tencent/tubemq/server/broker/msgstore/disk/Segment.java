/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tubemq.server.broker.msgstore.disk;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/***
 * Storage segment, usually implemented in file format.
 */
public interface Segment {

    int close() throws IOException;

    long append(final ByteBuffer buf) throws IOException;

    long flush(boolean force) throws IOException;

    int checkAndSetExpired(final long checkTimestamp, final long maxValidTimeMs);

    boolean isClosed();

    long getStart();

    long getLast();

    long getCommitLast();

    File getFile();

    long getCachedSize();

    long getCommitSize();

    boolean isExpired();

    boolean contains(final long offset);

    FileChannel getFileChannel();

    boolean isMutable();

    void setMutable(boolean mutable);

    RecordView getViewRef(final long start, final long offset, final long limit);

    RecordView getViewRef();

    void relViewRef();

    boolean equals(Segment other);

}
