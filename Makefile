OS := $(shell uname)
CC := javac

ifeq ($(OS), Windows_NT)
	SEP:=;
else
	SEP:=:
endif

SRC_DIR := ./src

SRC := $(shell find ./src/main -name '*.java')
SRC_TEST := $(shell find ./src/test -name '*.java')

BUILD_DIR := ./build

OBJS  := $(patsubst $(SRC_DIR)/%.java, $(BUILD_DIR)/%.class, $(SRC))
OBJS_TEST := $(patsubst $(SRC_DIR)/%.java, $(BUILD_DIR)/%.class, $(SRC_TEST))
LIBS := $(shell find ./lib -name '*.jar')
TEST_LIBS := $(wildcard ./testlib/*.jar)

CP_OBJS := $(shell echo $(SRC_DIR)$(addprefix $(SEP), $(LIBS)) | tr -d ' ')
CP_BUILD := $(shell echo $(BUILD_DIR)$(addprefix $(SEP), $(LIBS)) | tr -d ' ')
CP_TEST := $(shell echo $(CP_BUILD)$(addprefix $(SEP), $(TEST_LIBS)) | tr -d ' ')

all: etags $(OBJS)
	java -cp  $(CP_BUILD) dio.challenge.Main

run-jar: jar
	java -jar $(BUILD_DIR)/BankAccount.jar

jar: etags $(OBJS)
	 jar --create --file $(BUILD_DIR)/BankAccount.jar \
	--main-class dio.challenge.Main -C build .

$(OBJS) : $(BUILD_DIR)/%.class : $(SRC_DIR)/%.java
	$(CC) -g -cp $(CP_OBJS) $< -d $(BUILD_DIR)

$(OBJS_TEST) : $(BUILD_DIR)/%.class : $(SRC_DIR)/%.java
	$(CC) -g -cp $(CP_TEST) $< -d $(BUILD_DIR)

test: $(OBJS) $(OBJS_TEST)
	java -cp $(CP_TEST)  org.junit.runner.JUnitCore dio.challenge.TestMain

etags:
	etags $(SRC) --include '~/java11_src/TAGS'

debug-attach:
	jdb -sourcepath ./src/main/ \
	-connect com.sun.jdi.SocketAttach:hostname=localhost,port=9000

debug-server:
	java -cp $(CP_BUILD) -Xdebug \
		-Xrunjdwp:transport=dt_socket,address=9000,server=y,suspend=y \
		dio.challenge.Main

clean:
	rm -r $(BUILD_DIR)

