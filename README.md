# Apache Lucene, Apache Solr and Elasticsearch comparison on Adobe Experience Manager

## Goal

Exploring **capabilities** **of** ***search engines*** mentioned in the title.
This is done with different types of content and with different technical cases. It was also important to explore behavior of the search engines with AEM's technology stack.

Proper testing of search engines capabilities requires large amount of data. To avoid looking for relevant content for imaginary business case, existing AEM reference site - We Retail is used.

## Description

Many options of the search engines are explored and then compared.

Using search engines is a critical functionality for almost all applications, but to choose which one to use primarily depends on you business case. However, more detailed comparison and implementation details are going to be available on my GitHub profile under - Wiki pages.

*This project is practical part of Master's Thesis "**Implementing Search Engines in Adobe Experience Manager**" on Faculty of Organization and Informatics. Theory part of Thesis can be found in the root of the project. It is written in Croatian, but additional blogs on implementation details are going to be available soon.*

## Functionalities

This project adds the following functionalities to the We Retail app:

- search component is updated with **option for user to choose search engine to execute query** with the given term. Options are:
  - Lucene (OOTB AEM Lucene Oak)
  - Apache Solr
  - Elasticsearch
- user can **choose to search different types of content**. Options are:
  - Pages (all nodes with cq:Page type - i.e. products)
  - Assets (all nodes with dam:Asset type - i.e pictures of products for content authors to edit)
  - All content (pages and assets combined in results)
  - By tag (look for all pages and assets with tag that matches the given term)

## Technologies
- **Adobe Experience Manager 6.4.2**
- **Apache Lucene 8.2.0**
- **Apache Solr 8.2.0**
- **Elasticsearch 7.4.1**
- **Java 8**
- **AEM Groovy 13.0.0 - writing scripts for additional content rendering** 


# Implementation

## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as servlets or request filters.
* ui.apps: contains the /apps (and /etc) parts of the project, ie JS&CSS clientlibs, components, templates, runmode specific configs as well as Hobbes-tests
* ui.content: contains sample content using the components from the ui.apps
* it.tests: Java bundle containing JUnit tests that are executed server-side. This bundle is not to be deployed onto production.
* it.launcher: contains glue code that deploys the ui.tests bundle (and dependent bundles) to the server and triggers the remote JUnit execution
* all: additional module to build a single package embedding ui.apps and ui.content

## How to build

**Install clean AEM 6.4 instance**

When you have a running AEM instance you can package the whole project and deploy into AEM with  

```Maven
mvn clean install -PautoInstallPackage
```

Additionally, bundle should be deployed to the author with running

```Maven
mvn clean install -PautoInstallBundle
```

To run a Groovy scripts under /resources/groovyScripts, install

- Groovy console for AEM - https://github.com/icfnext/aem-groovy-console/releases/tag/13.0.0
- Then you can run Groovy scripts directly on Author via http://localhost:4902/apps/groovyconsole.html


## Testing

There are three levels of testing contained in the project:

* unit test in core: this show-cases classic unit testing of the code contained in the bundle. To test, execute:

    mvn clean test

* server-side integration tests: this allows to run unit-like tests in the AEM-environment, ie on the AEM server. To test, execute:

    mvn clean integration-test -PintegrationTests

* client-side Hobbes.js tests: JavaScript-based browser-side tests that verify browser-side behavior. To test:

    in the navigation, go the 'Operations' section and open the 'Testing' console; the left panel will allow you to run your tests.


## Maven settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html



Key words: **Apache Lucene, Apache Solr, Elasticsearch**, AEM, search engines
