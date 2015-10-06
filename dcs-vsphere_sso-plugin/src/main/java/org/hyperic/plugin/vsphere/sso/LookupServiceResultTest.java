package org.hyperic.plugin.vsphere.sso;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by bharel on 4/2/2015.
 */
public class LookupServiceResultTest {

    private Map<String, Set<String>> typeToFqdns;
    private LookupServiceResult lookupServiceResult;

    @Test
    public void testParseLookupServiceOutput() {
        lookupServiceResult = new LookupServiceResult(getParseLookupServiceOutput());
        typeToFqdns = lookupServiceResult.getTypeToFqdns();

        assertResultsContains("vSphere SSO", "siesta-vc1.eng.vmware.com");
        assertResultsContains("vSphere Log Browser", "siesta-vc3.eng.vmware.com", "siesta-vc7.eng.vmware.com");
        assertResultsContains("vSphere Web Client", "siesta-vc4.eng.vmware.com");
        assertResultsContains("vCenter App Server", "siesta-vc6.eng.vmware.com", "siesta-vc7.eng.vmware.com");
    }

    @Test
    public void testParseLookupServiceOutputVC60() {
        lookupServiceResult = new LookupServiceResult(getParseLookupServiceOutput60());
        typeToFqdns = lookupServiceResult.getTypeToFqdns();

        assertResultsContains("vCenter App Server", "vcServer60-2.VC.ESO.LAB", "vcServer60.VC.ESO.LAB", "vcserver60lnx.vc.eso.lab");
    }

    private void assertResultsContains(String serviceType, String... fqdns) {
        Set<String> fqdnSet = new HashSet<String>();
        Collections.addAll(fqdnSet, fqdns);

        Assert.assertEquals(fqdnSet, typeToFqdns.get(serviceType));
    }

