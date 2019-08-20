package we.retail.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest
{

    @Test
    public void removeExistingStartingSlash()
    {
        assertTrue(StringUtils.equals(StringUtils.removeStartingSlash("/content/structure/search"), "content/structure/search"));
    }

    @Test
    public void removeNonExistingStartingSlash()
    {
        assertTrue(StringUtils.equals(StringUtils.removeStartingSlash("content/structure/search"), "content/structure/search"));
    }
}