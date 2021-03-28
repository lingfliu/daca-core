# DACA 

## Stateful model of data flowing 


## Terms and definitions
1. Application connectivity: the connectivity for byte stream decoding & encoding and data flow management.
2. Message: a message is a byte stream to convoy data between the transceivers.
2. Flow: a flow describes a transmission of a message either in uplink or downlink direction
3. Procedure: a procedure is a sequence of flows to achieve data exchange and interactions between the transcceivers.
4. Specification: declaration of rules for coding (AttributeSegment, Code, Codebook) and data flow (Flow, Routine, Routinebook) management
5. SVO: simple value object
5. Attribute segment: specifications to parse byte stream into attributes of SVO formats
6. Code: specification of message composition
7. Codebook: code collection of the protocol
8. Routine: specification to identify different procedures
9. Routinebook: collection of all  routines of a protocol
10. Shot: singleton data flow without routine
11. Uplink: data transmission from client to host
12. Downlink: data transmission from host to client

## Code attributes

### 1. Header
A header is a fixed byte arrays for the transceivers to locate the beginning of a message.

A protocol should contain only one header. There are protocols supporting different headers, which is not currently supported.

Some protocols doesn't specify the header because the messages are short. In this case, the codes can be headless. For headless messages, the decoding is triggered everytime the transceiver is sending byte streams.

###2. Meta data 
Meta data is the common segment of the message streams holding attributes e.g. type, id, auth. 
A meta data should be of fixed length for all attributes

###3. Payload
Payload can be of variant length.

###4. Code common attributes
1. Attributes of variant length should declare the length explicitly in either meta or payload prior to the data, or should be implicitly end-marked by a tail attributes in the payload. 
3. By defaults, strings are encoded in ASCII.

###5. Routine common attributes

