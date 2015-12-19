/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: amrk
 * Date: Jul 11, 2005
 * Time: 9:02:27 PM
 */
package com.theoryinpractice.testng.model;

import org.testng.remote.strprotocol.GenericMessage;
import org.testng.remote.strprotocol.IRemoteSuiteListener;
import org.testng.remote.strprotocol.IRemoteTestListener;
import org.testng.remote.strprotocol.SuiteMessage;
import org.testng.remote.strprotocol.TestMessage;
import org.testng.remote.strprotocol.TestResultMessage;
import com.theoryinpractice.testng.ui.TestNGConsoleView;
import com.theoryinpractice.testng.ui.TestNGResults;

public class TestNGRemoteListener implements IRemoteSuiteListener, IRemoteTestListener
{
	private final TestNGConsoleView console;
	private final TreeRootNode unboundOutputRoot;

	public TestNGRemoteListener(TestNGConsoleView console, TreeRootNode unboundOutputRoot)
	{
		this.console = console;
		this.unboundOutputRoot = unboundOutputRoot;
	}

	@Override
	public void onInitialization(GenericMessage genericMessage)
	{
	}

	@Override
	public void onStart(SuiteMessage suiteMessage)
	{
		final TestNGResults view = console.getResultsView();
		if(view != null)
		{
			view.start();
		}
	}

	@Override
	public void onFinish(SuiteMessage suiteMessage)
	{
		unboundOutputRoot.flush();
		console.finish();
	}

	@Override
	public void onStart(TestMessage tm)
	{
		final TestNGResults view = console.getResultsView();
		if(view != null)
		{
			view.setTotal(tm.getTestMethodCount());
		}
	}

	@Override
	public void onTestStart(TestResultMessage trm)
	{
		console.testStarted(trm);
	}

	@Override
	public void onFinish(TestMessage tm)
	{
		console.rebuildTree();
	}

	@Override
	public void onTestSuccess(TestResultMessage trm)
	{
		console.addTestResult(trm);
	}

	@Override
	public void onTestFailure(TestResultMessage trm)
	{
		console.addTestResult(trm);
	}

	@Override
	public void onTestSkipped(TestResultMessage trm)
	{
		console.addTestResult(trm);
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(TestResultMessage trm)
	{
	}
}
