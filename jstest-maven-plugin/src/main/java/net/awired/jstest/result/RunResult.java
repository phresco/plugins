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
package net.awired.jstest.result;

import java.util.ArrayList;
import java.util.List;
import net.awired.jscoverage.result.CoverageResult;
import nl.bitwalker.useragentutils.UserAgent;

public class RunResult {

    private final List<SuiteResult> suiteResults = new ArrayList<SuiteResult>();
    private long duration;
    private UserAgent userAgent;
    private boolean emulator;
    private CoverageResult coverageResult;
    private final int browserId;

    public RunResult(int browserId) {
        this.browserId = browserId;
    }

    public String userAgentToString() {
        if (emulator) {
            return "emulator";
        } else {
            return userAgent.getOperatingSystem().getName() //
                    + ',' + userAgent.getBrowser().getName() //
                    + ' ' + userAgent.getBrowserVersion();
        }
    }

    public void addSuite(SuiteResult suiteResult) {
        suiteResults.add(suiteResult);
    }
    
    public int findTests() {
        int tests = 0;
        for (SuiteResult suiteResult : suiteResults) {
            tests += suiteResult.getTests().size();
        }
        return tests;
    }

    public int findFailures() {
        int failures = 0;
        for (SuiteResult suiteResult : suiteResults) {
            failures += suiteResult.getFailures();
        }
        return failures;
    }

    public int findErrors() {
        int errors = 0;
        for (SuiteResult suiteResult : suiteResults) {
            errors += suiteResult.getErrors();
        }
        return errors;
    }

    public int findSkipped() {
        int skipped = 0;
        for (SuiteResult suiteResult : suiteResults) {
            skipped += suiteResult.getSkipped();
        }
        return skipped;
    }

    private long findAggregatedDuration() {
        long duration = 0;
        for (SuiteResult suiteResult : suiteResults) {
            duration += suiteResult.getDuration();
        }
        return duration;
    }

    public boolean isFinished() {
        return duration > 0;
    }

    //////////////////////////////:

    public List<SuiteResult> getSuiteResults() {
        return suiteResults;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public void setBrowserType(String browserType) {
        userAgent = UserAgent.parseUserAgentString(browserType);
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    public void setEmulator(boolean emulator) {
        this.emulator = emulator;
    }

    public boolean isEmulator() {
        return emulator;
    }

    public int getBrowserId() {
        return browserId;
    }

    public CoverageResult getCoverageResult() {
        return coverageResult;
    }

    public void setCoverageResult(CoverageResult coverageResult) {
        this.coverageResult = coverageResult;
    }

}
