package au.com.dius.pact.provider.junit.loader;

import org.junit.Test;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class VersionedPactUrlLoaderTest {
    @Test
    public void replacesVersion() throws Exception {
        String versionVariable = "jupiter.version";
        System.setProperty(versionVariable, "555");

        String[] strings = VersionedPactUrlLoader
                .expandVariables(new String[]{format("http://artifactory:8081/jupiter-hydra/${%1$s}/jupiter-hydra-${%1$s}.json", versionVariable)});

        assertEquals("http://artifactory:8081/jupiter-hydra/555/jupiter-hydra-555.json", strings[0]);
        assertEquals(1, strings.length);
    }

    @Test
    public void replacesVersions() throws Exception {
        String versionVariable1 = "jupiter.version";
        String versionVariable2 = "saturn.version";
        System.setProperty(versionVariable1, "555");
        System.setProperty(versionVariable2, "666");

        String[] strings = VersionedPactUrlLoader
                .expandVariables(new String[]{format("http://artifactory:8081/jupiter-hydra/${%s}/jupiter-hydra-${%s}.json", versionVariable1, versionVariable2)});

        assertEquals("http://artifactory:8081/jupiter-hydra/555/jupiter-hydra-666.json", strings[0]);
        assertEquals(1, strings.length);
    }

    @Test
    public void failsWhenNoVersionSpecified() throws Exception {
        String versionVariable = "jupiter.version";
        System.clearProperty(versionVariable);

        try {
            VersionedPactUrlLoader.expandVariables(new String[]{format("http://artifactory:8081/jupiter-hydra/${%s}/jupiter-hydra-${%s}.json", versionVariable, versionVariable)});
            fail();
        } catch (Exception e) {
            assertEquals("http://artifactory:8081/jupiter-hydra/${jupiter.version}/jupiter-hydra-${jupiter.version}.json contains variables that could not be any of the system properties. Define a system property to replace them or remove the variables from the URL.", e.getMessage());
        }
    }
}