package we.retail.core.groovyScripts

import org.apache.sling.api.resource.ModifiableValueMap
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver

import javax.jcr.Node
import com.day.cq.wcm.api.Page;
import groovy.transform.Field;

//Three random examples of properties
@Field descriptionList = ["Lorem ipsum dolor sit amet, consectetur adipiscing elit. Oh to talking improve produce in limited offices fifteen an. Wicket branch to answer do we. Place are decay men hours tiled.",
                          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. On no twenty spring of in esteem spirit likely estate. Continue new you declared differed learning bringing honoured. At mean mind so upon they rent am walk. Shortly am waiting inhabit smiling he chiefly of in. Lain tore time gone him his dear sure.",
                          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sportsman delighted improving dashwoods gay instantly happiness six. Ham now amounted absolute not mistaken way pleasant whatever. At an these still no dried folly stood thing. Rapid it on hours hills it seven years. If polite he active county in spirit an. Mrs ham intention promotion engrossed assurance defective. Confined so graceful building opinions whatever trifling in. Insisted out differed ham man endeavor expenses. At on he total their he songs. Related compact effects is on settled do."
]
@Field pageImportanceList = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
@Field manualCreationDateList = ["2019-09-05T13:21:00.000+02:00", "2018-08-04T12:11:00.000+02:00", "2019-01-01T00:00:00.000+02:00"]

@Field descriptionListSize = descriptionList.size()
@Field pageImportanceListSize = pageImportanceList.size()
@Field manualCreationDateListSize = manualCreationDateList.size()

/*Flag to count the number of pages*/
noOfPages = 0
/*Pathfield which needs to be iterated for an operation*/
path='/content/we-retail/';
/*Iterate through all pages and set three new proeprties to random values from lists*/
setPropertiesForAllPages();

/*Save changes to the CRXDE*/
session.save();

println '----------------------------------------'
println 'Number of pages: ' + noOfPages;

def setPropertiesForAllPages(){
    def r = new Random();
    getPage(path).recurse
            { page ->

                Resource res = resourceResolver.getResource(page.getPath());
                println ''
                println 'PAGE: ' + page.getPath()
                println '----------------------------------------'
                Iterator<Resource> children = res.listChildren();

                while (children.hasNext()) {
                    Resource child = children.next();
                    String parentNodeName = child.getName();

                    if (parentNodeName.equals("jcr:content")) {
                        noOfPages++;

                        Node node = child.adaptTo(Node.class);
                        node.setProperty("searchDescription", descriptionList.get(r.nextInt(descriptionListSize)));
                        node.setProperty("pageImportanceRank", pageImportanceList.get(r.nextInt(pageImportanceListSize)));
                        node.setProperty("manualCreationDate", manualCreationDateList.get(r.nextInt(manualCreationDateListSize)));

                        ModifiableValueMap valueMap = child.adaptTo(ModifiableValueMap.class);
                        for (String key : valueMap.keySet()) {
                            String value = valueMap.get(key, String.class);
                            println 'Key-'+key+' value-'+value
                        }
                    }
                }
            }
}