    private String getParseLookupServiceOutput() {
        return "Intializing registration provider...\n"
                    + "Getting SSL certificates for https://siesta-vc:7444/lookupservice/sdk\n"
                    + "Anonymous execution\n"
                    + "Found 6 services.\n"
                    + "\n"
                    + "Service 1\n"
                    + "-----------\n"
                    + "serviceId=local:30a3983b-cc44-46e0-b869-f41fd08a9ab8\n"
                    + "serviceName=The security token service interface of the SSO server\n"
                    + "type=urn:sso:sts\n"
                    + "endpoints={[url=https://siesta-vc1.eng.vmware.com:7444/sts/STSService/vsphere.local,protocol=wsTrust]}\n"
                    + "version=1.5\n"
                    + "description=The security token service interface of the SSO server\n"
                    + "ownerId=\n"
                    + "productId=product:sso\n"
                    + "viSite=local\n"
                    + "\n"
                    + "Service 2\n"
                    + "-----------\n"
                    + "serviceId=local:f2f3148c-23be-49b9-8fc7-fbe293497db0\n"
                    + "serviceName=The administrative interface of the SSO server\n"
                    + "type=urn:sso:admin\n"
                    + "endpoints={[url=https://siesta-vc2.eng.vmware.com:7444/sso-adminserver/sdk/vsphere.local,protocol=vmomi]}\n"
                    + "version=1.5\n"
                    + "description=The administrative interface of the SSO server\n"
                    + "ownerId=\n"
                    + "productId=product:sso\n"
                    + "viSite=local\n"
                    + "\n"
                    + "Service 3\n"
                    + "-----------\n"
                    + "serviceId=local:485ab19b-92b9-4edf-9000-13e6868e2546\n"
                    + "serviceName=VMware Log Browser\n"
                    + "type=urn:logbrowser:logbrowser\n"
                    + "endpoints={[url=https://siesta-vc3.eng.vmware.com:12443/vmwb/logbrowser,protocol=unknown],[url=https://siesta-vc7.eng.vmware.com:12443/authentication/authtoken,protocol=unknown]}\n"
                    + "version=1.0.1997225\n"
                    + "description=Enables browsing vSphere log files within the VMware Web Client\n"
                    + "ownerId=logbrowser-10.23.45.134-36cf527b-071f-486c-a23a-a80f6c2ae38c\n"
                    + "productId=\n"
                    + "viSite=local\n"
                    + "\n"
                    + "Service 4\n"
                    + "-----------\n"
                    + "serviceId=local:08727a60-fa5a-4618-bb9c-0282ab9046a8\n"
                    + "serviceName=vsphere-client-localhost.localdom-1874eb18-7398-4b8e-814e-75780876696d\n"
                    + "type=urn:com.vmware.vsphere.client\n"
                    + "endpoints={[url=https://siesta-vc4.eng.vmware.com:9443/vsphere-client,protocol=vmomi]}\n"
                    + "version=5.5\n"
                    + "description=vSphere Web Client at siesta-vc.eng.vmware.com\n"
                    + "ownerId=vsphere-client-localhost.localdom-1874eb18-7398-4b8e-814e-75780876696d@vsphere.local\n"
                    + "productId=\n"
                    + "viSite=local\n"
                    + "\n"
                    + "Service 5\n"
                    + "-----------\n"
                    + "serviceId=local:e6fbc395-deb2-48e6-8160-c70945ff8ae3\n"
                    + "serviceName=The group check interface of the SSO server\n"
                    + "type=urn:sso:groupcheck\n"
                    + "endpoints={[url=https://siesta-vc5.eng.vmware.com:7444/sso-adminserver/sdk/vsphere.local,protocol=vmomi]}\n"
                    + "version=1.5\n"
                    + "description=The group check interface of the SSO server\n"
                    + "ownerId=\n"
                    + "productId=product:sso\n"
                    + "viSite=local\n"
                    + "\n"
                    + "Service 6\n"
                    + "-----------\n"
                    + "serviceId=local:7f02d82b-30b9-42ca-9fe9-4c7f4206d043\n"
                    + "serviceName=vpxd-localhost.localdom-6395cd8f-cabd-4846-b9f6-b6e3f55bad67\n"
                    + "type=urn:vc\n"
                    + "endpoints={[url=https://siesta-vc6.eng.vmware.com:443/sdk,protocol=vmomi]}\n"
                    + "version=5.5\n"
                    + "description=vCenter Virtual Appliance at siesta-vc.eng.vmware.com\n"
                    + "ownerId=vpxd-localhost.localdom-6395cd8f-cabd-4846-b9f6-b6e3f55bad67@vsphere.local\n"
                    + "productId=\n"
                    + "viSite=local\n"
                    + "\n"
                    + "Service 7\n"
                    + "-----------\n"
                    + "serviceId=local:7f02d82b-30b9-42ca-9fe9-4c7f4206d043\n"
                    + "serviceName=vpxd-localhost.localdom-6395cd8f-cabd-4846-b9f6-b6e3f55bad67\n"
                    + "type=urn:vc\n"
                    + "endpoints={[url=https://siesta-vc7.eng.vmware.com:443/sdk,protocol=vmomi]}\n"
                    + "version=5.5\n"
                    + "description=vCenter Virtual Appliance at siesta-vc.eng.vmware.com\n"
                    + "ownerId=vpxd-localhost.localdom-6395cd8f-cabd-4846-b9f6-b6e3f55bad67@vsphere.local\n"
                    + "productId=\n"
                    + "viSite=local\n"
                    + "Return code is: Success\n";
    }

