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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tubemq.corebase.utils.TStringUtils;
import org.apache.tubemq.server.broker.BrokerConfig;
import org.apache.tubemq.server.broker.exception.StartupException;
import org.apache.tubemq.server.master.MasterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for .ini configure file loading.
 * Copied from <a href="https://github.com/killme2008/Metamorphosis">Metamorphosis Project</a>
 */
public class ToolUtils {

    private static final Logger logger = LoggerFactory.getLogger(ToolUtils.class);

    public static BrokerConfig getBrokerConfig(final String configFilePath) {
        final BrokerConfig brokerConfig = new BrokerConfig();
        brokerConfig.loadFromFile(configFilePath);
        logger.info("Broker config is: " + brokerConfig);
        return brokerConfig;
    }

    public static MasterConfig getMasterConfig(final String configFilePath) {
        final MasterConfig masterConfig = new MasterConfig();
        masterConfig.loadFromFile(configFilePath);
        logger.info("master config is: " + masterConfig);
        return masterConfig;
    }

    public static String getConfigFilePath(final String[] args) throws StartupException {
        final Options options = new Options();
        final Option file = new Option("f", true, "configuration file path");
        options.addOption(file);
        final CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (final ParseException e) {
            throw new StartupException("Parse command line failed", e);
        }
        String configFilePath = null;
        if (line.hasOption("f")) {
            configFilePath = line.getOptionValue("f");
        } else {
            System.err.println("Please tell me the configuration file path by -f option");
            System.exit(1);
        }
        if (TStringUtils.isBlank(configFilePath)) {
            throw new StartupException("Blank file path");
        }
        return configFilePath;
    }
}
