package we.retail.core.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SolrUtils
{
    public static final String PROP_SEARCH_ROOT_ASSETS = "/content/dam/we-retail";

    // private constructor to avoid unnecessary instantiation of the class
    private SolrUtils()
    {
    }

    /**
     * This method converts jcr formatted date to Solr specification format
     * @param cal Takes input as Calendar
     * @return Solr formatted date of type string
     */
    public static String castToSolrDate(Calendar cal)
    {
        if (cal != null)
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-DD'T'hh:mm:ss");
            return dateFormat.format(cal.getTime()) + "Z";
        }
        else
        {
            return null;
        }
    }
}
