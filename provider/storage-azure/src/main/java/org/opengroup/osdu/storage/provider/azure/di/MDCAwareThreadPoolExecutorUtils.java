// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.storage.provider.azure.di;

import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.concurrent.Callable;

public class MDCAwareThreadPoolExecutorUtils {
    public static <T> Callable<T> wrapWithMdcContext(Callable<T> task, RequestAttributes context) {
        //save the current MDC context
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        //save the current request context
        return () -> {
            setMDCContext(contextMap);
            RequestContextHolder.setRequestAttributes(context);
            try {
                return task.call();
            } finally {
                // once the task is complete, clear MDC
                MDC.clear();
            }
        };
    }

    public static Runnable wrapWithMdcContext(Runnable task, RequestAttributes context) {
        //save the current MDC context
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            setMDCContext(contextMap);
            RequestContextHolder.setRequestAttributes(context);
            try {
                task.run();
            } finally {
                // once the task is complete, clear MDC
                MDC.clear();
            }
        };
    }

    public static void setMDCContext(Map<String, String> contextMap) {
        MDC.clear();
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }
}
