JFLAGS = -g
JCC = javac

default: packet.class commons.class client.class server.class

client.class: client.java
	$(JCC) $(JFLAGS) client.java

server.class: server.java
	$(JCC) $(JFLAGS) server.java

packet.class: packet.java
	$(JCC) $(JFLAGS) packet.java

commons.class: commons.java
	$(JCC) $(JFLAGS) commons.java

clean: 
	$(RM) server.class commons.class client.class packet.class
