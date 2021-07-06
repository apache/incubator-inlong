/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.codec;

import java.util.List;

import org.apache.inlong.dataproxy.config.EncryptConfigEntry;

public class EncodeObject {
    private static final String MESSAGE_ID_PREFIX = "messageId=";

    private byte[] bodyBytes;
    private String attributes;
    private String messageId;
    private int msgtype;
    private List<byte[]> bodylist;
    private String commonattr = "";
    private String messageKey = "data";
    private String proxyIp = "";
    // private long seqId
    private long dt;
    // package time
    private long packageTime = System.currentTimeMillis();
    private int cnt = -1;
    private boolean isReport = false;
    private boolean isBidTransfer = false;
    private boolean isSupportLF = false;
    private boolean isAuth = false;
    private boolean isEncrypt = false;
    private boolean isCompress = true;
    private int bidNum;
    private int tidNum;
    private String bid;
    private String tid;
    private short load;
    private String userName = "";
    private String secretKey = "";
    private String msgUUID = null;
    private EncryptConfigEntry encryptEntry = null;


    private boolean isException = false;
    private ErrorCode exceptionError = null;

    /* Used by de_serialization. msgtype=7/8*/
    public EncodeObject() {
    }

    /* Used by de_serialization. */
    public EncodeObject(byte[] bodyBytes, String attributes) {
        this.bodyBytes = bodyBytes;
        this.attributes = attributes;
        this.messageId = "";
        String[] tokens = attributes.split("&");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].startsWith("messageId=")) {
                this.messageId = tokens[i].substring(MESSAGE_ID_PREFIX.length(), tokens[i].length());
                break;
            }
        }
    }

    /* Used by serialization.But never used */
