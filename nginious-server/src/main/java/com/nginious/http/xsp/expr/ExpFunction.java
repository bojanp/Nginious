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

package com.nginious.http.xsp.expr;

/**
 * Returns Euler's number e raised to the power of an argument value.
 * 
 * @author Bojan Pisler, NetDigital Sweden AB
 *
 */
public class ExpFunction extends MathFunction {
	
	/**
	 * Constructs a new exponent function.
	 * 
	 * @param value the argument value
	 */
    public ExpFunction(Value value) {
        super("exp", value);
    }
    
    /**
     * Calculates the result of this function using this functions argument
     * value.
     * 
     * @return the result of this function
     */
    protected double getDoubleValue() {
    	return Math.exp(value.getDoubleValue());
    }
}
