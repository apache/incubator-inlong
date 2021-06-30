/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common.fielddef;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.RegexDef;
import org.apache.inlong.tubemq.server.common.TServerConstants;


public enum WebFieldDef {

    // Note: Due to compatibility considerations,
    //      the defined fields in the scheme are forbidden to be modified,
    //      only new fields can be added

    TOPICNAME(0, "topicName", "topic", WebFieldType.STRING,
            "Topic name", TBaseConstants.META_MAX_TOPICNAME_LENGTH,
            RegexDef.TMP_STRING),
    GROUPNAME(1, "groupName", "group", WebFieldType.STRING,
            "Group name", TBaseConstants.META_MAX_GROUPNAME_LENGTH,
            RegexDef.TMP_GROUP),
    PARTITIONID(2, "partitionId", "pid", WebFieldType.INT,
            "Partition id", RegexDef.TMP_NUMBER),
    CREATEUSER(3, "createUser", "cur", WebFieldType.STRING,
            "Record creator", TBaseConstants.META_MAX_USERNAME_LENGTH,
            RegexDef.TMP_STRING),
    MODIFYUSER(4, "modifyUser", "mur", WebFieldType.STRING,
            "Record modifier", TBaseConstants.META_MAX_USERNAME_LENGTH,
            RegexDef.TMP_STRING),

    MANUALOFFSET(5, "manualOffset", "offset", WebFieldType.LONG,
            "Reset offset value", RegexDef.TMP_NUMBER),
    MSGCOUNT(6, "msgCount", "cnt", WebFieldType.INT,
            "Number of returned messages", RegexDef.TMP_NUMBER),
    FILTERCONDS(7, "filterConds", "flts", WebFieldType.COMPSTRING,
            "Filter condition items", TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_LENGTH,
            TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT, RegexDef.TMP_FILTER),
    REQUIREREALOFFSET(8, "requireRealOffset", "dko", WebFieldType.BOOLEAN,
            "Require return disk offset details"),
    NEEDREFRESH(9, "needRefresh", "nrf", WebFieldType.BOOLEAN,
            "Require refresh data"),

    COMPSGROUPNAME(10, "groupName", "group", WebFieldType.COMPSTRING,
            "Group name", TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                   RegexDef.TMP_GROUP),
    COMPSTOPICNAME(11, "topicName", "topic", WebFieldType.COMPSTRING,
            "Topic name", TBaseConstants.META_MAX_TOPICNAME_LENGTH,
            RegexDef.TMP_STRING),
    COMPSPARTITIONID(12, "partitionId", "pid", WebFieldType.COMPINT,
            "Partition id", RegexDef.TMP_NUMBER),
    CALLERIP(13, "callerIp", "cip", WebFieldType.STRING,
            "Caller ip address", TBaseConstants.META_MAX_CLIENT_HOSTNAME_LENGTH),
    BROKERID(14, "brokerId", "brokerId", WebFieldType.INT,
            "Broker ID", RegexDef.TMP_NUMBER),

    COMPSBROKERID(15, "brokerId", "brokerId", WebFieldType.COMPINT,
            "Broker ID", RegexDef.TMP_NUMBER),
    WITHIP(16, "withIp", "ip", WebFieldType.BOOLEAN,
            "Require return ip information, default is false"),
    WITHDIVIDE(17, "divide", "div", WebFieldType.BOOLEAN,
            "Need to divide the returned result, default is false"),
    SRCGROUPNAME(18, "sourceGroupName", "srcGroup", WebFieldType.STRING,
            "Offset clone source group name", TBaseConstants.META_MAX_GROUPNAME_LENGTH,
            RegexDef.TMP_GROUP),
    TGTCOMPSGROUPNAME(19, "targetGroupName", "tgtGroup",
            WebFieldType.COMPSTRING, "Offset clone target group name",
            TBaseConstants.META_MAX_GROUPNAME_LENGTH, RegexDef.TMP_GROUP),

    MANUALSET(20, "manualSet", "manSet",
            WebFieldType.BOOLEAN, "Whether manual offset setting mode"),
    OFFSETJSON(21, "offsetJsonInfo", "offsetInfo",
            WebFieldType.JSONDICT, "The offset info that needs to be added or modified"),
    ONLYMEM(22, "onlyMemory", "onlyMem", WebFieldType.BOOLEAN,
            "Only clear the offset data in the memory cache, default is false"),
    ADMINAUTHTOKEN(23, "confModAuthToken", "authToken", WebFieldType.STRING,
            "Admin api operation authorization code",
            TServerConstants.CFG_MODAUTHTOKEN_MAX_LENGTH),
    MAXMSGSIZE(24, "maxMsgSizeInMB", "maxMsgSizeInMB", WebFieldType.INT,
            "Max allowed message size, unit MB", RegexDef.TMP_NUMBER),