// old version:we need add message id by attr
    public EncodeObject(byte[] bodyBytes, String attributes, String messageId) {
        this.bodyBytes = bodyBytes;
        this.messageId = messageId;
        this.attributes = attributes + "&messageId=" + messageId;
    }

    // used for bytes initializtion,msgtype=3/5
    public EncodeObject(byte[] bodyBytes, String attributes, String messageId,
                        int msgtype, boolean isCompress, final String bid) {
        this.bodyBytes = bodyBytes;
        this.messageId = messageId;
        this.attributes = attributes + "&messageId=" + messageId;
        this.msgtype = msgtype;
        this.bid = bid;
        this.isCompress = isCompress;
    }

    // used for bodylist initializtion,msgtype=3/5
    public EncodeObject(List<byte[]> bodyList, String attributes, String messageId,
                        int msgtype, boolean isCompress, final String bid) {
        this.bodylist = bodyList;
        this.messageId = messageId;
        this.attributes = attributes + "&messageId=" + messageId;
        this.msgtype = msgtype;
        this.bid = bid;
        this.isCompress = isCompress;
    }

    // used for bytes initializtion,msgtype=7/8
    public EncodeObject(byte[] bodyBytes, int msgtype, boolean isCompress, boolean isReport,
                        boolean isBidTransfer, long dt, long seqId, String bid, String tid, String commonattr) {
        this.bodyBytes = bodyBytes;
        this.msgtype = msgtype;
        this.isCompress = isCompress;
        this.isReport = isReport;
        this.dt = dt;
        this.isBidTransfer = isBidTransfer;
        this.commonattr = commonattr;
        this.messageId = String.valueOf(seqId);
        this.bid = bid;
        this.tid = tid;
    }

    // used for bodylist initializtion,msgtype=7/8
    public EncodeObject(List<byte[]> bodyList, int msgtype, boolean isCompress,
                        boolean isReport, boolean isBidTransfer, long dt,
                        long seqId, String bid, String tid, String commonattr) {
        this.bodylist = bodyList;
        this.msgtype = msgtype;
        this.isCompress = isCompress;
        this.isReport = isReport;
        this.dt = dt;
        this.isBidTransfer = isBidTransfer;
        this.commonattr = commonattr;
        this.messageId = String.valueOf(seqId);
        this.bid = bid;
        this.tid = tid;
    }

    // file agent, used for bytes initializtion,msgtype=7/8
    public EncodeObject(byte[] bodyBytes, int msgtype, boolean isCompress,
                        boolean isReport, boolean isBidTransfer, long dt,
                        long seqId, String bid, String tid, String commonattr, String messageKey, String proxyIp) {
        this.bodyBytes = bodyBytes;
        this.msgtype = msgtype;
        this.isCompress = isCompress;
        this.isReport = isReport;
        this.dt = dt;
        this.isBidTransfer = isBidTransfer;
        this.commonattr = commonattr;
        this.messageId = String.valueOf(seqId);
        this.bid = bid;
        this.tid = tid;
        this.messageKey = messageKey;
        this.proxyIp = proxyIp;
    }

    // file agent, used for bodylist initializtion,msgtype=7/8
    public EncodeObject(List<byte[]> bodyList, int msgtype, boolean isCompress,
                        boolean isReport, boolean isBidTransfer, long dt,
                        long seqId, String bid, String tid, String commonattr, String messageKey, String proxyIp) {
        this.bodylist = bodyList;
        this.msgtype = msgtype;
        this.isCompress = isCompress;
        this.isReport = isReport;
        this.dt = dt;
        this.isBidTransfer = isBidTransfer;
        this.commonattr = commonattr;
        this.messageId = String.valueOf(seqId);
        this.bid = bid;
        this.tid = tid;
        this.messageKey = messageKey;
        this.proxyIp = proxyIp;
    }

    public String getMsgUUID() {
        return msgUUID;
    }

    public void setMsgUUID(String msgUUID) {
        this.msgUUID = msgUUID;
    }

    public boolean isBidTransfer() {
        return isBidTransfer;
    }

    public void setBidTransfer(boolean isBidTransfer) {
        this.isBidTransfer = isBidTransfer;
    }

    public short getLoad() {
        return load;
    }

    public void setLoad(short load) {
        this.load = load;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public void setMsgtype(int msgtype) {
        this.msgtype = msgtype;
    }

    public void setBodyBytes(byte[] bodyBytes) {
        this.bodyBytes = bodyBytes;
    }

    public boolean isReport() {
        return isReport;
    }

    public void setReport(boolean isReport) {
        this.isReport = isReport;
    }

    public boolean isAuth() {
        return isAuth;
    }

    public void setAuth(boolean auth, final String userName, final String secretKey) {
        this.isAuth = auth;
        this.userName = userName;
        this.secretKey = secretKey;
    }

    public String getUserName() {
        return userName;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public boolean isEncrypt() {
        return isEncrypt;
    }

    public EncryptConfigEntry getEncryptEntry() {
        return encryptEntry;
    }

    public void setEncryptEntry(boolean isEncrypt, String userName, EncryptConfigEntry encryptEntry) {
        this.isEncrypt = isEncrypt;
        if (userName != null) {
            this.userName = userName;
        }
        this.encryptEntry = encryptEntry;
    }

    public int getBidNum() {
        return bidNum;
    }

    public void setBidNum(int bidNum) {
        this.bidNum = bidNum;
    }

    public int getTidNum() {
        return tidNum;
    }

    public void setTidNum(int tidNum) {
        this.tidNum = tidNum;
    }

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }

    public long getPackageTime() {
        return packageTime;
    }

    public void setPackageTime(long packageTime) {
        this.packageTime = packageTime;
    }

    public String getCommonattr() {
        return commonattr;
    }

    public void setCommonattr(String commonattr) {
        this.commonattr = commonattr;
    }

    public boolean isCompress() {
        return isCompress;
    }

    public List<byte[]> getBodylist() {
        return bodylist;
    }

    public int getMsgtype() {
        return msgtype;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public String getAttributes() {
        return attributes;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getProxyIp() {
        return proxyIp;
    }

    public void setProxyIp(String proxyIp) {
        this.proxyIp = proxyIp;
    }

    public boolean isSupportLF() {
        return isSupportLF;
    }

    public void setSupportLF(boolean supportLF) {
        isSupportLF = supportLF;
    }

    public int getCnt() {
        return cnt;
    }

    public int getRealCnt() {
        if (bodylist != null) {
            return bodylist.size();
        }
        return 1;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public boolean isException() {
        return isException;
    }

    public void setException(boolean exception) {
        isException = exception;
    }

    public ErrorCode getExceptionError() {
        return exceptionError;
    }

    public void setExceptionError(ErrorCode exceptionError) {
        this.exceptionError = exceptionError;
    }
}
