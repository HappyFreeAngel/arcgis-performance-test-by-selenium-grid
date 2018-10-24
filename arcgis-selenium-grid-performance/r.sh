#!/bin/bash
cd /Users/happy/server/docker-app/arcgis-map-performance-test/arcgis-selenium-grid-performance
gradle build
gradle distZip

rm -rf /Users/happy/server/docker-app/arcgis-map-performance-test/arcgis-selenium-grid-performance/build/distributions/arcgis-selenium-grid-performance-1.0-SNAPSHOT

cd /Users/happy/server/docker-app/arcgis-map-performance-test/arcgis-selenium-grid-performance/build/distributions/
unzip arcgis-selenium-grid-performance-1.0-SNAPSHOT.zip
cd /Users/happy/server/docker-app/arcgis-map-performance-test/arcgis-selenium-grid-performance/build/distributions/arcgis-selenium-grid-performance-1.0-SNAPSHOT/bin
pwd;ls -al;

cp ../../../../arcgis.conf .
./arcgis-selenium-grid-performance arcgis.conf

