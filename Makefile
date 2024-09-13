OS := $(shell uname)
CC := javac

ifeq ($(OS), Windows_NT)
	SEP := ;
else
	SEP := :
endif

SRC_DIR := ./src

SRC := 	$(shell find . -name '*.java')

BUILD_DIR := ./build

OBJS  := $(patsubst %.java, %.class, $(SRC))
LIBS := 

CP_OBJS := $(SRC_DIR)$(addprefix $(SEP), $(LIBS)) 
CP_BUILD := $(BUILD_DIR)$(addprefix $(SEP), $(LIBS)) 

all: $(OBJS)
	java -cp  $(CP_BUILD) dio.challenge.Main

$(OBJS) : %.class : %.java
	$(CC) -cp $(CP_OBJS) $< -d $(BUILD_DIR)

clean:
	rm -r $(BUILD_DIR)

