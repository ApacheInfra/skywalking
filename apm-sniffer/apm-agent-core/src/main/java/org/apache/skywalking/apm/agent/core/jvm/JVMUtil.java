/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.agent.core.jvm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.util.CollectionUtil;
import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;

public class JVMUtil {

    private static final ILog LOGGER = LogManager.getLogger(JVMUtil.class);
    public static Instrumentation INSTRUMENTATION;
    private final static String PATH_SEPARATOR = "/";
    private final static String JAR_SEPARATOR = "!";
    private static List<String> LAST_LIB_JAR_NAMES = new ArrayList<>();

    /**
     * Build the required JVM information to add to the instance properties
     */
    public static List<KeyStringValuePair> buildJVMInfo() {
        List<KeyStringValuePair> jvmInfo = new ArrayList<>();
        jvmInfo.add(KeyStringValuePair.newBuilder().setKey("Start Time").setValue(getVmStartTime()).build());
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        jvmInfo.add(KeyStringValuePair.newBuilder().setKey("JVM Arguments").setValue(gson.toJson(getVmArgs())).build());
        List<String> libJarNames = getLibJarNames();
        if (isLibJarNamesUpdated(libJarNames)) {
            jvmInfo.add(KeyStringValuePair.newBuilder().setKey("Jar Dependencies").setValue(gson.toJson(libJarNames)).build());
            LAST_LIB_JAR_NAMES.clear();
            LAST_LIB_JAR_NAMES.addAll(libJarNames);
        }
        return jvmInfo;
    }

    private static String getVmStartTime() {
        long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startTime));
    }

    private static List<String> getVmArgs() {
        List<String> vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        List<String> sortedVmArgs = new ArrayList<>(vmArgs);
        Collections.sort(sortedVmArgs);
        return sortedVmArgs;
    }

    private static List<String> getLibJarNames() {
        List<URL> classLoaderUrls = loadClassLoaderUrls();
        return extractLibJarNamesFromURLs(classLoaderUrls);
    }

    private static List<URL> loadClassLoaderUrls() {
        List<URL> classLoaderUrls = new ArrayList<>();
        Class[] clazzs = INSTRUMENTATION.getAllLoadedClasses();
        Set<ClassLoader> classLoaders = new HashSet<>();
        for (Class clazz : clazzs) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null) {
                classLoaders.add(clazz.getClassLoader());
            }
        }

        for (ClassLoader classLoader : classLoaders) {
            try {
                if (classLoader instanceof URLClassLoader) {
                    URLClassLoader webappClassLoader = (URLClassLoader) classLoader;
                    URL[] urls = webappClassLoader.getURLs();
                    classLoaderUrls.addAll(Arrays.asList(urls));
                }
            } catch (Exception e) {
                LOGGER.warn("Load classloader urls exception: {}", e.getMessage());
            }
        }
        return classLoaderUrls;
    }

    private static List<String> extractLibJarNamesFromURLs(List<URL> urls) {
        List<String> libJarNames = new ArrayList<>();
        for (URL url : urls) {
            try {
                String libJarName = extractLibJarName(url);
                if (libJarName.endsWith(".jar")) {
                    libJarNames.add(libJarName);
                }
            } catch (Exception e) {
                LOGGER.warn("Extracting library name exception: {}", e.getMessage());
            }
        }
        if (!CollectionUtil.isEmpty(libJarNames)) {
            Collections.sort(libJarNames.stream().distinct().collect(Collectors.toList()));
        }
        return libJarNames;
    }

    private static String extractLibJarName(URL url) {
        String protocol = url.getProtocol();
        if (protocol.equals("file")) {
            return extractNameFromFile(url.toString());
        } else if (protocol.equals("jar")) {
            return extractNameFromJar(url.toString());
        } else {
            return "";
        }
    }

    private static String extractNameFromFile(String fileUri) {
        int lastIndexOfSeparator = fileUri.lastIndexOf(PATH_SEPARATOR);
        if (lastIndexOfSeparator < 0) {
            return fileUri;
        } else {
            return fileUri.substring(lastIndexOfSeparator + 1);
        }
    }

    private static String extractNameFromJar(String jarUri) {
        String uri = jarUri.substring(0, jarUri.lastIndexOf(JAR_SEPARATOR));
        return extractNameFromFile(uri);
    }

    private static boolean isLibJarNamesUpdated(List<String> libJarNames) {
        if (CollectionUtil.isEmpty(LAST_LIB_JAR_NAMES) || libJarNames.size() != LAST_LIB_JAR_NAMES.size()) {
            return true;
        }
        for (int i = 0; i < libJarNames.size(); i++) {
            if (!libJarNames.get(i).equals(LAST_LIB_JAR_NAMES.get(i))) {
                return true;
            }
        }
        return false;
    }

}