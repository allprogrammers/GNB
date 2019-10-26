JFLAGS = -g
JCC = javac

default: packet.class client.class server.class

client.class: client.java
	$(JCC) $(JFLAGS) client.java

server.class: server.java
	$(JCC) $(JFLAGS) server.java

packet.class: packet.java
	$(JCC) $(JFLAGS) packet.java

clean: 
	$(RM) server.class client.class packet.class
