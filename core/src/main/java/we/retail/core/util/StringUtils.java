package we.retail.core.util;

public class StringUtils extends org.apache.commons.lang3.StringUtils
{
    public StringUtils()
    {
        super();
    }

    public static String removeStartingSlash(String text){
        if (startsWith(text, "/")) {
            return substring(text, 1);
        }
        return text;
    }
}
