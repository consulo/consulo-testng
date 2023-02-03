package consulo.testng;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.javaee.ResourceRegistrar;
import consulo.xml.javaee.StandardResourceProvider;

/**
 * @author VISTALL
 * @since 03/02/2023
 */
@ExtensionImpl
public class TestNGStandardResourceContributor implements StandardResourceProvider
{
	@Override
	public void registerResources(ResourceRegistrar resourceRegistrar)
	{
		resourceRegistrar.addStdResource("http://testng.org/testng-1.0.dtd", "/resources/standardSchemas/testng-1.0.dtd", getClass());
	}
}
