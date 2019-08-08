/*
 * Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.tool.util;

import java.util.Locale;

/**
 * Utility functions used by launcher.
 */
public class OSUtils {

    private static final String OS = System.getProperty("os.name").toLowerCase(Locale.getDefault());

    /**
     * Returns name of the operating system running. null if not a unsupported operating system.
     * @return operating system
     */
    public static String getConfigPath() {
        if (OSUtils.isWindows()) {
            return "C:\\Program Files\\Ballerina\\";
        } else if (OSUtils.isUnix() || OSUtils.isSolaris()) {
            return "/usr/lib/ballerina/";
        } else if (OSUtils.isMac()) {
            return "/Library/Ballerina/";
        }
        return null;
    }

    private static boolean isWindows() {
        return (OS.contains("win"));
    }

    private static boolean isMac() {
        return (OS.contains("mac"));
    }

    private static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    }

    private static boolean isSolaris() {
        return (OS.contains("sunos"));
    }
}
