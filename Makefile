VERSION=$$(git describe)
DEPLOYDIR=transport_$(VERSION)
DATADIR=transport_testdata_$(VERSION)

all: deploy data
.PHONY: all clean build deploy deploy_dir data

deploy: $(DEPLOYDIR).zip

$(DEPLOYDIR).zip: deploy_dir build
	cp README.md $(DEPLOYDIR)/README.md
	cp transport/target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar $(DEPLOYDIR)/transport.jar
	zip -r $(DEPLOYDIR).zip $(DEPLOYDIR)

deploy_dir:
	mkdir -p "$(DEPLOYDIR)"

data: $(DATADIR).zip

$(DATADIR).zip: data_dir
	cp -r transport/src/test/resources/testdata/* $(DATADIR)
	zip -r $(DATADIR).zip $(DATADIR)

data_dir:
	mkdir -p "$(DATADIR)"

build: transport/target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar

transport/target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar:
	mvn install -f transport

clean:
	mvn clean -f transport
	rm -f transport_v*.zip
	rm -r transport_v*
