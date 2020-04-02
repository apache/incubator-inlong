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

package org.apache.tubemq.server.common.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.tubemq.corebase.TBaseConstants;
import org.apache.tubemq.corebase.TokenConstants;
import org.apache.tubemq.corebase.utils.TStringUtils;
import org.apache.tubemq.server.broker.utils.DataStoreUtils;
import org.apache.tubemq.server.common.TServerConstants;
import org.apache.tubemq.server.common.TStatusConstants;
import org.apache.tubemq.server.master.TMaster;
import org.apache.tubemq.server.master.bdbstore.bdbentitys.BdbBrokerConfEntity;
import org.apache.tubemq.server.master.nodemanage.nodebroker.BrokerConfManage;
import org.apache.tubemq.server.master.nodemanage.nodebroker.BrokerSyncStatusInfo;


public class WebParameterUtils {

    private static final List<String> allowedDelUnits = Arrays.asList("s", "m", "h");

    /**
     * Parse the parameter value from an object value to a long value
     *
     * @param paramName    the parameter name
     * @param paramValue   the parameter value which is an object for parsing
     * @param required     a boolean value represent whether the parameter is must required
     * @param defaultValue a default value returned if failed to parse value from the given object
     * @return a long value of parameter
     * @throws Exception if failed to parse the object
     */
    public static long validLongDataParameter(String paramName, String paramValue,
                                              boolean required, long defaultValue) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue)) {
            return defaultValue;
        }
        if (!tmpParamValue.matches(TBaseConstants.META_TMP_NUMBER_VALUE)) {
            throw new Exception(new StringBuilder(512).append("the value of ")
                    .append(paramName).append(" parameter must only contain numbers").toString());
        }
        return Long.parseLong(tmpParamValue);
    }

    /**
     * Parse the parameter value from an object value to a integer value
     *
     * @param paramName    the parameter name
     * @param paramValue   the parameter value which is an object for parsing
     * @param required     a boolean value represent whether the parameter is must required
     * @param defaultValue a default value returned if failed to parse value from the given object
     * @return a int value of parameter
     * @throws Exception if failed to parse the object
     */
    public static int validIntDataParameter(String paramName, String paramValue,
                                            boolean required, int defaultValue,
                                            int minValue) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue)) {
            return defaultValue;
        }
        if (!tmpParamValue.matches(TBaseConstants.META_TMP_NUMBER_VALUE)) {
            throw new Exception(new StringBuilder(512)
                    .append("the value of ").append(paramName)
                    .append(" parameter must only contain numbers").toString());
        }
        int tmpInteger = Integer.parseInt(tmpParamValue);
        if (tmpInteger < minValue) {
            throw new Exception(new StringBuilder(512)
                    .append("the value of ").append(paramName)
                    .append(" parameter must >= ").append(minValue).toString());
        }
        return tmpInteger;
    }

    /**
     * Parse the parameter value from an object value to a boolean value
     *
     * @param paramName    the parameter name
     * @param paramValue   the parameter value which is an object for parsing
     * @param required     a boolean value represent whether the parameter is must required
     * @param defaultValue a default value returned if failed to parse value from the given object
     * @return a boolean value of parameter
     * @throws Exception if failed to parse the object
     */
    public static boolean validBooleanDataParameter(String paramName, String paramValue,
                                                    boolean required, boolean defaultValue) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(tmpParamValue);
    }

    /**
     * Parse the parameter value from an object value to Date value
     *
     * @param paramName    the parameter name
     * @param paramValue   the parameter value which is an object for parsing
     * @param required     a boolean value represent whether the parameter is must required
     * @param defaultValue a default value returned if failed to parse value from the given object
     * @return a Date value of parameter
     * @throws Exception if failed to parse the object
     */
    public static Date validDateParameter(String paramName, String paramValue, int paramMaxLen,
                                          boolean required, Date defaultValue) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue)) {
            return defaultValue;
        }
        // yyyyMMddHHmmss
        if (tmpParamValue.length() > paramMaxLen) {
            throw new Exception(new StringBuilder(512)
                    .append("the date format is yyyyMMddHHmmss of ")
                    .append(paramName).append(" parameter").toString());
        }
        if (!tmpParamValue.matches(TBaseConstants.META_TMP_NUMBER_VALUE)) {
            throw new Exception(new StringBuilder(512).append("the value of ")
                    .append(paramName).append(" parameter must only contain numbers").toString());
        }
        DateFormat sdf = new SimpleDateFormat(TBaseConstants.META_TMP_DATE_VALUE);
        Date date = sdf.parse(tmpParamValue);
        return date;
    }

    /**
     * Parse the parameter value from an object value to string value
     *
     * @param paramName    the parameter name
     * @param paramValue   the parameter value which is an object for parsing
     * @param paramMaxLen  the max length of string to return
     * @param required     a boolean value represent whether the parameter is must required
     * @param defaultValue a default value returned if failed to parse value from the given object
     * @return a string value of parameter
     * @throws Exception if failed to parse the object
     */
    public static String validStringParameter(String paramName, String paramValue, int paramMaxLen,
                                              boolean required, String defaultValue) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue)) {
            return defaultValue;
        }
        if (paramMaxLen != TBaseConstants.META_VALUE_UNDEFINED) {
            if (tmpParamValue.length() > paramMaxLen) {
                throw new Exception(new StringBuilder(512).append("the max length of ")
                        .append(paramName).append(" parameter is ")
                        .append(paramMaxLen).append(" characters").toString());
            }
        }
        if (!tmpParamValue.matches(TBaseConstants.META_TMP_STRING_VALUE)) {
            throw new Exception(new StringBuilder(512).append("the value of ")
                    .append(paramName).append(" parameter must begin with a letter, ")
                    .append("can only contain characters,numbers,and underscores").toString());
        }
        return tmpParamValue;
    }

    /**
     * Parse the parameter value from an object value to group string value
     *
     * @param paramName    the parameter name
     * @param paramValue   the parameter value which is an object for parsing
     * @param paramMaxLen  the max length of string to return
     * @param required     a boolean value represent whether the parameter is must required
     * @param defaultValue a default value returned if failed to parse value from the given object
     * @return a string value of parameter
     * @throws Exception if failed to parse the object
     */
    public static String validGroupParameter(String paramName, String paramValue, int paramMaxLen,
                                             boolean required, String defaultValue) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue)) {
            return defaultValue;
        }
        if (paramMaxLen != TBaseConstants.META_VALUE_UNDEFINED) {
            if (tmpParamValue.length() > paramMaxLen) {
                throw new Exception(new StringBuilder(512).append("the max length of ")
                    .append(paramName).append(" parameter is ")
                    .append(paramMaxLen).append(" characters").toString());
            }
        }
        if (!tmpParamValue.matches(TBaseConstants.META_TMP_GROUP_VALUE)) {
            throw new Exception(new StringBuilder(512).append("the value of ")
                .append(paramName).append(" parameter must begin with a letter, ")
                .append("can only contain characters,numbers,hyphen,and underscores").toString());
        }
        return tmpParamValue;
    }

    /**
     * Parse the parameter value from an object value to ip address of string value
     *
     * @param paramName    the parameter name
     * @param paramValue   the parameter value which is an object for parsing
     * @param paramMaxLen  the max length of string to return
     * @param required     a boolean value represent whether the parameter is must required
     * @param defaultValue a default value returned if failed to parse value from the given object
     * @return a ip string of parameter
     * @throws Exception if failed to parse the object
     */
    public static String validAddressParameter(String paramName, String paramValue, int paramMaxLen,
                                               boolean required, String defaultValue) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue)) {
            return defaultValue;
        }
        if (tmpParamValue.length() > paramMaxLen) {
            throw new Exception(new StringBuilder(512).append("the max length of ")
                    .append(paramName).append(" parameter is ").append(paramMaxLen)
                    .append(" characters").toString());
        }
        if (!tmpParamValue.matches(TBaseConstants.META_TMP_IP_ADDRESS_VALUE)) {
            throw new Exception(new StringBuilder(512)
                    .append("the value of ").append(paramName)
                    .append(" parameter not matches the regulation :")
                    .append(TBaseConstants.META_TMP_IP_ADDRESS_VALUE).toString());
        }
        return tmpParamValue;
    }

    /**
     * Decode the parameter value from an object value
     *
     * @param paramName    the parameter name
     * @param paramValue   the parameter value which is an object for parsing
     * @param paramMaxLen  the max length of string to return
     * @param required     a boolean value represent whether the parameter is must required
     * @param defaultValue a default value returned if failed to parse value from the given object
     * @return the decoded string of parameter
     * @throws Exception if failed to parse the object
     */
    public static String validDecodeStringParameter(String paramName, String paramValue, int paramMaxLen,
                                                    boolean required, String defaultValue) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue)) {
            return defaultValue;
        }
        String output = null;
        try {
            output = URLDecoder.decode(tmpParamValue, TBaseConstants.META_DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new Exception(new StringBuilder(512).append("Decode ")
                    .append(paramName).append("error, exception is ")
                    .append(e.toString()).toString());
        }
        if (paramMaxLen != TBaseConstants.META_VALUE_UNDEFINED) {
            if (output.length() > paramMaxLen) {
                throw new Exception(new StringBuilder(512)
                        .append("the max length of ").append(paramName)
                        .append(" parameter is ").append(paramMaxLen)
                        .append(" characters").toString());
            }
        }
        return output;
    }

    /**
     * Proceed authorization
     *
     * @param master           the service master
     * @param brokerConfManage the broker configuration manager
     * @param reqToken         the token for checking
     * @throws Exception if authorization failed
     */
    public static void reqAuthorizenCheck(TMaster master,
                                          BrokerConfManage brokerConfManage,
                                          String reqToken) throws Exception {
        if (brokerConfManage.isPrimaryNodeActive()) {
            throw new Exception(
                    "Illegal visit: designatedPrimary happened...please check if the other member is down");
        }
        String inPutConfModAuthToken =
                validStringParameter("confModAuthToken", reqToken,
                        TServerConstants.CFG_MODAUTHTOKEN_MAX_LENGTH, true, "");
        if (!inPutConfModAuthToken.equals(master.getMasterConfig().getConfModAuthToken())) {
            throw new Exception("Illegal visit: not authorized to process authorization info!");
        }
    }

    /**
     * Decode the deletePolicy parameter value from an object value
     * the value must like {method},{digital}[s|m|h]
     *
     * @param paramName    the parameter name
     * @param paramValue   the parameter value which is an object for parsing
     * @param required     a boolean value represent whether the parameter is must required
     * @param defaultValue a default value returned if failed to parse value from the given object
     * @return the decoded string of parameter
     * @throws Exception if failed to parse the object
     */
    public static String validDeletePolicyParameter(String paramName, String paramValue,
                                                    boolean required, String defaultValue) throws Exception {
        int paramMaxLen = TServerConstants.CFG_DELETEPOLICY_MAX_LENGTH;
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue)) {
            return defaultValue;
        }
        String inDelPolicy = null;
        try {
            inDelPolicy = URLDecoder.decode(tmpParamValue, TBaseConstants.META_DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new Exception(new StringBuilder(512).append("Decode ")
                    .append(paramName).append("error, exception is ")
                    .append(e.toString()).toString());
        }
        if (inDelPolicy.length() > paramMaxLen) {
            throw new Exception(new StringBuilder(512)
                    .append("the max length of ").append(paramName)
                    .append(" parameter is ").append(paramMaxLen)
                    .append(" characters").toString());
        }
        String[] tmpStrs = inDelPolicy.split(",");
        if (tmpStrs.length != 2) {
            throw new Exception(new StringBuilder(512)
                    .append("Illegal value: must include one and only one comma character,")
                    .append(" the format of ").append(paramName)
                    .append(" must like {method},{digital}[m|s|h]").toString());
        }
        if (TStringUtils.isBlank(tmpStrs[0])) {
            throw new Exception(new StringBuilder(512)
                    .append("Illegal value: method's value must not be blank!")
                    .append(" the format of ").append(paramName)
                    .append(" must like {method},{digital}[s|m|h]").toString());
        }
        if (!"delete".equalsIgnoreCase(tmpStrs[0].trim())) {
            throw new Exception(new StringBuilder(512)
                    .append("Illegal value: only support delete method now!").toString());
        }
        String validValStr = tmpStrs[1];
        String timeUnit = validValStr.substring(validValStr.length() - 1).toLowerCase();
        if (Character.isLetter(timeUnit.charAt(0))) {
            if (!allowedDelUnits.contains(timeUnit)) {
                throw new Exception(new StringBuilder(512)
                        .append("Illegal value: only support [s|m|h] unit!").toString());
            }
        }
        long validDuration = 0;
        try {
            if (timeUnit.endsWith("s")) {
                validDuration = Long.valueOf(validValStr.substring(0, validValStr.length() - 1)) * 1000;
            } else if (timeUnit.endsWith("m")) {
                validDuration = Long.valueOf(validValStr.substring(0, validValStr.length() - 1)) * 60000;
            } else if (timeUnit.endsWith("h")) {
                validDuration = Long.valueOf(validValStr.substring(0, validValStr.length() - 1)) * 3600000;
            } else {
                validDuration = Long.valueOf(validValStr) * 3600000;
            }
        } catch (Throwable e) {
            throw new Exception(new StringBuilder(512)
                    .append("Illegal value: the value of valid duration must digits!").toString());
        }
        if (validDuration <= 0 || validDuration > DataStoreUtils.MAX_FILE_VALID_DURATION) {
            throw new Exception(new StringBuilder(512)
                    .append("Illegal value: the value of valid duration must")
                    .append(" be greater than 0 and  less than or equal to ")
                    .append(DataStoreUtils.MAX_FILE_VALID_DURATION).append(" seconds!").toString());
        }
        if (Character.isLetter(timeUnit.charAt(0))) {
            return new StringBuilder(512).append("delete,")
                    .append(validValStr.substring(0, validValStr.length() - 1))
                    .append(timeUnit).toString();
        } else {
            return new StringBuilder(512).append("delete,")
                    .append(validValStr).append("h").toString();
        }
    }

    /**
     * check the filter conditions and get them
     *
     * @param inFilterConds the filter conditions to be decoded
     * @param isTransBlank  denote whether it translate blank condition
     * @param sb            the string buffer used to construct result
     * @return the decoded filter conditions
     * @throws Exception if failed to decode the filter conditions
     */
    public static String checkAndGetFilterConds(String inFilterConds,
                                                boolean isTransBlank,
                                                StringBuilder sb) throws Exception {
        if (TStringUtils.isNotBlank(inFilterConds)) {
            inFilterConds = escDoubleQuotes(inFilterConds.trim());
        }
        if (TStringUtils.isBlank(inFilterConds)) {
            if (isTransBlank) {
                sb.append(TServerConstants.TOKEN_BLANK_FILTER_CONDITION);
            }
        } else {
            sb.append(TokenConstants.ARRAY_SEP);
            TreeSet<String> filterConds = new TreeSet<String>();
            String[] strFilterConds = inFilterConds.split(TokenConstants.ARRAY_SEP);
            for (int i = 0; i < strFilterConds.length; i++) {
                if (TStringUtils.isBlank(strFilterConds[i])) {
                    continue;
                }
                String filterCond = strFilterConds[i].trim();
                if (filterCond.length() > TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_LENGTH) {
                    sb.delete(0, sb.length());
                    throw new Exception(sb.append("Illegal value: the max length of ")
                            .append(filterCond).append(" in filterConds parameter over ")
                            .append(TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_LENGTH)
                            .append(" characters").toString());
                }
                if (!filterCond.matches(TBaseConstants.META_TMP_FILTER_VALUE)) {
                    sb.delete(0, sb.length());
                    throw new Exception(sb.append("Illegal value: the value of ")
                        .append(filterCond).append(" in filterCond parameter ")
                        .append("must only contain characters,numbers,and underscores").toString());
                }
                filterConds.add(filterCond);
            }
            int count = 0;
            for (String itemStr : filterConds) {
                if (count++ > 0) {
                    sb.append(TokenConstants.ARRAY_SEP);
                }
                sb.append(itemStr);
            }
            sb.append(TokenConstants.ARRAY_SEP);
        }
        String strNewFilterConds = sb.toString();
        sb.delete(0, sb.length());
        return strNewFilterConds;
    }

    /**
     * check the filter conditions and get them in a set
     *
     * @param inFilterConds the filter conditions to be decoded
     * @param transCondItem whether to translate condition item
     * @param checkTotalCnt whether to check condition item exceed max count
     * @param sb            the string buffer used to construct result
     * @return the decoded filter conditions
     * @throws Exception if failed to decode the filter conditions
     */
    public static Set<String> checkAndGetFilterCondSet(String inFilterConds,
                                                       boolean transCondItem,
                                                       boolean checkTotalCnt,
                                                       StringBuilder sb) throws Exception {
        Set<String> filterCondSet = new HashSet<String>();
        if (TStringUtils.isBlank(inFilterConds)) {
            return filterCondSet;
        }
        inFilterConds = escDoubleQuotes(inFilterConds.trim());
        if (TStringUtils.isNotBlank(inFilterConds)) {
            String[] strFilterConds = inFilterConds.split(TokenConstants.ARRAY_SEP);
            for (int i = 0; i < strFilterConds.length; i++) {
                if (TStringUtils.isBlank(strFilterConds[i])) {
                    continue;
                }
                String filterCond = strFilterConds[i].trim();
                if (filterCond.length() > TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT) {
                    sb.delete(0, sb.length());
                    throw new Exception(sb.append("Illegal value: the max length of ")
                            .append(filterCond).append(" in filterConds parameter over ")
                            .append(TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT)
                            .append(" characters").toString());
                }
                if (!filterCond.matches(TBaseConstants.META_TMP_FILTER_VALUE)) {
                    sb.delete(0, sb.length());
                    throw new Exception(sb.append("Illegal value: the value of ")
                        .append(filterCond).append(" in filterCond parameter must ")
                        .append("only contain characters,numbers,and underscores").toString());
                }
                if (transCondItem) {
                    filterCondSet.add(sb.append(TokenConstants.ARRAY_SEP)
                            .append(filterCond).append(TokenConstants.ARRAY_SEP).toString());
                    sb.delete(0, sb.length());
                } else {
                    filterCondSet.add(filterCond);
                }
            }
            if (checkTotalCnt) {
                if (filterCondSet.size() > TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT) {
                    throw new Exception(sb.append("Illegal value: the count of filterCond's ")
                        .append("value over max allowed count(")
                        .append(TBaseConstants.CFG_FLT_MAX_FILTER_ITEM_COUNT).append(")!").toString());
                }
            }
        }
        return filterCondSet;
    }

    public static Set<String> getBatchGroupNames(String inputGroupName,
                                                 boolean checkEmpty,
                                                 boolean checkResToken,
                                                 Set<String> resTokens,
                                                 final StringBuilder sb) throws Exception {
        Set<String> bathOpGroupNames = new HashSet<String>();
        if (TStringUtils.isNotBlank(inputGroupName)) {
            inputGroupName = escDoubleQuotes(inputGroupName.trim());
        }
        if (TStringUtils.isBlank(inputGroupName)) {
            if (checkEmpty) {
                throw new Exception("Illegal value: required groupName parameter");
            }
            return bathOpGroupNames;
        }
        String[] strGroupNames = inputGroupName.split(TokenConstants.ARRAY_SEP);
        if (strGroupNames.length > TServerConstants.CFG_BATCH_RECORD_OPERATE_MAX_COUNT) {
            throw new Exception(sb.append("Illegal value: groupName's bath count over max count ")
                .append(TServerConstants.CFG_BATCH_RECORD_OPERATE_MAX_COUNT).toString());
        }
        for (int i = 0; i < strGroupNames.length; i++) {
            if (TStringUtils.isBlank(strGroupNames[i])) {
                continue;
            }
            String groupName = strGroupNames[i].trim();
            if (checkResToken) {
                if (resTokens != null && !resTokens.isEmpty()) {
                    if (resTokens.contains(groupName)) {
                        throw new Exception(sb.append("Illegal value: in groupName parameter, '")
                            .append(groupName).append("' is a system reserved token!").toString());
                    }
                }
            }
            if (groupName.length() > TBaseConstants.META_MAX_GROUPNAME_LENGTH) {
                throw new Exception(sb.append("Illegal value: the max length of ")
                        .append(groupName).append(" in groupName parameter over ")
                        .append(TBaseConstants.META_MAX_GROUPNAME_LENGTH)
                        .append(" characters").toString());
            }
            if (!groupName.matches(TBaseConstants.META_TMP_GROUP_VALUE)) {
                throw new Exception(sb.append("Illegal value: the value of ").append(groupName)
                    .append("in groupName parameter must begin with a letter, can only contain ")
                    .append("characters,numbers,hyphen,and underscores").toString());
            }
            bathOpGroupNames.add(groupName);
        }
        if (bathOpGroupNames.isEmpty()) {
            if (checkEmpty) {
                throw new Exception("Illegal value: Null value of groupName parameter");
            }
        }
        return bathOpGroupNames;
    }

    public static Set<String> getBatchTopicNames(String inputTopicName,
                                                 boolean checkEmpty,
                                                 boolean checkRange,
                                                 Set<String> checkedTopicList,
                                                 final StringBuilder sb) throws Exception {
        Set<String> bathOpTopicNames = new HashSet<String>();
        if (TStringUtils.isNotBlank(inputTopicName)) {
            inputTopicName = escDoubleQuotes(inputTopicName.trim());
        }
        if (TStringUtils.isBlank(inputTopicName)) {
            if (checkEmpty) {
                throw new Exception("Illegal value: required topicName parameter");
            }
            return bathOpTopicNames;
        }
        String[] strTopicNames = inputTopicName.split(TokenConstants.ARRAY_SEP);
        if (strTopicNames.length > TServerConstants.CFG_BATCH_RECORD_OPERATE_MAX_COUNT) {
            throw new Exception(sb.append("Illegal value: topicName's bath count over max count ")
                    .append(TServerConstants.CFG_BATCH_RECORD_OPERATE_MAX_COUNT).toString());
        }
        for (int i = 0; i < strTopicNames.length; i++) {
            if (TStringUtils.isBlank(strTopicNames[i])) {
                continue;
            }
            String topicName = strTopicNames[i].trim();
            if (topicName.length() > TBaseConstants.META_MAX_TOPICNAME_LENGTH) {
                throw new Exception(sb.append("Illegal value: the max length of ")
                        .append(topicName).append(" in topicName parameter over ")
                        .append(TBaseConstants.META_MAX_TOPICNAME_LENGTH)
                        .append(" characters").toString());
            }
            if (!topicName.matches(TBaseConstants.META_TMP_STRING_VALUE)) {
                throw new Exception(sb.append("Illegal value: the value of ")
                    .append(topicName).append(" in topicName parameter must begin with a letter,")
                    .append(" can only contain characters,numbers,and underscores").toString());
            }
            if (checkRange) {
                if (!checkedTopicList.contains(topicName)) {
                    throw new Exception(sb.append("Illegal value: topic(").append(topicName)
                            .append(") not configure in master's topic configure, please configure first!").toString());
                }
            }
            bathOpTopicNames.add(topicName);
        }
        if (bathOpTopicNames.isEmpty()) {
            if (checkEmpty) {
                throw new Exception("Illegal value: Null value of topicName parameter");
            }
        }
        return bathOpTopicNames;
    }

    public static Set<String> getBatchBrokerIpSet(String inStrBrokerIps,
                                                  boolean checkEmpty) throws Exception {
        Set<String> bathBrokerIps = new HashSet<String>();
        if (TStringUtils.isNotBlank(inStrBrokerIps)) {
            inStrBrokerIps = escDoubleQuotes(inStrBrokerIps.trim());
        }
        if (TStringUtils.isBlank(inStrBrokerIps)) {
            if (checkEmpty) {
                throw new Exception("Illegal value: required brokerIp parameter");
            }
            return bathBrokerIps;
        }
        String[] strBrokerIps = inStrBrokerIps.split(TokenConstants.ARRAY_SEP);
        for (int i = 0; i < strBrokerIps.length; i++) {
            if (TStringUtils.isEmpty(strBrokerIps[i])) {
                continue;
            }
            String brokerIp =
                    validAddressParameter("brokerIp", strBrokerIps[i],
                            TBaseConstants.META_MAX_BROKER_IP_LENGTH, true, "");
            if (bathBrokerIps.contains(brokerIp)) {
                continue;
            }
            bathBrokerIps.add(brokerIp);
        }
        if (bathBrokerIps.isEmpty()) {
            if (checkEmpty) {
                throw new Exception("Illegal value: Null value of brokerIp parameter");
            }
        }
        return bathBrokerIps;
    }

    public static Set<Integer> getBatchBrokerIdSet(String inStrBrokerIds,
                                                   boolean checkEmpty) throws Exception {
        Set<Integer> bathBrokerIdSet = new HashSet<Integer>();
        if (TStringUtils.isNotBlank(inStrBrokerIds)) {
            inStrBrokerIds = escDoubleQuotes(inStrBrokerIds.trim());
        }
        if (TStringUtils.isBlank(inStrBrokerIds)) {
            if (checkEmpty) {
                throw new Exception("Illegal value: required brokerId parameter");
            }
            return bathBrokerIdSet;
        }
        String[] strBrokerIds = inStrBrokerIds.split(TokenConstants.ARRAY_SEP);
        if (strBrokerIds.length > TServerConstants.CFG_BATCH_BROKER_OPERATE_MAX_COUNT) {
            throw new Exception(new StringBuilder(512)
                    .append("Illegal value: bath numbers of brokerId's value over max count ")
                    .append(TServerConstants.CFG_BATCH_BROKER_OPERATE_MAX_COUNT).toString());
        }
        for (int i = 0; i < strBrokerIds.length; i++) {
            if (TStringUtils.isEmpty(strBrokerIds[i])) {
                continue;
            }
            int brokerId =
                    validIntDataParameter("brokerId", strBrokerIds[i], true, 0, 1);
            bathBrokerIdSet.add(brokerId);
        }
        if (bathBrokerIdSet.isEmpty()) {
            if (checkEmpty) {
                throw new Exception("Illegal value: Null value of brokerId parameter");
            }
        }
        return bathBrokerIdSet;
    }

    public static Set<BdbBrokerConfEntity> getBatchBrokerIdSet(String inStrBrokerIds,
                                                               BrokerConfManage webMaster,
                                                               boolean checkEmpty,
                                                               final StringBuilder sb) throws Exception {
        Set<BdbBrokerConfEntity> bathBrokerIdSet = new HashSet<BdbBrokerConfEntity>();
        if (TStringUtils.isNotBlank(inStrBrokerIds)) {
            inStrBrokerIds = escDoubleQuotes(inStrBrokerIds.trim());
        }
        if (TStringUtils.isBlank(inStrBrokerIds)) {
            if (checkEmpty) {
                throw new Exception("Illegal value: required brokerId parameter");
            }
            return bathBrokerIdSet;
        }
        String[] strBrokerIds = inStrBrokerIds.split(TokenConstants.ARRAY_SEP);
        if (strBrokerIds.length > TServerConstants.CFG_BATCH_BROKER_OPERATE_MAX_COUNT) {
            throw new Exception(sb
                    .append("Illegal value: bath numbers of brokerId's value over max count ")
                    .append(TServerConstants.CFG_BATCH_BROKER_OPERATE_MAX_COUNT).toString());
        }
        for (int i = 0; i < strBrokerIds.length; i++) {
            if (TStringUtils.isEmpty(strBrokerIds[i])) {
                continue;
            }
            int brokerId =
                    validIntDataParameter("brokerId", strBrokerIds[i], true, 0, 1);
            BdbBrokerConfEntity brokerConfEntity =
                    webMaster.getBrokerDefaultConfigStoreInfo(brokerId);
            if (brokerConfEntity == null) {
                throw new Exception(sb
                        .append("Illegal value: not found broker default configure record by brokerId=")
                        .append(brokerId).toString());
            }
            bathBrokerIdSet.add(brokerConfEntity);
        }
        if (bathBrokerIdSet.isEmpty()) {
            if (checkEmpty) {
                throw new Exception("Illegal value: Null value of brokerId parameter");
            }
        }
        return bathBrokerIdSet;
    }

    /**
     * check and get parameter value with json array
     *
     * @param paramName   the parameter name
     * @param paramValue  the object value of the parameter
     * @param paramMaxLen the maximum length of json array
     * @param required    denote whether the parameter is must required
     * @return a list of linked hash map represent the json array
     * @throws Exception
     */
    public static List<Map<String, String>> checkAndGetJsonArray(String paramName,
                                                                 String paramValue,
                                                                 int paramMaxLen,
                                                                 boolean required) throws Exception {
        String tmpParamValue = checkParamCommonRequires(paramName, paramValue, required);
        if (TStringUtils.isBlank(tmpParamValue) && !required) {
            return null;
        }
        String decTmpParamVal = null;
        try {
            decTmpParamVal = URLDecoder.decode(tmpParamValue,
                    TBaseConstants.META_DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new Exception(new StringBuilder(512).append("Decode ")
                    .append(paramName).append("error, exception is ")
                    .append(e.toString()).toString());
        }
        if (TStringUtils.isBlank(decTmpParamVal)) {
            if (required) {
                throw new Exception(new StringBuilder(512)
                        .append("Blank value of ").append(paramName)
                        .append(" parameter").toString());
            } else {
                return null;
            }
        }
        if (paramMaxLen != TBaseConstants.META_VALUE_UNDEFINED) {
            if (decTmpParamVal.length() > paramMaxLen) {
                throw new Exception(new StringBuilder(512)
                        .append("the max length of ").append(paramName)
                        .append(" parameter is ").append(paramMaxLen)
                        .append(" characters").toString());
            }
        }
        return new Gson().fromJson(decTmpParamVal, new TypeToken<List<Map<String, String>>>(){}.getType());
    }

    /**
     * Check the broker online status
     *
     * @param curEntity the entity of bdb broker configuration
     * @return the true if broker is online, false in other cases
     */
    public static boolean checkBrokerInOnlineStatus(BdbBrokerConfEntity curEntity) {
        if (curEntity != null) {
            return (curEntity.getManageStatus() == TStatusConstants.STATUS_MANAGE_ONLINE
                    || curEntity.getManageStatus() == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_WRITE
                    || curEntity.getManageStatus() == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_READ);
        }
        return false;
    }

    /**
     * check whether the broker is working in progress
     *
     * @param brokerId  the id of the broker
     * @param webMaster the broker configuration manager
     * @param sBuilder  the string builder used to construct the detail err
     * @return true if the broker is working in progress, false in other cases
     * @throws Exception
     */
    public static boolean checkBrokerInProcessing(int brokerId,
                                                  BrokerConfManage webMaster,
                                                  StringBuilder sBuilder) throws Exception {
        BrokerSyncStatusInfo brokerSyncStatusInfo =
                webMaster.getBrokerRunSyncStatusInfo(brokerId);
        if ((brokerSyncStatusInfo != null) && (brokerSyncStatusInfo.isBrokerRegister())) {
            int status = brokerSyncStatusInfo.getBrokerRunStatus();
            if (!((status == TStatusConstants.STATUS_SERVICE_UNDEFINED)
                    || (status == TStatusConstants.STATUS_SERVICE_TOONLINE_WAIT_REGISTER)
                    || (status == TStatusConstants.STATUS_SERVICE_TOONLINE_PART_WAIT_REGISTER))) {
                if (sBuilder != null) {
                    sBuilder.append("Illegal value: the broker of brokerId=")
                            .append(brokerId).append(" is processing event(")
                            .append(brokerSyncStatusInfo.getBrokerRunStatus())
                            .append("), please try later! ");
                }
                return true;
            }
        }
        return false;
    }

    public static boolean checkBrokerUnLoad(int brokerId,
                                            BrokerConfManage webMaster,
                                            StringBuilder sBuilder) throws Exception {
        BrokerSyncStatusInfo brokerSyncStatusInfo =
                webMaster.getBrokerRunSyncStatusInfo(brokerId);
        if ((brokerSyncStatusInfo != null) && (brokerSyncStatusInfo.isBrokerRegister())) {
            int status = brokerSyncStatusInfo.getBrokerManageStatus();
            if ((status == TStatusConstants.STATUS_MANAGE_ONLINE)
                    || (status == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_WRITE)
                    || (status == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_READ)) {
                if (!brokerSyncStatusInfo.isBrokerLoaded()) {
                    if (sBuilder != null) {
                        sBuilder.append("The broker's configure of brokerId=").append(brokerId)
                                .append(" changed but not reload in online status, please reload configure first!");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkBrokerInOfflining(int brokerId,
                                                 int manageStatus,
                                                 BrokerConfManage webMaster,
                                                 StringBuilder sBuilder) throws Exception {
        BrokerSyncStatusInfo brokerSyncStatusInfo =
                webMaster.getBrokerRunSyncStatusInfo(brokerId);
        if ((brokerSyncStatusInfo != null)
                && (brokerSyncStatusInfo.isBrokerRegister())) {
            if ((manageStatus == TStatusConstants.STATUS_MANAGE_OFFLINE)
                    && (brokerSyncStatusInfo.getBrokerRunStatus() != TStatusConstants.STATUS_SERVICE_UNDEFINED)) {
                if (sBuilder != null) {
                    sBuilder.append("Illegal value: the broker is processing offline event by brokerId=")
                            .append(brokerId).append(", please wait and try later!");
                }
                return true;
            }
        }
        return false;
    }

    public static String getBrokerManageStatusStr(int manageStatus) {
        String strManageStatus = "unsupported_status";
        if (manageStatus == TStatusConstants.STATUS_MANAGE_APPLY) {
            strManageStatus = "draft";
        } else if (manageStatus == TStatusConstants.STATUS_MANAGE_ONLINE) {
            strManageStatus = "online";
        } else if (manageStatus == TStatusConstants.STATUS_MANAGE_OFFLINE) {
            strManageStatus = "offline";
        } else if (manageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_WRITE) {
            strManageStatus = "only-read";
        } else if (manageStatus == TStatusConstants.STATUS_MANAGE_ONLINE_NOT_READ) {
            strManageStatus = "only-write";
        }
        return strManageStatus;
    }

    public static String date2yyyyMMddHHmmss(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(date);
    }

    private static String checkParamCommonRequires(final String paramName, final String paramValue,
                                                   boolean required) throws Exception {
        String temParamValue = null;
        if (paramValue == null) {
            if (required) {
                throw new Exception(new StringBuilder(512).append("Required ")
                        .append(paramName).append(" parameter").toString());
            }
        } else {
            temParamValue = escDoubleQuotes(paramValue.trim());
            if (TStringUtils.isBlank(temParamValue)) {
                if (required) {
                    throw new Exception(new StringBuilder(512)
                            .append("Null or blank value of ").append(paramName)
                            .append(" parameter").toString());
                }
            }
        }
        return temParamValue;
    }

    private static String escDoubleQuotes(String inPutStr) {
        if (TStringUtils.isBlank(inPutStr) || inPutStr.length() < 2) {
            return inPutStr;
        }
        if (inPutStr.charAt(0) == '\"'
                && inPutStr.charAt(inPutStr.length() - 1) == '\"') {
            if (inPutStr.length() == 2) {
                return "";
            } else {
                return inPutStr.substring(1, inPutStr.length() - 1).trim();
            }
        }
        return inPutStr;
    }
}
