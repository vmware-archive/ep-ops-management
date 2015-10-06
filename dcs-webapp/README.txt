EPOps Webapp
------------

What is it?
-----------

Epops Webapp is a GPLv3 application.
Its purpose is to:
1. Expose the Epops model of all the supported monitored resources.
2. Act as a middleware between the Epops agents and the Backend components. It passes the requests from the Apache to the Agent Adapter.

The document describes what EPOps webapp API exposes, and what it sends to the Agent Adapter.

Licensing
---------

GPLv3:
http://www.gnu.org/licenses/gpl-3.0.html


API
---

This is the API exposed by the EPOps webapp:

URL: http://localhost:8081/epops-webapp/epops/
Description: This webapp api exposes the model of the monitored resources.

e.g. to get the model from command line:
curl -H "Content-Type: application/json" -d '["/usr/lib/vmware-vcops/user/plugins/inbound/agent_adapter/conf/plugins/agent_plugins/system-plugin.jar", 
"/usr/lib/vmware-vcops/user/plugins/inbound/agent_adapter/conf/plugins/netservices-1.0.0/netservices-plugin.jar", 
"/usr/lib/vmware-vcops/user/plugins/inbound/agent_adapter/conf/plugins/agent-1.0.0/hqagent-plugin.jar"]' 
http://localhost:8081/epops-webapp/epops/ | python -m json.tool > epops_model.json

curl -H "Content-Type: application/json" -d '["/usr/lib/vmware-vcops/user/plugins/inbound/agent_adapter/conf/plugins/agent_plugins/system-plugin.jar", 
"/usr/lib/vmware-vcops/user/plugins/inbound/agent_adapter/conf/plugins/netservices-1.0.0/netservices-plugin.jar", 
"/usr/lib/vmware-vcops/user/plugins/inbound/agent_adapter/conf/plugins/agent-1.0.0/hqagent-plugin.jar"]' 
http://localhost:8081/epops-webapp/epops/ | python -m json.tool > epops_model.json

[
    {
        "configProperties": [
            {
                "advanced": null,
                "defaultValue": "data",
                "description": "Path to Directory",
                "displayName": "path",
                "enumType": false,
                "enumValues": null,
                "hidden": false,
                "mandatory": true,
                "name": "path",
                "password": false,
                "readonly": false,
                "sensitive": false,
                "type": "string"
            }
        ],
        "discoverableProperties": [],
        "light": true,
        "measurementProperties": [
            {
                "defaultMonitored": false,
                "discrete": false,
                "group": "AVAILABILITY",
                "indicator": false,
                "key": "ResourceAvailability",
                "name": "Resource Availability",
                "units": "none"
            },
            {
                "defaultMonitored": false,
                "discrete": false,
                "group": "UTILIZATION",
                "indicator": false,
                "key": "Sockets",
                "name": "Sockets",
                "units": "none"
            },
            {
                "defaultMonitored": false,
                "discrete": false,
                "group": "UTILIZATION",
                "indicator": false,
                "key": "SymbolicLinks",
                "name": "Symbolic Links",
                "units": "none"
            },
            {
                "defaultMonitored": false,
                "discrete": false,
                "group": "UTILIZATION",
                "indicator": false,
                "key": "DiskUsage",
                "name": "Disk Usage",
                "units": "bytes"
            },
            {
                "defaultMonitored": false,
                "discrete": false,
                "group": "UTILIZATION",
                "indicator": false,
                "key": "RegularFiles",
                "name": "Regular Files",
                "units": "none"
            }
        ],
        "name": "FileServer Directory Tree",
        "parents": [
            "AIX",
            "Linux",
            "Solaris",
            "HPUX",
            "Windows"
        ],
        "remote": false,
        "roaming": false,
        "virtual": false
    },

...
]


The EPOps webapp sends requests to the URLs below:
(Each API contains the name, http url, description, example of the Epops webapp request in json format and the Backend component response in json format)

