﻿# Transport
[![Build Status](https://travis-ci.com/nismod/transport.svg?token=zHcMSQsYgUFq9yhr52P7&branch=master)](https://travis-ci.com/nismod/transport)
[![Documentation Status](https://readthedocs.org/projects/nt2/badge/?version=latest)](https://nt2.readthedocs.io/en/latest/?badge=latest)
[![Code Coverage](https://img.shields.io/codecov/c/github/nismod/transport/master.svg)](https://codecov.io/github/nismod/transport?branch=master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This is the home of the NISMOD v2 Transport Model.

## Description

NISMOD v2 Transport Model is a national-scale (*Great Britain*) transport model developed to support policy making regarding the future infrastructure. It forecasts the impact of various endogenous and exogenous factors on transport demand and capacity utilisation, following an elasticity-based simulation methodology. The model consists of three submodels covering the following modes of transport: road (passenger and freight vehicle flows), rail (total station usage), and air (passenger and flight movements).

## How to run the model

0. Install *Java Development Kit* version 8 from: http://www.oracle.com.
1. Install *Eclipse IDE for Java Developers*: https://eclipse.org/downloads/.
2. Run Eclipse and choose the workspace folder.
3. Import the existing Maven project from the local git folder where the code has been cloned. In Eclipse: *File -> Import -> Maven -> Existing Maven Projects.* Wait until all Maven dependencies (specified in the *pom.xml* file) are downloaded. If the *pom.xml* file has been changed, the Maven project should be first updated (*Alt+F5*).
4. The classes containing the *main* method can be run as a Java application. The classes containing the methods annotated with *@Test* can be run as *JUnit* tests.
5. To run the main model in Eclipse, open the *Run Configuration* for *nismod.transport.App.java* and pass the path to the config file as an argument:

[<img alt="Configuration" src="images/configuration.jpg" style="max-width:500px" />](images/configuration.jpg)

6. Alternatively, to build the project and run the main model in the command prompt:
    * Make sure the Java home environment variable is set for the operating system and pointing to the directory where *Java Development Kit* has been installed.
    * Download maven, install it and set the environment variables: http://maven.apache.org/. Then type:  
       `mvn clean install`
    * To run the base-year *road* model (2015) type:
       `java -cp target/transport-0.0.1-SNAPSHOT.jar nismod.transport.App -c ./path/to/config.properties -b`
    * To predict and run a future year (e.g. 2020) using the results of a previously run year (e.g. 2015), for *road* model type:
       `java -cp target/transport-0.0.1-SNAPSHOT.jar nismod.transport.App -c ./path/to/config.properties -road 2020 2015`
    * To predict and run a future year (e.g. 2020) using the results of a previously run year (e.g. 2015), for *rail* model type:
       `java -cp target/transport-0.0.1-SNAPSHOT.jar nismod.transport.App -c ./path/to/config.properties -rail 2020 2015`
    * To run the interactive showcase demo (1920 x 1080 resolution required) type:
       `java -cp target/transport-0.0.1-SNAPSHOT.jar nismod.transport.App -c ./path/to/config.properties -d`

    * Options:

        * To increase the max heap size, run with `java -XX:MaxHeapSize=120g ...`
        * To enable debug messages, run with `java -Dlog4j2.debug ...`

## Contact information

* Milan Lovric lovric.milan@gmail.com / M.Lovric@soton.ac.uk (Modelling and development)
* Simon Blainey S.P.Blainey@soton.ac.uk (MISTRAL Co-Lead)
* John Preston J.M.Preston@soton.ac.uk (MISTRAL Co-Lead)
* Manuel Buitrago mbm1d15@soton.ac.uk (Seaports and freight)


## Acknowledgments 

This work has been undertaken at the *University of Southampton*, as part of the ITRC consortium, under grant EP/N017064/1 (MISTRAL: Multi-scale InfraSTRucture systems AnaLytics) of the UK *Engineering and Physical Science Research Council* (EPSRC).  
https://www.itrc.org.uk/

The test resources contain a sample of data and shapefiles that come with the following licencing and copyright statemens:
* *Open Government Licence:*  
http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/
* Contains *National Statistics* data © *Crown* copyright and database right 2012.
* Contains *Ordnance Survey* data © *Crown* copyright and database right 2012.

The authors acknowledge the use of the IRIDIS *High Performance Computing Facility*, and associated support services at the *University of Southampton*, in the completion of this work.

The implementation uses an open source library *GeoTools* for geospatial processing.  
http://www.geotools.org/about.html