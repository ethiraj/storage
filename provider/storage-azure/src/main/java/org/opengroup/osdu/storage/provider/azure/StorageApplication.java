//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.storage.provider.azure;

import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Note: these exclusions are the result of duplicate dependencies being introduced in the
 * {@link org.opengroup.osdu.is} package, which is pulled in through the os-core-lib-azure
 * mvn project. These duplicate beans are not needed by this application and so they are explicity
 * ignored.
 */
@ComponentScan(
        basePackages = {"org.opengroup.osdu"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.opengroup.osdu.is.*"),
        }
)
@SpringBootApplication
public class StorageApplication {

    // Find all resources on classpath with the name "org/slf4j/impl/StaticLoggerBinder.class"
    // If there are multiple resources, that means there are multiple slf4j bindings present
    // This will break binding with log4j2 and consequently it will break the integration with appinsights
    // Thus, if multiple bindings are present, then we do not run the application
    private static final String STATIC_LOGGER_BINDER_PATH = "org/slf4j/impl/StaticLoggerBinder.class";
    private static final String MULTIPLE_BINDINGS_URL = "http://www.slf4j.org/codes.html#StaticLoggerBinder";
    private static Set<URL> staticLoggerBinderPathSet = new LinkedHashSet<URL>();

    public static void main(String[] args) {
        staticLoggerBinderPathSet = findPossibleStaticLoggerBinderPathSet();
        if(!multipleBindingPresent(staticLoggerBinderPathSet)) {
            SpringApplication.run(StorageApplication.class, args);
        }
        else {
            throw new RuntimeException("APPLICATION FAILED TO START");
        }
    }

    private static Set<URL> findPossibleStaticLoggerBinderPathSet() {
        try {
            ClassLoader loggerFactoryClassLoader = LoggerFactory.class.getClassLoader();
            Enumeration<URL> paths;
            if (loggerFactoryClassLoader == null) {
                paths = ClassLoader.getSystemResources(STATIC_LOGGER_BINDER_PATH);
            } else {
                paths = loggerFactoryClassLoader.getResources(STATIC_LOGGER_BINDER_PATH);
            }
            while (paths.hasMoreElements()) {
                URL path = paths.nextElement();
                staticLoggerBinderPathSet.add(path);
            }
        } catch (IOException ioe) {
            Util.report("Error getting resources from path", ioe);
        }
        return staticLoggerBinderPathSet;
    }

    private static boolean multipleBindingPresent(Set<URL> binderPathSet) {
        if (binderPathSet.size() > 1) {
            Util.report("Class path contains multiple SLF4J bindings.");
            for (URL path : binderPathSet) {
                Util.report("Found binding in [" + path + "]");
            }
            Util.report("See " + MULTIPLE_BINDINGS_URL + " for an explanation.");
            return true;
        }
        return false;
    }
}