1. RegisterCommandData
URL: POST http://localhost:8877/agentAdapterCommand/RegisterCommandData
Description: Register an agent to vROps. The agent should authenticate using user name and password. The user should have "AgentManager" role in vROps and provide input for the certificate 
sign request (CSR) which includes a unique UUID as the CSR common name. The server will authenticate the agent and return the signed certificate which should be used by the agent for further
communication.
request: 
{
"version":"1.0.0",
"cpuCount":2,
"certificateRequest":
"TUlJQ3JEQ0NBWlFDQVFBd1p6RVZNQk1HQTFVRUNoTU1WazEzWVhKbExDQkpibU11TVE0d0RBWURWUVFMRXdWQloyVnVkREUrTUR3R0ExVUVBeE0xTVRRek5qSTNPRFl5TWpNME9DMHhNamN4T1RneE1USTRPREl4TkRFek1qRXhMVGMwTkRNME1USTJPVGt4TmpZNE5EVTRORE13Z2dFaU1BMEdDU3FHU0liM0RRRUJBUVVBQTRJQkR3QXdnZ0VLQW9JQkFRQ050TlZZZ2R1aWVhdDMxTW5xbHJWemNwaVJCbXNEWmhlWk9KeEpKR2hrcU9JQzh6YnNJcHJ0V3krNXFtQkdkSk90K2pCYmpKSUtPSmtCdXFyTHpTY25ET3RvMnBaeGhWNzVPZ3gxNE1QQ0FOdENjR2VtMzkwMmRqZVN1eWM0bnNnZkFBQitsUmhRWjErbUlYU1RCNnk2T1ZYSXNGdUNyM2tZdnpQT1MrTkFvWWxPTGxYN0xpRFF2MnR4VE9QcEZJQzdxVUxVUU5yTHRFaTB2dVNiVG5rOXU4bUcxYy94SDdKdzVrT0Z4Z2hmM3QwZWhuMDZBR1JuODR1b09Raml0bWMyalcyTDF4akwyS2VpZVhJcmFyelhTOTB2YTRHOVNoYmlSVWFHSXMwSTRWcXc5bGcvSWMxUUdadVF6V2xWbW9KYUh6UFZBUHRuQW9tbWpTcm1jN0NMQWdNQkFBR2dBREFOQmdrcWhraUc5dzBCQVEwRkFBT0NBUUVBZk1JSEpkNGFOK0RJQUNMU3VxUk1tWjhQTGY3RWdKcVA2dmFPS2IySzRreWVVZU1BT1owMlVRVzZpTnZIaVArU3RFd2hWQ2tHakRSY1pRdUdkdlVrRnQxd2x4bXJva0FQTldQanVwZmlYeWc3dmY5bW0xK2UreE1PWWthSHgySWdUb05ucndtVS8xYnMzd3lUR01waEFiWENTMUxTSHNKV0t5UGQ5Vk03ZUpLS1BrUHFxUVJZUG9oZnRCT0lJVkxKUkhmRHI4RnVFNkJXdHlZMFZLYTBKYktZMlphU1J3a0pIc01RWUpXOUtmWmFIMlNBOTZFRnQzYU1FTVFkU242OUNDWXR4R2VVNlRsRTgzTzNkYU5NTnBDQ1dOWkcrNUMxNFZ2M3ZQVnZuRXdmUFlrUk0vYjRzalI0T3c1eHBHL25mVkFyWGYvNmc3LzJWZytLbXRXZTB3PT0=",
"agentIp":"EP Ops Agent - vRealizeClusterNode",
"userName":"admin",
"password":"Admin12345!",
"commandName":"registerAgent"
}
 
