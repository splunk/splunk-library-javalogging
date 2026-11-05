# text reset
NO_COLOR=\033[0m
# green
OK_COLOR=\033[32;01m
# red
ERROR_COLOR=\033[31;01m
# cyan
WARN_COLOR=\033[36;01m
# yellow
ATTN_COLOR=\033[33;01m

ROOT_DIR := $(shell git rev-parse --show-toplevel)

VERSION := `git describe --tags --dirty 2>/dev/null`
COMMITHASH := `git rev-parse --short HEAD 2>/dev/null`
DATE := `date "+%FT%T%z"`

.PHONY: all
all: init test

init:
	@echo "$(ATTN_COLOR)==> init $(NO_COLOR)"

.PHONY: test
test: test_unit test_acceptance

.PHONY: test_unit
test_unit:
	@echo "$(ATTN_COLOR)==> Running unit tests $(NO_COLOR)"
	@mvn -P UnitTest -B verify --file pom.xml

.PHONY: test_acceptance
test_acceptance:
	@echo "$(ATTN_COLOR)==> Running acceptance tests $(NO_COLOR)"
	@mvn -P AcceptanceTest -B verify --file pom.xml

.PHONY: test_specific
test_specific:
	@echo "$(ATTN_COLOR)==> test_specific $(NO_COLOR)"
	@sh ./scripts/test_specific.sh

.PHONY: up
up:
	@echo "$(ATTN_COLOR)==> up $(NO_COLOR)"
	@docker compose up -d

.PHONY: wait_up
wait_up:
	@echo "$(ATTN_COLOR)==> wait_up $(NO_COLOR)"
	@for i in `seq 0 180`; do docker exec splunk /sbin/checkstate.sh >/dev/null 2>&1 && exit 0; sleep 5; done; echo "Splunk did not start in 600 seconds"; exit 1

.PHONY: down
down:
	@echo "$(ATTN_COLOR)==> down $(NO_COLOR)"
	@docker compose stop
