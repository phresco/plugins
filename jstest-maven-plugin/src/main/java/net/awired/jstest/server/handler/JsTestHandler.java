/**
 * JsTest Maven Plugin
 *
 * Copyright (C) 1999-2014 Photon Infotech Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.awired.jstest.server.handler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.awired.jstest.resource.ResourceResolver;
import net.awired.jstest.runner.Runner;
import net.awired.jstest.runner.RunnerType;
import net.awired.jstest.runner.TestType;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class JsTestHandler extends AbstractHandler {

	private final Log log;
    private final ResourceResolver resourceResolver;
    private final FaviconHandler faviconHandler = new FaviconHandler();
    private final ResourceHandler resourceHandler;
    private final RunnerResourceHandler runnerHandler;
    private final ResultHandler resultHandler;
    private final Runner runnerGenerator;
    private final RunnerType runnerType;
    private final TestType testType;
    private File instrumentedDir;
    private final UUID runId = UUID.randomUUID();

    private int browserId = 0;

    public JsTestHandler(ResultHandler result, Log log, ResourceResolver resolver, RunnerType runnerType,
            TestType testType, boolean serverMode, boolean debug, List<String> amdPreloads) {
        this.log = log;
        this.resourceResolver = resolver;
        this.runnerType = runnerType;
        this.testType = testType;
        this.runnerHandler = new RunnerResourceHandler(log);
        this.resourceHandler = new ResourceHandler(log, resolver);
        this.resultHandler = result;
        this.runnerGenerator = runnerType.buildRunner(testType, resolver, serverMode, debug, amdPreloads);
    }
    
    public JsTestHandler(ResultHandler result, Log log, ResourceResolver resolver, RunnerType runnerType,
            TestType testType, boolean serverMode, boolean debug, List<String> amdPreloads, File instrumentedDir) {
    	
    	this(result, log, resolver, runnerType, testType, serverMode, debug, amdPreloads);
    	this.instrumentedDir = instrumentedDir;
    }

    @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
    
   
        try {
            response.addDateHeader("EXPIRES", 0L);
            response.addHeader("CACHE_CONTROL", "NO_CACHE");
            response.addHeader("PARAGMA", "NO CACHE");
            response.addHeader("Access-Control-Allow-Origin", "*");
            if (target.startsWith("/favicon")) {
                faviconHandler.handle(target, baseRequest, request, response);
                return;
            } else if (target.startsWith(ResourceResolver.SRC_RESOURCE_PREFIX)
                    || target.startsWith(ResourceResolver.TEST_RESOURCE_PREFIX)) {
            	if(TestType.YUITEST.equals(testType)){
            		resourceHandler.yuiHandler(target, instrumentedDir.toString(), baseRequest, request, response);
            	} else {
            		resourceHandler.handle(target, baseRequest, request, response);
            	}
            } else if (target.startsWith(RunnerResourceHandler.RUNNER_RESOURCE_PATH)) {
                runnerHandler.handle(target, baseRequest, request, response);
            } else if (target.equals("/")) {
                resourceResolver.updateChangeableDirectories();
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                response.getWriter().write(runnerGenerator.generate(generateBrowserId(), isEmulator(request), runId));
                // give root page
            } else if (target.startsWith("/result/") || target.startsWith("/log")) {
                resultHandler.handle(target, baseRequest, request, response);
            } else if (target.equals("/runId")) {
                response.getWriter().print(runId);
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
            }
        } catch (Exception e) {
            log.error("Error on processing request in test server", e);
        }

    }

    private boolean isEmulator(HttpServletRequest request) {
        boolean emulator = false;
        String emulatorParam = request.getParameter("emulator");
        if (emulatorParam != null) {
            emulator = Boolean.valueOf(emulatorParam);
        }
        return emulator;
    }

    public synchronized int generateBrowserId() {
        return browserId++;
    }

}