response: 200
{
"errorCode":null,
 "errorString":null,
  "certificate":"-----BEGIN CERTIFICATE-----\nMIIEdTCCAl2gAwIBAgIUcw4Jhn8vyDFHFDx7RkqjxP3+kGkwDQYJKoZIhvcNAQEN\nBQAwZjE/MD0GA1UEAww2dmMtb3BzLWNsdXN0ZXItY2FfMjNhYzc5NjgtMGYzMi00\nNzgxLWE4NmYtMjgxYmY3NzJkMDg5MRUwEwYDVQQKDAxWTXdhcmUsIEluYy4xDDAK\nBgNVBAsMA01CVTAeFw0xNTA3MDYxNDE3MTZaFw0yMDA3MDUxMzUwNDFaMGcxFTAT\nBgNVBAoTDFZNd2FyZSwgSW5jLjEOMAwGA1UECxMFQWdlbnQxPjA8BgNVBAMTNTE0\nMzYyNzg2MjIzNDgtMTI3MTk4MTEyODgyMTQxMzIxMS03NDQzNDEyNjk5MTY2ODQ1\nODQzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjbTVWIHbonmrd9TJ\n6pa1c3KYkQZrA2YXmTicSSRoZKjiAvM27CKa7VsvuapgRnSTrfowW4ySCjiZAbqq\ny80nJwzraNqWcYVe+ToMdeDDwgDbQnBnpt/dNnY3krsnOJ7IHwAAfpUYUGdfpiF0\nkwesujlVyLBbgq95GL8zzkvjQKGJTi5V+y4g0L9rcUzj6RSAu6lC1EDay7RItL7k\nm055PbvJhtXP8R+ycOZDhcYIX97dHoZ9OgBkZ/OLqDkI4rZnNo1ti9cYy9inonly\nK2q810vdL2uBvUoW4kVGhiLNCOFasPZYPyHNUBmbkM1pVZqCWh8z1QD7ZwKJpo0q\n5nOwiwIDAQABoxowGDAWBgNVHSUBAf8EDDAKBggrBgEFBQcDAjANBgkqhkiG9w0B\nAQ0FAAOCAgEAWKmX/UYTF8W6qBtlKPgn++7WJcqniM+QFPLweY5vnw0pr2wzsBbw\nJZWiBtcaeFEEsRhGODdvEv76f6+YLboV1JfzWUQwAt1OUPLvTN/yAhv+Ufq37+XK\ntT9ZsEG6KMMf2Gsozp4OYD79B0ekpH2qD33R9hK47QVOQsSwbto6LPVprXGf+Vba\nwxOfbYuhdtRjpMyNSExmSYerNSBrkUm8V3vqCU1Q1G0curXwO3YhvFE51dqG7tmU\nMy68JJRYpDIOAOb8zR9DAZ4zKkT71l5tv46dLEPOWuSOR3W5jIGC6PyM83Dr9vAw\nEr2iLSpQJRzajkXTPzG4GNVZgZe4AtymbtmHk6OwsAeVIm7N2c3L5MxAAYs/X2cL\n42ys/rIJUB+y315K2rQyoE33iLFtM9vDtKLGeL1c9tKjJbeypZx6TL+0fK0RGHs9\n0ReIHlLo6PnOXFjDgiKiVLHCSfdR5B07C6n4ovTVq9xJW40Qk+gvpe2RxcnJhaMd\nYB5GxqhzXMxMnkFRTVxCslPFE/X7tda0SvBRSygkTnynoEXzEIESLpJ1rcQuAuZA\npd9AXUmcUcBcJnxiU/TLSPTKtG082Hxdx7wvUKA7qBaq4GAXE+SDvGw+NhKEHMLw\nBDIMzw5bCFBuvQ3UTvsTyVVf9PGkkldNtgaEf3LxGfOo82Tmtzn+A0w=\n-----END CERTIFICATE-----\n"

}


