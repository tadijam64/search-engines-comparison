package we.retail.core.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ExtendedStringUtilsTest
{

    @Test
    public void removeExistingStartingSlash()
    {
        assertTrue(ExtendedStringUtils.equals(ExtendedStringUtils.removeStartingSlash("/content/structure/search"), "content/structure/search"));
    }

    @Test
    public void removeNonExistingStartingSlash()
    {
        assertTrue(ExtendedStringUtils.equals(ExtendedStringUtils.removeStartingSlash("content/structure/search"), "content/structure/search"));
    }
}