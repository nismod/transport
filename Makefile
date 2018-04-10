VERSION=$$(git describe)
DEPLOYDIR=transport_$(VERSION)

all: deploy
.PHONY: all clean build deploy deploy_dir

deploy: $(DEPLOYDIR).zip

$(DEPLOYDIR).zip: deploy_dir build
	cp README.md $(DEPLOYDIR)/README.md
	cp transport/target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar $(DEPLOYDIR)/transport.jar
	zip -r $(DEPLOYDIR).zip $(DEPLOYDIR)

deploy_dir:
	mkdir -p "$(DEPLOYDIR)"

build: transport/target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar

transport/target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar:
	mvn install -f transport

clean:
	mvn clean -f transport
	rm -f $(DEPLOYDIR).zip
	rm -r $(DEPLOYDIR)