    CREATEDATE(25, "createDate", "cDate", WebFieldType.STRING,
            "Record creation date", TBaseConstants.META_MAX_DATEVALUE_LENGTH),
    MODIFYDATE(26, "modifyDate", "mDate", WebFieldType.STRING,
            "Record modification date", TBaseConstants.META_MAX_DATEVALUE_LENGTH),
    HOSTNAME(27, "hostName", "hostName", WebFieldType.STRING,
            "Host name information", TBaseConstants.META_MAX_CLIENT_HOSTNAME_LENGTH),
    CLIENTID(28, "clientId", "clientId", WebFieldType.STRING,
            "Client ID", TBaseConstants.META_MAX_CLIENT_ID_LENGTH),
    @Deprecated
    CONSUMEGROUP(29, "consumeGroup", "group", WebFieldType.STRING,
            "Group name", TBaseConstants.META_MAX_GROUPNAME_LENGTH,
            RegexDef.TMP_GROUP),

    @Deprecated
    COMPSCONSUMEGROUP(30, "consumeGroup", "group", WebFieldType.COMPSTRING,
            "Group name", TBaseConstants.META_MAX_GROUPNAME_LENGTH,
                   RegexDef.TMP_GROUP),
    REGIONID(31, "regionId", "regionId", WebFieldType.INT,
            "Region id", RegexDef.TMP_NUMBER),
    COMPREGIONID(32, "regionId", "regionId", WebFieldType.COMPINT,
            "Region id", RegexDef.TMP_NUMBER),
    DATAVERSIONID(33, "dataVersionId", "dataVerId", WebFieldType.LONG,
            "Data version id", RegexDef.TMP_NUMBER),
    TOPICNAMEID(34, "topicNameId", "topicId", WebFieldType.LONG,
            "Topic name id", RegexDef.TMP_NUMBER),

    COMPTOPICNAMEID(35, "topicNameId", "topicId", WebFieldType.COMPLONG,
            "Topic name id", RegexDef.TMP_NUMBER),
    NUMTOPICSTORES(36, "numTopicStores", "numStore", WebFieldType.INT,
            "Number of topic stores", RegexDef.TMP_NUMBER),
    NUMPARTITIONS(37, "numPartitions", "numPart", WebFieldType.INT,
            "Number of partitions", RegexDef.TMP_NUMBER),
    UNFLUSHTHRESHOLD(38, "unflushThreshold", "unfDskMsgCnt", WebFieldType.INT,
            "Maximum allowed disk unflushing message count", RegexDef.TMP_NUMBER),
    UNFLUSHINTERVAL(39, "unflushInterval", "unfDskInt", WebFieldType.INT,
            "Maximum allowed disk unflushing interval", RegexDef.TMP_NUMBER),

    UNFLUSHDATAHOLD(40, "unflushDataHold", "unfDskDataSize", WebFieldType.INT,
            "Maximum allowed disk unflushing data size", RegexDef.TMP_NUMBER),
    MCACHESIZEINMB(41, "memCacheMsgSizeInMB", "cacheSizeInMB", WebFieldType.INT,
            "Maximum allowed memory cache size in MB", RegexDef.TMP_NUMBER),
    UNFMCACHECNTINK(42, "memCacheMsgCntInK", "unfMemMsgCnt", WebFieldType.INT,
            "Maximum allowed memory cache unflushing message count", RegexDef.TMP_NUMBER),
    UNFMCACHEINTERVAL(43, "memCacheFlushIntvl", "unfMemInt", WebFieldType.INT,
            "Maximum allowed disk unflushing data size", RegexDef.TMP_NUMBER),
    MAXMSGSIZEINMB(44, "maxMsgSizeInMB", "maxMsgSizeInMB", WebFieldType.INT,
            "Maximum allowed message length", RegexDef.TMP_NUMBER),

    ACCEPTPUBLISH(45, "acceptPublish", "accPub", WebFieldType.BOOLEAN,
            "Enable publishing"),
    ACCEPTSUBSCRIBE(46, "acceptSubscribe", "accSub", WebFieldType.BOOLEAN,
            "Enable subscription"),
    DELETEPOLICY(47, "deletePolicy", "delPolicy",
            WebFieldType.DELPOLICY, "File aging strategy"),
    TOPICJSONSET(48, "topicJsonSet", "topicSet",
            WebFieldType.JSONSET, "The topic info set that needs to be added or modified"),
    BROKERIP(49, "brokerIp", "brokerIp", WebFieldType.STRING,
            "Broker ip", TBaseConstants.META_MAX_BROKER_IP_LENGTH,
            RegexDef.TMP_IPV4ADDRESS),

