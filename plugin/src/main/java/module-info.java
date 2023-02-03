/**
 * @author VISTALL
 * @since 03/02/2023
 */
module consulo.testng
{
	requires consulo.ide.api;
	requires consulo.java;

	requires consulo.java.execution.api;
	requires consulo.java.execution.impl;

	requires consulo.util.xml.fast.reader;

	requires org.testng;
	requires jcommander;

	// TODO remove in future
	requires java.desktop;
	requires forms.rt;

	opens com.theoryinpractice.testng.configuration to consulo.util.xml.serializer;
	opens com.theoryinpractice.testng.model to consulo.util.xml.serializer;
	opens com.theoryinpractice.testng.inspection to consulo.util.xml.serializer;

	// cache value manager checks
	opens com.theoryinpractice.testng.util to consulo.application.impl;
}