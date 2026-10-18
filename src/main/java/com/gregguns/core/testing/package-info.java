/**
 * The in-game scenario harness: scenarios, their registry, the runner and the report formatting. The framework classes
 * are plain Java so JUnit can test them; only {@link com.gregguns.core.testing.WorldScenarioContext} and the built-in
 * scenarios touch the world. Scenarios run on the logical server only.
 */
package com.gregguns.core.testing;
