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

package org.apache.tubemq.server.tools;

import static org.apache.tubemq.server.common.fielddef.CliArgDef.FILEPATH;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.tubemq.corebase.utils.TStringUtils;
import org.apache.tubemq.server.common.utils.ProcessResult;



public class CliUtils {

    public static boolean getConfigFilePath(final String[] args, ProcessResult result) {
        // build file option
        Options options = new Options();
        Option fileOption = new Option(FILEPATH.opt,
                FILEPATH.longOpt, FILEPATH.hasArg, FILEPATH.optDesc);
        if (FILEPATH.hasArg) {
            fileOption.setArgName(FILEPATH.argDesc);
        }
        options.addOption(fileOption);
        // parse args
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cli = parser.parse(options, args);
            if (cli == null) {
                result.setFailResult("Parse args failure");
                return result.success;
            }
            if (!cli.hasOption(FILEPATH.longOpt)) {
                result.setFailResult(new StringBuilder(512)
                        .append("Please input the configuration file path by ")
                        .append("-").append(FILEPATH.opt).append(" or ")
                        .append("-").append(FILEPATH.longOpt).append(" option").toString());
                return result.success;
            }
            String configFilePath = cli.getOptionValue(FILEPATH.longOpt);
            if (TStringUtils.isBlank(configFilePath)) {
                result.setFailResult(new StringBuilder(512)
                        .append(FILEPATH.longOpt).append(" is required!").toString());
                return result.success;
            }
            result.setSuccResult(configFilePath);
        } catch (Throwable e) {
            result.setFailResult(new StringBuilder(512)
                    .append("Parse configuration file path failure: ")
                    .append(e.toString()).toString());
        }
        return result.success;
    }
}
