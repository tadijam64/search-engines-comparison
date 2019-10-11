package we.retail.core.util;

public class ExtendedStringUtils extends org.apache.commons.lang3.StringUtils
{
    public ExtendedStringUtils()
    {
        super();
    }

    public static String removeStartingSlash(String text)
    {
        if (startsWith(text, "/"))
        {
            return substring(text, 1);
        }
        return text;
    }
}
