# Transport
[![Build Status](https://travis-ci.com/nismod/transport.svg?token=zHcMSQsYgUFq9yhr52P7&branch=master)](https://travis-ci.com/nismod/transport)

This is the home of the transport model code for NISMOD v2.0.0.

## Description

The new transport model forecasts the impact of endogenous and exogenous factors on transport demand and capacity utilisation, following an elasticity-based simulation methodology similar to the original ITRC transport model. The new model is explicitly network-based, which means that the demand is assigned to the network to obtain more accurate predictions of travel times and capacity utilisation.

The transport sector has various links with other sectors:
* Energy: energy consumption, fuel price, electrification of vehicles, fuel transport, power outage (rail and air disruption).
* Digital Communications: supporting smart mobility (e.g. mobility as a service, autonomous mobility on demand), coverage and service disruptions.
* Water Supply: floods causing road and rail disruptions.
* Solid Waste: waste transport (e.g. waste exports through seaports).

<img src="https://cloud.githubusercontent.com/assets/7933541/21935019/abdd2b2c-d9a3-11e6-9b81-1a5acb8419bb.jpg" width="500">

The model is currently focusing on interdependencies with the energy sector:
*	The fuel price from the energy sector is used in the traffic flow prediction.
*	The transport model provides information about fuel consumption to the energy sector.

The implementation uses an open source library *GeoTools* for geospatial processing:
http://www.geotools.org/about.html

## Contact information

* Milan Lovric lovric.milan@gmail.com / M.Lovric@soton.ac.uk (Modelling and development)
* Manuel Buitrago mbm1d15@soton.ac.uk (Seaports and freight)
* James Pritchard j.a.pritchard@soton.ac.uk (Environmental emissions)
* Simon Blainey S.P.Blainey@soton.ac.uk (MISTRAL Co-Lead)

## How to run the model

1. Install *Eclipse IDE for Java Developers*: https://eclipse.org/downloads/.
2. Run Eclipse and choose the workspace folder.
3. Import the existing Maven project from the local git folder where the code has been cloned. In Eclipse: *File -> Import -> Maven -> Existing Maven Projects.* Wait until all Maven dependencies (specified in the *pom.xml* file) are downloaded. If the *pom.xml* file has been changed, the Maven project should be first updated (*Alt+F5*).
4. The classes containing the *main* method can be run as a Java application. The classes containing the methods annotated with *@Test* can be run as *JUnit* tests.
5. To run the main model in Eclipse, open the *Run Configuration* for *nismod.transport.App.java* and pass the path to the config file as an argument:

<img src="https://cloud.githubusercontent.com/assets/7933541/23258716/5c43c4f2-f9c1-11e6-9c14-13977f40ecf9.jpg" width="500">

6. To build the project and run the main model in the command prompt, type:
 * mvn package
 * java -cp target/transport-0.0.1-SNAPSHOT-main.jar nismod.transport.App ./path/to/config.properties
