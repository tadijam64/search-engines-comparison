import groovy.transform.Field
import org.apache.sling.api.resource.ModifiableValueMap
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ValueMap


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


def predicates = [path: "/content/dam/we-retail", type: "dam:Asset",  "orderby.index": "true", "orderby.sort": "desc"]
def query = createQuery(predicates)
query.hitsPerPage = 500
def result = query.result
println "${result.totalMatches} hits, execution time = ${result.executionTime}s\n--"

result.hits.each { hit ->
    def path=hit.node.path
    Resource res = resourceResolver.getResource(path)
    if(res!=null){
        setValuesToChildren(res);
    }
}

session.save()

def setValuesToChildren(res){
    def r = new Random();
    Iterator<Resource> children = res.listChildren();

    while (children.hasNext()) {
        Resource child = children.next();
        String parentNodeName = child.getName();
        Resource assetRes = child.getParent();
        ValueMap properties = assetRes.adaptTo(ValueMap.class);
        String type = properties.get("jcr:primaryType");
        println(type)

        if (parentNodeName.equals("jcr:content") && type.equals("dam:Asset")) {

            Node node = child.adaptTo(Node.class);
            node.setProperty("description", descriptionList.get(r.nextInt(descriptionListSize)));
            node.setProperty("sellingPriority", pageImportanceList.get(r.nextInt(pageImportanceListSize)));
            node.setProperty("creationDate", manualCreationDateList.get(r.nextInt(manualCreationDateListSize)));

            ModifiableValueMap valueMap = child.adaptTo(ModifiableValueMap.class);
            for (String key : valueMap.keySet()) {
                String value = valueMap.get(key, String.class);
                println 'Key-'+key+' value-'+value
            }
        }
    }
}