    private String getParseLookupServiceOutput60() {
        return "\n"
                    + "serviceVersion=6.0\n"
                    + "ownerId=vpxd-cc3a4bf0-fe32-11e4-b3f9-005056ab20e4@vsphere.local\n"
                    + "serviceType.product=com.vmware.cis\n"
                    + "serviceType.type=vcenterserver\n"
                    + "nodeId=cd968091-fe32-11e4-a5e4-005056ab20e4\n"
                    + "serviceNameResourceKey=AboutInfo.vpx.name\n"
                    + "serviceDescriptionResourceKey=AboutInfo.vpx.name\n"
                    + "attribute0.key=com.vmware.cis.cm.GroupInternalId\n"
                    + "attribute0.value=com.vmware.vim.vcenter\n"
                    + "attribute1.key=com.vmware.cis.cm.ControlScript\n"
                    + "attribute1.value=vmware-vpxd.bat\n"
                    + "attribute2.key=com.vmware.cis.cm.HostId\n"
                    + "attribute2.value=cc3a4bf0-fe32-11e4-b3f9-005056ab20e4\n"
                    + "attribute3.key=com.vmware.vim.vcenter.instanceName\n"
                    + "attribute3.value=vcServer60.VC.ESO.LAB\n"
                    + "endpoint0.type.protocol=http\n"
                    + "endpoint0.type.id=com.vmware.cis.common.resourcebundle\n"
                    + "endpoint0.url=https://vcServer60.VC.ESO.LAB:443/catalog/catalog.zip\n"
                    + "endpoint0.ssltrust0=MIIDbjCCAlagAwIBAgIJAPrquCRwOUuJMA0GCSqGSIb3DQEBCwUAMFoxCzAJBgNVBAMMAkNBMRcwFQYKCZImiZPyLGQBGRYHdnNwaGVyZTEVMBMGCgmSJomT8ixkARkWBWxvY2FsMQswCQYDVQQGEwJVUzEOMAwGA1UECgwFUFNDNjAwHhcNMTUwNTE5MTQyNDQ2WhcNMjUwNTEzMTIxMTE4WjAtMR4wHAYDVQQDDBV2Y1NlcnZlcjYwLlZDLkVTTy5MQUIxCzAJBgNVBAYTAlVTMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA8KSEa7VCTr7Td1ji64LCsxaFEjhYmyPevlwoEH+0GE0LPrT6IEmpHcR9ZwNXlRugKFbXpNzN+9cixmCwbHHP0iEda7wYP+UH8Dhc0pwtu9P6H03pAwK5Q6F+U2tcZQsqC72ae6NoC42XLlkwMaj5jiXYkrlzkiobxKl0tn+1iENy8vXn36wGru6/8bpn5DOSWGBrLQPWf8JGrd8qnJgwKnG9eeWpKytoQOnxCx1fF49bxsghgoVIh7cigodMs4ESjscrmXA8i1pIa5nGLTKWsNpbG4hVpGSHStUnnmoKDUilVPAtvRW2IEIHMqi6sQRzDywaZIYPhCo4/WOWuuX7AQIDAQABo2QwYjAgBgNVHREEGTAXghV2Y1NlcnZlcjYwLlZDLkVTTy5MQUIwHQYDVR0OBBYEFEC37G1Ik8ykQK+setUr9ir9x7h0MB8GA1UdIwQYMBaAFDYT3EJIq5kwuj0uFgD2D/Y+FLCjMA0GCSqGSIb3DQEBCwUAA4IBAQDzAwn1vb79ASTEAUUzjx2GVH9DLo/CaHC5qtCQyEql351qSIe+IUaWb/KMzwT2Ydino+5/nAKlru4Kc5EDxTtmfy/5gIZYdb4ykEXp0pdy9GFlzrfvpYkslDRuvDtqOugFLe4oxyPvGMWXCFweN6SsytljVaBNXTpLOxqWG7EFQkQxXLi6TM+Ls29Tyqm3K3HCjYYiReDMi61cBgkXGfJCfpkqK0x0hZ4DXf6+V6qigTlQ3eCKv+b5VNMFkBzW1DKVoj/37w0ulIdgwkL2oZhjK5srVW4XsL7RRqBPUQ6fLINIzL7fD48t2ZXrTMW/s4mfb/fFRvibydrC8S1e1Ye2\n"
                    + "endpoint0.data0.key=com.vmware.cis.common.resourcebundle.basename\n"
                    + "endpoint0.data0.value=cis.vc.action:cis.vc.alarm:cis.vc.auth:cis.vc.cluster:cis.vc.default:cis.vc.enum:cis.vc.evc:cis.vc.event:cis.vc.eventaux:cis.vc.fault:cis.vc.gos:cis.vc.host:cis.vc.locmsg:cis.vc.option:cis.vc.perf:cis.vc.question:cis.vc.stask:cis.vc.task:cis.vc.vm:cis.vc.profile:cis.vc.hostdiag.enum:cis.vc.hostdiag.eventaux:cis.vc.hostdiag.event:cis.vc.hostdiag.locmsg:cis.vc.VirtualCenter.enum:cis.vc.VirtualCenter.eventaux:cis.vc.VirtualCenter.event:cis.vc.VirtualCenter.extension:cis.vc.VirtualCenter.locmsg\n"
                    + "-------------------------------------------------------\n"
                    + "serviceId=5395783c-e1de-42d4-83d6-43f3994cad64\n"
                    + "serviceVersion=6.0\n"
                    + "ownerId=vpxd-c84b66a0-fec3-11e4-95eb-005056abac99@vsphere.local\n"
                    + "serviceType.product=com.vmware.cis\n"
                    + "serviceType.type=vcenterserver\n"
                    + "nodeId=c9839880-fec3-11e4-8ada-005056abac99\n"
                    + "serviceNameResourceKey=AboutInfo.vpx.name\n"
                    + "serviceDescriptionResourceKey=AboutInfo.vpx.name\n"
                    + "attribute0.key=com.vmware.cis.cm.GroupInternalId\n"
                    + "attribute0.value=com.vmware.vim.vcenter\n"
                    + "attribute1.key=com.vmware.cis.cm.ControlScript\n"
                    + "attribute1.value=vmware-vpxd.bat\n"
                    + "attribute2.key=com.vmware.cis.cm.HostId\n"
                    + "attribute2.value=c84b66a0-fec3-11e4-95eb-005056abac99\n"
                    + "attribute3.key=com.vmware.vim.vcenter.instanceName\n"
                    + "attribute3.value=vcServer60-2.VC.ESO.LAB\n"
                    + "endpoint0.type.protocol=http\n"
                    + "endpoint0.type.id=com.vmware.cis.common.resourcebundle\n"
                    + "endpoint0.url=https://vcServer60-2.VC.ESO.LAB:443/catalog/catalog.zip\n"
                    + "endpoint0.ssltrust0=MIIDcjCCAlqgAwIBAgIJANuiG0G6c23KMA0GCSqGSIb3DQEBCwUAMFoxCzAJBgNVBAMMAkNBMRcwFQYKCZImiZPyLGQBGRYHdnNwaGVyZTEVMBMGCgmSJomT8ixkARkWBWxvY2FsMQswCQYDVQQGEwJVUzEOMAwGA1UECgwFUFNDNjAwHhcNMTUwNTIwMDc0MjM3WhcNMjUwNTEzMTIxMTE4WjAvMSAwHgYDVQQDDBd2Y1NlcnZlcjYwLTIuVkMuRVNPLkxBQjELMAkGA1UEBhMCVVMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCx3B9B02UfJwlKYlfZPL+Ba42SILZWgoTuSXwcLNS+RznQtYOeMrUP9djCXIBQb8HbZ22GQdYeDCX2VHu7XCvYSQWc0ohAQdA9w5ad/+HqnqS32kKnQ0QbCbyCRh735OjrYgsnZ3cLi3BFCfMVbZ6WiRk9TULumdMgbJcjtCpEP9GTekb3Bdyla2im1K/CX++Qmlxuf1dcNgrbBzw69x4a6vGmCk68VAmkHuPFoiZXv8U5Eg6YA1IG4lFGElEH3yP8/bHKWy1MUgJuwNhpLRrlWxt1ZO1KFBWLzXAJaFj5OJ7VoDuFyjjYxfbANIgHth0TsnkYWFVQ1/sYfKLnu6EnAgMBAAGjZjBkMCIGA1UdEQQbMBmCF3ZjU2VydmVyNjAtMi5WQy5FU08uTEFCMB0GA1UdDgQWBBQK8plQOCWXp5xeAvz5Ft9pudZmyDAfBgNVHSMEGDAWgBQ2E9xCSKuZMLo9LhYA9g/2PhSwozANBgkqhkiG9w0BAQsFAAOCAQEAVnBrN4spp5/CY/n66TYLStokl2+PO+vIdpBQvTFe5qVoqX3egRLx4BRYeRmm2T6iJZlc9oLPfIbfb9+nNONUaj/T5M6rUTY+iqHxxmf0Xor8RSz9f++Lz4+/N/wq3p2vnFjtHgWesRioijuD7FQxo1S3NNBom+0C7wuvyraodpjoLSQBBJbv2YKww0w9DF4IETvHW2pcD2d13ybaVxdss2VeKNlDDKCWZ6dMsVRkzI1kv/z6qEwSeysdNwFOL516jTaAAR3DcINUnuak+3Ju0ZjFMuEwzosos8FFaqUeghpxOR72WsyRG/QDs6Ih7CdrqWG2AEBxA85njjqB/hL7WQ==\n"
                    + "endpoint0.data0.key=com.vmware.cis.common.resourcebundle.basename\n"
                    + "endpoint0.data0.value=cis.vc.action:cis.vc.alarm:cis.vc.auth:cis.vc.cluster:cis.vc.default:cis.vc.enum:cis.vc.evc:cis.vc.event:cis.vc.eventaux:cis.vc.fault:cis.vc.gos:cis.vc.host:cis.vc.locmsg:cis.vc.option:cis.vc.perf:cis.vc.question:cis.vc.stask:cis.vc.task:cis.vc.vm:cis.vc.profile:cis.vc.hostdiag.enum:cis.vc.hostdiag.eventaux:cis.vc.hostdiag.event:cis.vc.hostdiag.locmsg:cis.vc.VirtualCenter.enum:cis.vc.VirtualCenter.eventaux:cis.vc.VirtualCenter.event:cis.vc.VirtualCenter.extension:cis.vc.VirtualCenter.locmsg\n"
                    + "-------------------------------------------------------\n"
                    + "serviceId=e5b1c3c2-5b96-43ff-bb32-f3e4631a628a\n"
                    + "serviceVersion=6.0\n"
                    + "ownerId=vpxd-03f26b17-5de6-4ba0-a4d3-26aa6fdd2557@vsphere.local\n"
                    + "serviceType.product=com.vmware.cis\n"
                    + "serviceType.type=vcenterserver\n"
                    + "nodeId=79dde47a-045e-11e5-a84c-000c29b4c2e2\n"
                    + "serviceNameResourceKey=AboutInfo.vpx.name\n"
                    + "serviceDescriptionResourceKey=AboutInfo.vpx.name\n"
                    + "attribute0.key=com.vmware.cis.cm.GroupInternalId\n"
                    + "attribute0.value=com.vmware.vim.vcenter\n"
                    + "attribute1.key=com.vmware.cis.cm.ControlScript\n"
                    + "attribute1.value=vmware-vpxd.sh\n"
                    + "attribute2.key=com.vmware.cis.cm.HostId\n"
                    + "attribute2.value=03f26b17-5de6-4ba0-a4d3-26aa6fdd2557\n"
                    + "attribute3.key=com.vmware.vim.vcenter.instanceName\n"
                    + "attribute3.value=vcserver60lnx.vc.eso.lab\n"
                    + "endpoint0.type.protocol=http\n"
                    + "endpoint0.type.id=com.vmware.cis.common.resourcebundle\n"
                    + "endpoint0.url=https://vcserver60lnx.vc.eso.lab:443/catalog/catalog.zip\n"
                    + "endpoint0.ssltrust0=MIIDhTCCAm2gAwIBAgIJANwEGVfPLJjDMA0GCSqGSIb3DQEBCwUAMGsxCzAJBgNVBAMMAkNBMRcwFQYKCZImiZPyLGQBGRYHdnNwaGVyZTEVMBMGCgmSJomT8ixkARkWBWxvY2FsMQswCQYDVQQGEwJVUzEfMB0GA1UECgwWUFNDNjAtTElOVVguVkMuRVNPLkxBQjAeFw0xNTA1MjcxMDUyMzFaFw0yNTA1MjAxMDE3NTdaMDAxITAfBgNVBAMMGHZjc2VydmVyNjBsbngudmMuZXNvLmxhYjELMAkGA1UEBhMCVVMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDCdi2iWqRLodnyRosAGeccJLe1EXRj8aeGaoo9UgvVM6lA4fM+ZNzr2O7ckLINr3Dnd/uSoUVHqHFuAdZ1P1Z6ojvjKEaJhawzBfesmlNbmM1WUu/vKorw0/6CGiwDGCBbrvW8MxOJEbHn3SJlcXCU0PpGerWmEvrzlP8IBeEBrstNaKACrbvXGsMzdFUy4bqvs7IKvxdU79LXmowWpXfmV/fjX5lZOkhVGJNaoORKRrRk/gMAEyPsQnQvZ2E8jZVzpXeTyAKmnTjtE5h5u0oRl/TgyO41otqeoh1szVkneiON819BB7kxs4GlR0pq29XLuidYeP2kufpLqIvYdEk5AgMBAAGjZzBlMCMGA1UdEQQcMBqCGHZjc2VydmVyNjBsbngudmMuZXNvLmxhYjAdBgNVHQ4EFgQUNph90T816gHLqa0xu45H0bX5BXUwHwYDVR0jBBgwFoAUP+hTS65PBbqIQWaoiRWB/PpZWu8wDQYJKoZIhvcNAQELBQADggEBAGgKbIyZfilMvjHrnmP2QkN66oMWvDsKKMsczzrd5rSlnSpA61wR/qV80qsynt31pq/TuHczrF3i0yxcQgh2m5B55dVpscbgjD++eVXzYDadp6RAMlkRNUQZndLn4dAZzxKu+S9yhWgVMfMCug1Au4YrbfyQwHZ5Ew3UTO7gCPAKIzIbf1j4fc0uooWZb0yoIBl/TZ2/O1FxpbDxV4y88JhmrK7rTXIHB0XWqZieAKhuI8ePfFHGwzhhQINIuCIyJ02MheUynqIUqx2fW1OBVupI1T5jaLX+C2f48RwFuguQeKEGoREUVT7k+rZh2ty+yyesuVJZdwg1WqJx7CrbFvw=\n"
                    + "endpoint0.data0.key=com.vmware.cis.common.resourcebundle.basename\n"
                    + "endpoint0.data0.value=cis.vc.action:cis.vc.alarm:cis.vc.auth:cis.vc.cluster:cis.vc.default:cis.vc.enum:cis.vc.evc:cis.vc.event:cis.vc.eventaux:cis.vc.fault:cis.vc.gos:cis.vc.host:cis.vc.locmsg:cis.vc.option:cis.vc.perf:cis.vc.question:cis.vc.stask:cis.vc.task:cis.vc.vm:cis.vc.profile:cis.vc.hostdiag.enum:cis.vc.hostdiag.eventaux:cis.vc.hostdiag.event:cis.vc.hostdiag.locmsg:cis.vc.VirtualCenter.enum:cis.vc.VirtualCenter.eventaux:cis.vc.VirtualCenter.event:cis.vc.VirtualCenter.extension:cis.vc.VirtualCenter.locmsg\n"
                    + "\n";
    }
}
