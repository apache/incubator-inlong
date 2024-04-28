## Overview
The Audit Sdk is used to count the receiving and sending volume of each module in real time according to the cycle, 
and the statistical results are sent to the audit access layer according to the cycle.

## Features
### Data uniqueness
The Audit Sdk will add a unique mark to each audit, which can be used to remove duplicates.

### Unified audit standard
The Audit Sdk uses log production time as the audit standard, 
which can ensure that each module is reconciled in accordance with the unified audit standard.

## Usage
### setAuditProxy API
Set the AuditProxy ip:port list. The Audit Sdk will summarize the results according to the cycle 
and send them to the ip:port list set by the interface.
If the ip:port of the AuditProxy is fixed, then this interface needs to be called once. 
If the AuditProxy changes in real time, then the business program needs to call this interface periodically to update
```java
    HashSet<String> ipPortList=new HashSet<>();
    ipPortList.add("0.0.0.0:54041");
    AuditOperator.getInstance().setAuditProxy(ipPortList);
```

### add API
Call the add method for statistics, where the auditID parameter uniquely identifies an audit object,
inlongGroupID,inlongStreamID,logTime are audit dimensions, count is the number of items, size is the size, and logTime
is milliseconds.

#### Example of add API for Agent
```java
    AuditOperator.getInstance().add(auditID,auditTag,inlongGroupID,inlongStreamID,logTime,
        count,size,auditVersion);
```
The scenario of supplementary recording of agent data, so the version number parameter needs to be passed in.
#### Example of add API for DataProxy
```java
    AuditOperator.getInstance().add(auditID,auditTag,inlongGroupID,inlongStreamID,logTime,
        count,size,auditVersion);
```
The scenario of supplementary recording of DataProxy data, so the version number parameter needs to be passed in.

#### Example of add API for Sort
```java
    AuditReporterImpl auditReporter=new AuditReporterImpl();
        auditReporter.setAuditProxy(ipPortList);

        AuditDimensions dimensions;
        AuditValues values;
        auditReporter.add(dimensions,values);
```

##### AuditReporterImpl
In order to ensure the accuracy of auditing, each operator needs to create an auditAuditReporterImpl instance.
##### Explain of AuditDimensions
| parameter      | meaning                                                                                          |
|----------------|--------------------------------------------------------------------------------------------------|
| auditID        | audit id,each module's reception and transmission will be assigned its own independent audit-id. |   
| logTime        | log time ,each module uses the log time of the data source uniformly                             |     
| auditVersion   | audit version                                                                                    |     
| isolateKey     | Flink Checkpoint id                                                                              |
| auditTag       | audit tag,Used to mark the same audit-id but different data sources and destinations             |     
| inlongGroupID  | inlongGroupID                                                                                    |
| inlongStreamID | inlongStreamID                                                                                   | 

##### Explain of AuditValues
| parameter       | meaning       |
|----------|----------|
| count  | count  |   
| size | size   |     
| delayTime     | Data transmission delay,equal to current time minus log time |