    BROKERPORT(50, "brokerPort", "brokerPort", WebFieldType.INT,
            "Broker port", RegexDef.TMP_NUMBER),
    BROKERTLSPORT(51, "brokerTLSPort", "brokerTLSPort", WebFieldType.INT,
            "Broker tls port", RegexDef.TMP_NUMBER),
    BROKERJSONSET(52, "brokerJsonSet", "brokerSet",
            WebFieldType.JSONSET, "The broker info set that needs to be added or modified"),
    STATUSID(53, "statusId", "statusId", WebFieldType.INT,
            "Status id", RegexDef.TMP_NUMBER),
    QRYPRIORITYID(54, "qryPriorityId", "qryPriId", WebFieldType.INT,
            "Query priority id", RegexDef.TMP_NUMBER),

    FLOWCTRLSET(55, "flowCtrlInfo", "flowCtrlSet",
            WebFieldType.JSONSET,
            "The flow control info set that needs to be added or modified"),
    CONDSTATUS(56, "condStatus", "condStatus", WebFieldType.INT,
            "Group control rule status id", RegexDef.TMP_NUMBER),
    FILTERJSONSET(57, "filterCondJsonSet", "filterJsonSet",
            WebFieldType.JSONSET, "The batch filter condition configure json array");





    public final int id;
    public final String name;
    public final String shortName;
    public final WebFieldType type;
    public final String desc;
    public final boolean compVal;
    public final String splitToken;
    public final int itemMaxCnt;
    public final int valMaxLen;
    public final boolean regexCheck;
    public final RegexDef regexDef;


    WebFieldDef(int id, String name, String shortName, WebFieldType type, String desc) {
        this(id, name, shortName, type, desc, TBaseConstants.META_VALUE_UNDEFINED,
                TBaseConstants.META_VALUE_UNDEFINED, false, null);
    }

    WebFieldDef(int id, String name, String shortName, WebFieldType type,
                String desc, int valMaxLen) {
        this(id, name, shortName, type, desc, valMaxLen,
                TBaseConstants.META_VALUE_UNDEFINED, false, null);
    }

    WebFieldDef(int id, String name, String shortName, WebFieldType type,
                String desc, RegexDef regexDef) {
        this(id, name, shortName, type, desc,
                TBaseConstants.META_VALUE_UNDEFINED, regexDef);
    }

    WebFieldDef(int id, String name, String shortName, WebFieldType type,
                String desc, int valMaxLen, RegexDef regexDef) {
        this(id, name, shortName, type, desc, valMaxLen,
                TServerConstants.CFG_BATCH_RECORD_OPERATE_MAX_COUNT,
                true, regexDef);
    }

    WebFieldDef(int id, String name, String shortName, WebFieldType type,
                String desc, int valMaxLen, int itemMaxCnt, RegexDef regexDef) {
        this(id, name, shortName, type, desc, valMaxLen,
                itemMaxCnt, true, regexDef);
    }

    WebFieldDef(int id, String name, String shortName, WebFieldType type,
                String desc, int valMaxLen, int itemMaxCnt,
                boolean regexChk, RegexDef regexDef) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.type = type;
        this.desc = desc;
        if (isCompFieldType()) {
            this.compVal = true;
            this.splitToken = TokenConstants.ARRAY_SEP;
            this.itemMaxCnt = itemMaxCnt;
        } else {
            this.compVal = false;
            this.splitToken = "";
            this.itemMaxCnt = TBaseConstants.META_VALUE_UNDEFINED;
        }
        this.valMaxLen = valMaxLen;
        this.regexCheck = regexChk;
        this.regexDef = regexDef;
    }

    public boolean isCompFieldType() {
        return (this.type == WebFieldType.COMPINT
                || this.type == WebFieldType.COMPLONG
                || this.type == WebFieldType.COMPSTRING);
    }

    public static final int MAX_FIELD_ID;
    private static final WebFieldDef[] WEB_FIELD_DEFS;
    private static final int MIN_FIELD_ID = 0;

    static {
        int maxId = -1;
        for (WebFieldDef fieldDef : WebFieldDef.values()) {
            maxId = Math.max(maxId, fieldDef.id);
        }
        WebFieldDef[] idToType = new WebFieldDef[maxId + 1];
        for (WebFieldDef fieldDef : WebFieldDef.values()) {
            idToType[fieldDef.id] = fieldDef;
        }
        WEB_FIELD_DEFS = idToType;
        MAX_FIELD_ID = maxId;
    }

    public static WebFieldDef valueOf(int fieldId) {
        if (fieldId >= MIN_FIELD_ID && fieldId <= MAX_FIELD_ID) {
            return WEB_FIELD_DEFS[fieldId];
        }
        throw new IllegalArgumentException(
                String.format("Unexpected WebFieldDef id `%s`, it should be between `%s` "
                        + "and `%s` (inclusive)", fieldId, MIN_FIELD_ID, MAX_FIELD_ID));
    }
}
