package we.retail.core.util;

public class ExtendedStringUtils extends org.apache.commons.lang3.StringUtils
{
    public ExtendedStringUtils()
    {
        super();
    }

    /**
     * This method takes text to return the same text without starting "/" character if it exists
     * @param text
     * @return
     */
    public static String removeStartingSlash(String text)
    {
        if (startsWith(text, "/"))
        {
            return substring(text, 1);
        }
        return text;
    }
}
