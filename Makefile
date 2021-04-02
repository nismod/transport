VERSION=$(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout -f transport)
DEPLOYDIR=transport_$(VERSION)
DATADIR=transport_testdata_$(VERSION)

all: deploy data
.PHONY: all clean build deploy deploy_dir data

deploy: $(DEPLOYDIR).zip

$(DEPLOYDIR).zip: deploy_dir build
	cp README.md $(DEPLOYDIR)/README.md
	cp transport/target/transport-$(VERSION).jar $(DEPLOYDIR)/transport.jar
	zip -r $(DEPLOYDIR).zip $(DEPLOYDIR)

deploy_dir:
	mkdir -p "$(DEPLOYDIR)"

data: $(DATADIR).zip

$(DATADIR).zip: data_dir
	cp -r transport/src/test/resources/testdata/* $(DATADIR)
	zip -r $(DATADIR).zip $(DATADIR)

data_dir:
	mkdir -p "$(DATADIR)"

build: transport/target/transport-$(VERSION).jar

transport/target/transport-$(VERSION).jar:
	mvn package -Dmaven.test.skip=true -f transport

clean:
	mvn clean -f transport
	rm -f transport_*.zip
	rm -r transport_*
