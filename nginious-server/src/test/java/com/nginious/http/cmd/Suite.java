/**
 * Copyright 2012 NetDigital Sweden AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.nginious.http.cmd;

import junit.framework.Test;
import junit.framework.TestSuite;

public class Suite extends TestSuite {

    public static Test suite() {
    	TestSuite suite = new TestSuite();
    	
    	suite.addTest(new CmdTestCase("testShortNameCmd"));
    	suite.addTest(new CmdTestCase("testLongNameCmd"));
    	suite.addTest(new CmdTestCase("testInvalidCmd"));
    	suite.addTest(new CmdTestCase("testHelp"));
    	
    	return suite;
    }
    
    public static void main(String[] argv) {
    	junit.textui.TestRunner.run(suite());
    }
}