2. AiSendReportCommandData
URL: POST http://localhost:8877/agentAdapterCommand/AiSendReportCommandData
Description: This command represents the Operating System and the servers discovered by the agent. The server creates the respective resources in vROps, and then sends a configuration to the agent
to start monitoring (see #8). The configuration is based on the describe and the vROps configured policy.
A resource has a sync boolean field. If this field is "true", the vROps server will know that it needs to send the configuration to this agent.
request: 
{
"agentToken":"1436278622348-1271981128821413211-7443412699166845843","commandName":"aiSendReport","rawResources":[{"resourceName":"vRealizeClusterNode","resourceType":"Linux","rawIdentifier":"1436278622348-1271981128821413211-7443412699166845843","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":true,"children":[{"resourceName":"EP Ops Agent - vRealizeClusterNode","resourceType":"EP Ops Agent","rawIdentifier":"EP Ops Agent","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":true,"children":[],"configProperties":{"installpath":"/opt/vmware/epops-agent-1.0.0"},"discoveredProperties":{"SigarVersion":"1.6.6.0","SigarNativeVersion":"1.6.6.0","AgentBundleVersion":"agent-x86-64-linux-1.0.0","UserHome":"/root","name":"EP Ops Agent - vRealizeClusterNode","JavaVendor":"Oracle Corporation","description":"EP Ops Agent","JavaVersion":"1.8.0_45","version":"1.0.0"},"metrics":null},{"resourceName":"FileServer","resourceType":"FileServer","rawIdentifier":"10.23.206.243 Linux FileServer","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":true,"children":[],"configProperties":{"installpath":"/"},"discoveredProperties":{"name":"FileServer"},"metrics":null},{"resourceName":"NetworkServer","resourceType":"NetworkServer","rawIdentifier":"10.23.206.243 Linux NetworkServer","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":true,"children":[],"configProperties":{"installpath":"/"},"discoveredProperties":{"name":"NetworkServer"},"metrics":null},{"resourceName":"ProcessServer","resourceType":"ProcessServer","rawIdentifier":"10.23.206.243 Linux ProcessServer","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":true,"children":[],"configProperties":{"installpath":"/"},"discoveredProperties":{"name":"ProcessServer"},"metrics":null}],"configProperties":{},"discoveredProperties":{"vendorVersion":"11","cpuSpeed":"2 @ 2660 MHz","primaryDNS":"10.23.192.1","fqdn":"10.23.206.243","ip":"10.23.206.243","description":"SuSE 11","type":"Linux","defaultGateway":"10.23.207.253","version":"3.0.101-0.47.52-default","vm_kind":"VMware","vendor":"SuSE","name":"vRealizeClusterNode","network_config":"[127.0.0.1, 255.0.0.0, 00:00:00:00:00:00];[10.23.206.243, 255.255.252.0, 00:50:56:97:78:28]","secondaryDNS":"10.23.192.2","arch":"x86_64","ram":"8008 MB"},"metrics":null}]
}
response: 200 empty 


3. AiSendRuntimeReportCommandData
URL: POST http://localhost:8877/agentAdapterCommand/AiSendRuntimeReportCommandData
Description: This command represents the services discovered by the agent. The server creates the respective resources in vROps, and then sends a configuration to the agent to start monitoring 
the services (see #8). The configuration is based on the describe and the vROps configured policy.
A resource has a sync boolean field. If this field is "true", the vROps server will know that it needs to send the configuration to this agent.
request:
{
"agentToken":"1436278622348-1271981128821413211-7443412699166845843","commandName":"aiSendRuntimeReport","rawResources":[{"resourceName":"vRealizeClusterNode","resourceType":"Linux","rawIdentifier":"1436278622348-1271981128821413211-7443412699166845843","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[{"resourceName":"NetworkServer","resourceType":"NetworkServer","rawIdentifier":"10.23.206.243 Linux NetworkServer","internalId":32,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[{"resourceName":"Network Interface lo (loopback)","resourceType":"NetworkServer Interface","rawIdentifier":"Network Interface lo (loopback)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"interface":"lo"},"discoveredProperties":{"address":"127.0.0.1","netmask":"255.0.0.0","flags":"UP LOOPBACK RUNNING","mtu":"16436"},"metrics":null},{"resourceName":"Network Interface lo (loopback)","resourceType":"NetworkServer Interface","rawIdentifier":"Network Interface lo (loopback)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"interface":"lo"},"discoveredProperties":{"address":"127.0.0.1","netmask":"255.0.0.0","flags":"UP LOOPBACK RUNNING","mtu":"16436"},"metrics":null},{"resourceName":"Network Interface eth0 (ethernet)","resourceType":"NetworkServer Interface","rawIdentifier":"Network Interface eth0 (ethernet)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"interface":"eth0"},"discoveredProperties":{"broadcast":"10.23.204.255","address":"10.23.206.243","netmask":"255.255.252.0","flags":"UP BROADCAST RUNNING MULTICAST","mac":"00:50:56:97:78:28","mtu":"1500"},"metrics":null}],"configProperties":{"installpath":"/"},"discoveredProperties":{"name":"NetworkServer"},"metrics":null}],"configProperties":{},"discoveredProperties":{"vendorVersion":"11","cpuSpeed":"2 @ 2660 MHz","primaryDNS":"10.23.192.1","fqdn":"10.23.206.243","ip":"10.23.206.243","description":"SuSE 11","type":"Linux","defaultGateway":"10.23.207.253","version":"3.0.101-0.47.52-default","vm_kind":"VMware","vendor":"SuSE","name":"vRealizeClusterNode","network_config":"[127.0.0.1, 255.0.0.0, 00:00:00:00:00:00];[10.23.206.243, 255.255.252.0, 00:50:56:97:78:28]","secondaryDNS":"10.23.192.2","arch":"x86_64","ram":"8008 MB"},"metrics":null},{"resourceName":"vRealizeClusterNode","resourceType":"Linux","rawIdentifier":"1436278622348-1271981128821413211-7443412699166845843","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[{"resourceName":"ProcessServer","resourceType":"ProcessServer","rawIdentifier":"10.23.206.243 Linux ProcessServer","internalId":32,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[{"resourceName":"CPU 1 (2660Mhz Intel Xeon)","resourceType":"CPU","rawIdentifier":"CPU 1 (2660Mhz Intel Xeon)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"cpu":"0"},"discoveredProperties":{},"metrics":null},{"resourceName":"CPU 2 (2660Mhz Intel Xeon)","resourceType":"CPU","rawIdentifier":"CPU 2 (2660Mhz Intel Xeon)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"cpu":"1"},"discoveredProperties":{},"metrics":null}],"configProperties":{"installpath":"/"},"discoveredProperties":{"name":"ProcessServer"},"metrics":null}],"configProperties":{},"discoveredProperties":{"vendorVersion":"11","cpuSpeed":"2 @ 2660 MHz","primaryDNS":"10.23.192.1","fqdn":"10.23.206.243","ip":"10.23.206.243","description":"SuSE 11","type":"Linux","defaultGateway":"10.23.207.253","version":"3.0.101-0.47.52-default","vm_kind":"VMware","vendor":"SuSE","name":"vRealizeClusterNode","network_config":"[127.0.0.1, 255.0.0.0, 00:00:00:00:00:00];[10.23.206.243, 255.255.252.0, 00:50:56:97:78:28]","secondaryDNS":"10.23.192.2","arch":"x86_64","ram":"8008 MB"},"metrics":null},{"resourceName":"vRealizeClusterNode","resourceType":"Linux","rawIdentifier":"1436278622348-1271981128821413211-7443412699166845843","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[{"resourceName":"FileServer","resourceType":"FileServer","rawIdentifier":"10.23.206.243 Linux FileServer","internalId":32,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[{"resourceName":"/dev/sda","resourceType":"FileServer Block Device","rawIdentifier":"/dev/sda","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"name":"sda"},"discoveredProperties":{},"metrics":null},{"resourceName":"/dev/sdb","resourceType":"FileServer Block Device","rawIdentifier":"/dev/sdb","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"name":"sdb"},"discoveredProperties":{},"metrics":null},{"resourceName":"/dev/sdc","resourceType":"FileServer Block Device","rawIdentifier":"/dev/sdc","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"name":"sdc"},"discoveredProperties":{},"metrics":null},{"resourceName":"/dev/sda3 mounted on / (local/ext3)","resourceType":"FileServer Mount","rawIdentifier":"/dev/sda3 mounted on / (local/ext3)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"mount":"/"},"discoveredProperties":{},"metrics":null},{"resourceName":"/dev/sda1 mounted on /boot (local/ext3)","resourceType":"FileServer Mount","rawIdentifier":"/dev/sda1 mounted on /boot (local/ext3)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"mount":"/boot"},"discoveredProperties":{},"metrics":null},{"resourceName":"/dev/mapper/data-core mounted on /storage/core (local/ext3)","resourceType":"FileServer Mount","rawIdentifier":"/dev/mapper/data-core mounted on /storage/core (local/ext3)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"mount":"/storage/core"},"discoveredProperties":{},"metrics":null},{"resourceName":"/dev/mapper/data-log mounted on /storage/log (local/ext3)","resourceType":"FileServer Mount","rawIdentifier":"/dev/mapper/data-log mounted on /storage/log (local/ext3)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"mount":"/storage/log"},"discoveredProperties":{},"metrics":null},{"resourceName":"/dev/mapper/data-db mounted on /storage/db (local/ext3)","resourceType":"FileServer Mount","rawIdentifier":"/dev/mapper/data-db mounted on /storage/db (local/ext3)","internalId":null,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{"mount":"/storage/db"},"discoveredProperties":{},"metrics":null}],"configProperties":{"installpath":"/"},"discoveredProperties":{"name":"FileServer"},"metrics":null}],"configProperties":{},"discoveredProperties":{"vendorVersion":"11","cpuSpeed":"2 @ 2660 MHz","primaryDNS":"10.23.192.1","fqdn":"10.23.206.243","ip":"10.23.206.243","description":"SuSE 11","type":"Linux","defaultGateway":"10.23.207.253","version":"3.0.101-0.47.52-default","vm_kind":"VMware","vendor":"SuSE","name":"vRealizeClusterNode","network_config":"[127.0.0.1, 255.0.0.0, 00:00:00:00:00:00];[10.23.206.243, 255.255.252.0, 00:50:56:97:78:28]","secondaryDNS":"10.23.192.2","arch":"x86_64","ram":"8008 MB"},"metrics":null}]
}

response: 200 empty


4. MeasurementReportCommandData
URL: POST http://localhost:8877/agentAdapterCommand/MeasurementReportCommandData
Description: Represents the metrics values of a monitoring resource in a point of time.
request:
{
"agentToken":"1436278622348-1271981128821413211-7443412699166845843","metrics":null,"commandName":"measurementSendReport","rawResources":[{"resourceName":null,"resourceType":null,"rawIdentifier":null,"internalId":32,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{},"discoveredProperties":{},"metrics":{"2899":[{"timestamp":1436278860000,"metricValue":100.0},{"timestamp":1436278800000,"metricValue":100.0}]}},{"resourceName":null,"resourceType":null,"rawIdentifier":null,"internalId":31,"agentToken":"1436278622348-1271981128821413211-7443412699166845843","sync":false,"children":[],"configProperties":{},"discoveredProperties":{},"metrics":{"2899":[{"timestamp":1436278860000,"metricValue":100.0},{"timestamp":1436278800000,"metricValue":100.0}]}}]
}

response: 200 empty


5. ServerInfoCommandData
URL: POST http://localhost:8877/agentAdapterCommand/ServerInfoCommandData
Description: Returns the certificate type the the vROps server is using. (custom or default)
The certificate type determines which certificate will be stored in the agent keystore.
request: 
{
"commandName":"serverInfo"
}
 
response: 200
{
"errorCode":null,"errorString":null,"customCertificate":false
}


6. VerifyCommandData
URL: POST http://localhost:8877/agentAdapterCommand/VerifyCommandData
Description: Verify that the agent is a registered agent. This call is made from the EPOps webapp to the Agent Adapter at the beginning of handling every command to be sure the server is not
processing commands from a non valid agent. (e.g. for a measurementReportCommandData, the webapp first sends a VerifyCommandData to the server and just then continue processing the measurement.
request:
{
"certificateSerialNumber":"730e09867f2fc83147143c7b464aa3c4fdfe9069","agentToken":"1436278622348-1271981128821413211-7443412699166845843","agentIp":null,"userName":null,"password":null,"commandName":"VerifyAgent"
}
 
response: 200
{
"errorCode":null,"errorString":null,"token":"1436278622348-1271981128821413211-7443412699166845843"}
}


7. GetAgentQueueCommandData
URL: POST http://localhost:8877/agentAdapterCommand/GetAgentQueueCommandData
Description: This commands returns all the messages that are waiting for the agent in his mailbox. In this case it is a configuration command for the Linux monitored by the agent.
request:
{
  "agentToken": "1423743704635-3592667989218084828-100039927906453229",
  "commandName": "getAgentQueue"
}
response:
{
"errorCode":null,"errorString":null,"agentCommandsQueue":[{"commandUUID":{"agentToken":"1436278622348-1271981128821413211-7443412699166845843","deliveryTime":0,"commandType":"CONFIGURE_RESOURCE","uuid":"1381c16e-dfda-4628-ba06-f3866c61c28c"},"agentMailCommand":{"resourceId":32,"resourceKind":"Linux","publicConfiguration":{"agentID":"1436278622348-1271981128821413211-7443412699166845843","discoveryMode":"","monitoredResourceID":"1436278622348-1271981128821413211-7443412699166845843","relatedVmId":"","Override_agent_configuration_data":"false","parentID":"NO_PARENT"},"securedConfiguration":{},"properties":{"vendorVersion":"11","virtual_servers":"FileServer:{name=FileServer};NetworkServer:{name=NetworkServer};ProcessServer:{name=ProcessServer}","lastSyncTime":"1436278746747","cpuSpeed":"2 @ 2660 MHz","fqdn":"10.23.206.243","primaryDNS":"10.23.192.1","ip":"10.23.206.243","System Properties|resource_kind_type":"GENERAL","description":"SuSE 11","type":"Linux","defaultGateway":"10.23.207.253","version":"3.0.101-0.47.52-default","System Properties|resource_kind_subtype":"GENERAL","vm_kind":"VMware","vendor":"SuSE","name":"vRealizeClusterNode","network_config":"[127.0.0.1, 255.0.0.0, 00:00:00:00:00:00];[10.23.206.243, 255.255.252.0, 00:50:56:97:78:28]","secondaryDNS":"10.23.192.2","arch":"x86_64","id":"32","Missing Configuration":"false","ram":"8008 MB"},"enableMetrics":[{"metricId":2899,"metricName":"ResourceAvailability","pollingInterval":60000},{"metricId":2908,"metricName":"CpuUsage","pollingInterval":300000},{"metricId":2910,"metricName":"PercentUsedMemory","pollingInterval":300000},{"metricId":2909,"metricName":"PercentUsedSwap","pollingInterval":300000},{"metricId":2906,"metricName":"SwapTotal","pollingInterval":300000},{"metricId":2904,"metricName":"Totaldiskcapacity","pollingInterval":300000},{"metricId":2905,"metricName":"TotalMemory","pollingInterval":300000},{"metricId":2907,"metricName":"UsedMemory","pollingInterval":300000}],"configuration":{"agentID":"1436278622348-1271981128821413211-7443412699166845843","discoveryMode":"","monitoredResourceID":"1436278622348-1271981128821413211-7443412699166845843","relatedVmId":"","Override_agent_configuration_data":"false","parentID":"NO_PARENT"},"deleteResource":false,"securedConfigurationDecrypted":{},"commandType":"CONFIGURE_RESOURCE","commandDetails":"[Command type: CONFIGURE_RESOURCE]"}}]
}


8. GetAgentQueueCommandData
URL: POST http://localhost:8877/agentAdapterCommand/GetAgentQueueCommandData
Description: This commands returns all the messages that are waiting for the agent in his mailbox. In this case the message to the agent is the list of plugins that should be downloaded by it and the respective URLs.
request:
{
  "agentToken": "1423743704635-3592667989218084828-100039927906453229",
  "commandName": "getAgentQueue"
}
response:
{
"errorCode": null, "errorString": null,"agentCommandsQueue": [{"commandUUID": {"agentToken": "1436278622348-1271981128821413211-7443412699166845843","deliveryTime": 0, "commandType": "AGENT_UPDATE_FILES","uuid": "2c93305a-c07f-492e-976a-18052518d34f" }, "agentMailCommand": { "filesToUpdate": [ { "sourceFileURI": "/agent-plugins/hqagent-plugin.jar", "destFileRelativePath": "${agent.bundle.home}/pdk/plugins/hqagent-plugin.jar",
"md5sum": "42ab90521a0b22439c0ab502ae5b920c"},{ "sourceFileURI": "/agent-plugins/netservices-plugin.jar", "destFileRelativePath": "${agent.bundle.home}/pdk/plugins/netservices-plugin.jar","md5sum": "602b8bb6f073320ca8ac20a5128260fb" }], "filesToRemove": [],"restartIfSuccessful": false,"commandType": "AGENT_UPDATE_FILES","commandDetails": "[Command type: AGENT_UPDATE_FILES]"}}]
}


9. PluginReportCommandData
URL: POST http://localhost:8877/agentAdapterCommand/PluginReportCommandData
Description: Represents the plugins and their MD5 that are installed on the agent. The server then knows if the agent is having the latest plugins that it should have and updates it if necessary.
request:
{
"agentToken":"1436278622348-1271981128821413211-7443412699166845843",
"commandName":"pluginSendReport",
"pluginFileNameToChecksumMap":{"system-plugin.jar":"1bd6e344a657947ce653027e2ea0a8b9",
"hqagent-plugin.jar":"7d8ad8ee82146c05efbce62bba44f632"}
}
response: 200 empty


10. ReturnAgentResultCommandData
URL: POST http://localhost:8877/agentAdapterCommand/ReturnAgentResultCommandData
Description:The Agent Adapter gets in this service the results of the agents after processing previous commands sent. Currently not used by vROPs
request:
{
"agentToken":"1436278622348-1271981128821413211-7443412699166845843","responses":[{"commandUUID":{"agentToken":"1436278622348-1271981128821413211-7443412699166845843","deliveryTime":0,"commandType":"CONFIGURE_RESOURCE","uuid":"4b734148-aa9e-4180-b476-dc11ced750c7"},"agentMailResponse":null},{"commandUUID":{"agentToken":"1436278622348-1271981128821413211-7443412699166845843","deliveryTime":0,"commandType":"AGENT_UPDATE_FILES","uuid":"2c93305a-c07f-492e-976a-18052518d34f"},"agentMailResponse":null}],"commandName":"returnAgentResult"
}
response: 200
{"errorCode":null,"errorString":null}


11. health-check
URL: GET http://localhost:8877/health-check
Description: Returns the status of the Agent Adapter(e.g. ONLINE). Can be used by monitoring tools or load balancer.
request: empty
response:
ONLINE


12. GetDisabledPlugins
URL: POST http://localhost:8877/agentAdapterCommand/GetDisabledPluginsCommandData
Description: Returns a list of all disabled epops plugins.
request:
{
  "agentToken": "1423743704635-3592667989218084828-100039927906453229",
  "commandName": "getDisabledPlugins"
}
response: 200
{"errorCode":null,"errorString":null,"disabledPlugins":["postgresql","vcenter-relationships"]}

