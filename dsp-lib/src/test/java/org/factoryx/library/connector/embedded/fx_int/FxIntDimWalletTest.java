package org.factoryx.library.connector.embedded.fx_int;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils;
import org.factoryx.library.connector.embedded.teststarter.SampleDataAssetManagementService;
import org.factoryx.library.connector.embedded.teststarter.TestStarter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.factoryx.library.connector.embedded.provider.service.helpers.JsonUtils.parse;
import static org.junit.jupiter.api.Assertions.*;


@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = TestStarter.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnabledIf(expression = "#{systemProperties['testcontainer.fxint.dim.disable'] == 'false'}")
public class FxIntDimWalletTest {

    private static String env(String key) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) v = System.getenv(key);
        return v;
    }

    static String PROV_DIM_TOKENURL = env("PROVIDER_DIM_TOKEN_URL");
    static String PROV_DIM_CLIENTID = env("PROVIDER_DIM_CLIENT_ID");
    static String PROV_DIM_URL = env("PROVIDER_DIM_URL");
    static String PROV_DID_WEB = env("PROVIDER_DID_WEB");
    static String PROV_DIM_SECRET = env("PROVIDER_DIM_SECRET");

    static String TRUSTED_ISSUER = env("TRUSTED_ISSUER");

    static String CONS_DIM_TOKENURL = env("CONSUMER_DIM_TOKEN_URL");
    static String CONS_DIM_CLIENTID = env("CONSUMER_DIM_CLIENT_ID");
    static String CONS_DIM_URL = env("CONSUMER_DIM_URL");
    static String CONS_DID_WEB = env("CONSUMER_DID_WEB");
    static String CONS_DIM_SECRET = env("CONSUMER_DIM_SECRET");

    static final Network DOCKER_NET = Network.newNetwork();
    final static Logger log = LoggerFactory.getLogger(FxIntDimWalletTest.class);

    private final URI FX_EDC_MANAGEMENT_URL = URI.create("http://localhost:9010/management/v3/");
    private final String TEST_ASSET_ID = "fe015510-1ea7-4169-b904-aa4a119ab837";

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("org.factoryx.library.policyservice", () -> "fxv0_1");
        registry.add("org.factoryx.library.validationservice", () -> "fxv0_1");
        registry.add("org.factoryx.library.validationservice.stsapi", () -> "dim-wallet");
        registry.add("org.factoryx.library.fxv01.vaulturl", () -> "http://localhost:8200");
        registry.add("org.factoryx.library.fxv01.vaultroottoken", () -> "root");
        registry.add("org.factoryx.library.fxv01.vaultsecretalias", () -> "providerdimsecret");
        registry.add("org.factoryx.library.id", () -> PROV_DID_WEB);
        registry.add("org.factoryx.library.fxv01.dimtokenurl", () -> PROV_DIM_TOKENURL);
        registry.add("org.factoryx.library.fxv01.dimclientid", () -> PROV_DIM_CLIENTID);
        registry.add("org.factoryx.library.fxv01.dimurl", () -> PROV_DIM_URL);
        registry.add("org.factoryx.library.fxv01.trustedissuer", () -> TRUSTED_ISSUER);
        registry.add("org.factoryx.library.fxv01.bearer", () -> "false");
    }

    @Autowired
    private SampleDataAssetManagementService sampleDataAssetManagementService;

    @Autowired
    private RestClient restClient;

    @Test
    void doTest() throws Exception {
        log.info("Starting test");
        sampleDataAssetManagementService.addTckDataAsset(TEST_ASSET_ID);

        // fetch catalog from dsp-lib
        URI catalogApi = FX_EDC_MANAGEMENT_URL.resolve("catalog/request");
        String catalogResponse = restClient
                .post()
                .uri(catalogApi)
                .body(catalogRequestBody)
                .header("Content-Type", "application/json")
                .header("x-api-password", "mypw")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, resp) -> log.info("Status: {}", resp.getStatusCode()))
                .body(String.class);
        var catalogObject = parse(catalogResponse);
        var targetDatasetObject = catalogObject.getJsonArray("dcat:dataset").getJsonObject(0);
        String targetAssetId = targetDatasetObject.getString("@id");
        // item in catalog matches expected id
        assertEquals(TEST_ASSET_ID, targetAssetId);

        // generate contract negotiation request based on policy from catalog entry
        var offeredPolicy = targetDatasetObject.getJsonArray("odrl:hasPolicy").getJsonObject(0);
        assertNotNull(offeredPolicy);
        String contractRequestBody = createContractRequest(offeredPolicy);

        URI contractNegotiationApi = FX_EDC_MANAGEMENT_URL.resolve("contractnegotiations/");
        var contractRequestResponse = restClient
                .post()
                .uri(contractNegotiationApi)
                .body(contractRequestBody)
                .header("Content-Type", "application/json")
                .header("x-api-password", "mypw")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, resp) -> log.warn("Status code: {}", resp.getStatusCode()))
                .body(String.class);

        // obtain negotiation id
        var contractResponseObject = parse(contractRequestResponse);
        String negotiationId = contractResponseObject.getString("@id");


        // poll status of negotiation
        URI negotiationStatusApi = contractNegotiationApi.resolve(negotiationId);
        boolean finalized = false;
        String contractId = null;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            var statusResponse = restClient
                    .get()
                    .uri(negotiationStatusApi)
                    .header("x-api-password", "mypw")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, resp) -> log.warn("Status code: {}", resp.getStatusCode()))
                    .body(String.class);
            var statusRequestResponseObject = parse(statusResponse);
            if ("FINALIZED".equalsIgnoreCase(statusRequestResponseObject.getString("state"))) {
                finalized = true;
                contractId = statusRequestResponseObject.getString("contractAgreementId");
                break;
            }
        }
        // contract is finalized
        assertTrue(finalized);
        assertNotNull(contractId);

        // init transfer request
        URI transferProcessApi = FX_EDC_MANAGEMENT_URL.resolve("transferprocesses");
        var transferRequestResponse = restClient
                .post()
                .uri(transferProcessApi)
                .body(createTransferRequest(contractId))
                .header("Content-Type", "application/json")
                .header("x-api-password", "mypw")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, resp) -> log.warn("Status code: {}", resp.getStatusCode()))
                .body(String.class);
        // extract transfer id
        var transferResponseObject = parse(transferRequestResponse);
        var transferId = transferResponseObject.getString("@id");
        assertNotNull(transferId);

        // use transfer id to obtain edr
        URI edrsApi = FX_EDC_MANAGEMENT_URL.resolve("edrs/" + transferId + "/dataaddress");
        String authToken = null, edrEndpoint = null;
        for (int i = 0; i < 10; i++)
            try {
                Thread.sleep(1000);
                var edrResponse = restClient
                        .get()
                        .uri(edrsApi)
                        .header("x-api-password", "mypw")
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (request, resp) -> log.warn("Status code: {}", resp.getStatusCode()))
                        .body(String.class);

                var edrResponseObject = parse(edrResponse);
                authToken = "Bearer " + edrResponseObject.getString("authorization");
                edrEndpoint = edrResponseObject.getString("endpoint").replace("host.docker.internal", "localhost");
                break;
            } catch (Exception e) {
            }

        assertNotNull(authToken);
        assertNotNull(edrEndpoint);

        // use edr to fetch asset
        var assetFetch = restClient
                .get()
                .uri(URI.create(edrEndpoint))
                .header("authorization", authToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, resp) -> log.warn("Status code: {}", resp.getStatusCode()))
                .body(String.class);
        var assetObject = parse(assetFetch);
        String foundAssetId = assetObject.getString("id");
        // transferred asset matches expectation
        assertEquals(TEST_ASSET_ID, foundAssetId);
        log.info("Successfully obtained asset, terminating now");
    }

    private static String escapeDollar(String v) {
        return v.replace("$", "\\$");
    }

    @Container
    final static VaultContainer<?> VAULT_CONTAINER = new VaultContainer<>("hashicorp/vault:1.20")
            .withNetwork(DOCKER_NET)
            .withExposedPorts(8200)
            .withCreateContainerCmdModifier(cmd -> {
                HostConfig hc = cmd.getHostConfig();
                if (hc == null) hc = new HostConfig();
                Ports bindings = new Ports();
                bindings.bind(ExposedPort.tcp(8200), Ports.Binding.bindPort(8200));
                hc.withPortBindings(bindings);
                cmd.withHostConfig(hc);
            })
            .withNetworkAliases("vault")
            .withVaultToken("root")
            .withInitCommand(
                    "secrets enable transit",
                    "write -f transit/keys/my-key",
                    "kv put secret/consumerdimsecret content=" + escapeDollar(CONS_DIM_SECRET),
                    "kv put secret/providerdimsecret content=" + escapeDollar(PROV_DIM_SECRET)
            );

    @Container
    final static PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:16.4-alpine")
            .withNetwork(DOCKER_NET)
            .withNetworkAliases("consumer-postgres-controlplane")
            .withDatabaseName("edc-controlplane")
            .withUsername("admin")
            .withPassword("password");

    @Container
    final static GenericContainer<?> FX_EDC_CONTAINER = new GenericContainer<>("ghcr.io/factory-x-contributions/edc-controlplane-postgresql-hashicorp-vault:0.1.2")
            .withNetwork(DOCKER_NET)
            .withNetworkAliases("consumer-controlplane")
            .withExposedPorts(9000, 9010, 9020, 9030, 9040, 9050, 9060)
            .withExtraHost("host.docker.internal", "host-gateway")
            .withCreateContainerCmdModifier(cmd -> {
                cmd.withEntrypoint("java", "-Dedc.fs.config=/app/configuration.properties", "-jar", "edc-runtime.jar");
                cmd.withCmd();
                HostConfig hc = cmd.getHostConfig();
                if (hc == null) hc = new HostConfig();
                Ports bindings = new Ports();
                bindings.bind(ExposedPort.tcp(9010), Ports.Binding.bindPort(9010));
                bindings.bind(ExposedPort.tcp(9020), Ports.Binding.bindPort(9020));

                bindings.bind(ExposedPort.tcp(9000), Ports.Binding.empty());
                bindings.bind(ExposedPort.tcp(9030), Ports.Binding.empty());
                bindings.bind(ExposedPort.tcp(9040), Ports.Binding.empty());
                bindings.bind(ExposedPort.tcp(9050), Ports.Binding.empty());
                bindings.bind(ExposedPort.tcp(9060), Ports.Binding.empty());
                hc.withPortBindings(bindings);
                cmd.withHostConfig(hc);
            })
            .waitingFor(Wait.forHttp("/api/check/readiness").forPort(9000).forStatusCode(200).withStartupTimeout(Duration.ofSeconds(300)));

    final static Map<String, String> FX_EDC_CONFIG = Map.ofEntries(
            Map.entry("EDC_DSP_CALLBACK_ADDRESS", "http://localhost:9020/protocol"),
            Map.entry("EDC_PARTICIPANT_ID", CONS_DID_WEB),
            Map.entry("EDC_COMPONENT_ID", "consumer-controlplane-component-id"),
            Map.entry("WEB_HTTP_PORT", "9000"),
            Map.entry("WEB_HTTP_PATH", "/api"),
            Map.entry("WEB_HTTP_MANAGEMENT_PORT", "9010"),
            Map.entry("WEB_HTTP_MANAGEMENT_PATH", "/management"),
            Map.entry("WEB_HTTP_PROTOCOL_PORT", "9020"),
            Map.entry("WEB_HTTP_PROTOCOL_PATH", "/protocol"),
            Map.entry("WEB_HTTP_VALIDATION_PORT", "9030"),
            Map.entry("WEB_HTTP_VALIDATION_PATH", "/validation"),
            Map.entry("WEB_HTTP_CONTROL_PORT", "9050"),
            Map.entry("WEB_HTTP_CONTROL_PATH", "/control"),
            Map.entry("EDC_SQL_SCHEMA_AUTOCREATE", "true"),
            Map.entry("EDC_DATASOURCE_DEFAULT_URL", "jdbc:postgresql://consumer-postgres-controlplane:5432/edc-controlplane"),
            Map.entry("EDC_DATASOURCE_DEFAULT_USER", "admin"),
            Map.entry("EDC_DATASOURCE_DEFAULT_PASSWORD", "password"),
            Map.entry("EDC_VAULT_HASHICORP_URL", "http://vault:8200"),
            Map.entry("EDC_VAULT_HASHICORP_HEALTH_CHECK_ENABLED", "false"),
            Map.entry("EDC_VAULT_HASHICORP_TOKEN", "root"),
            Map.entry("EDC_IAM_TRUSTED-ISSUER_0-ISSUER_ID", TRUSTED_ISSUER),
            Map.entry("EDC_IAM_ISSUER_ID", CONS_DID_WEB),
            Map.entry("EDC_IAM_STS_OAUTH_TOKEN_URL", CONS_DIM_TOKENURL),
            Map.entry("EDC_IAM_STS_OAUTH_CLIENT_ID", CONS_DIM_CLIENTID),
            Map.entry("EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS", "consumerdimsecret"),
            Map.entry("TX_EDC_IAM_STS_DIM_URL", CONS_DIM_URL),
            Map.entry("WEB_HTTP_MANAGEMENT_AUTH_KEY", "mypw"),
            Map.entry("EDC_DCP_V08_FORCED", "true")
    );

    static String catalogRequestBody = """
            {
                "@context": {
                    "edc": "https://w3id.org/edc/v0.0.1/ns/"
                },
                "@type": "CatalogRequest",
                "protocol": "dataspace-protocol-http",
                "counterPartyAddress": "http://host.docker.internal:8080/dsp",
                "counterPartyId": "REPLACE_ME"
            }
            """;

    final static String CONTRACT_REQUEST_TEMPLATE = """
            {
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                "odrl": "http://www.w3.org/ns/odrl/2/"
              },
              "@type": "ContractRequest",
              "counterPartyAddress": "http://host.docker.internal:8080/dsp",
              "protocol": "dataspace-protocol-http"
            }
            """;

    static String createContractRequest(JsonObject offeredPolicy) {
        var requestBuilder = Json.createObjectBuilder(JsonUtils.parse(CONTRACT_REQUEST_TEMPLATE));
        var policy = Json.createObjectBuilder()
                .add("@type", "odrl:Offer")
                .add("@id", "my-test-offer")
                .add("odrl:target", offeredPolicy.getJsonObject("odrl:target"))
                .add("odrl:assigner", Json.createObjectBuilder().add("@id", offeredPolicy.getJsonString("odrl:assigner")))
                .add("odrl:assignee", Json.createObjectBuilder().add("@id", offeredPolicy.getJsonString("odrl:assignee")))
                .add("odrl:permission", Json.createArrayBuilder().add(offeredPolicy.getJsonObject("odrl:permission")))
                .build();
        requestBuilder.add("policy", policy);
        requestBuilder.add("connectorId", PROV_DID_WEB);
        return requestBuilder.build().toString();
    }

    static String TRANSFER_REQUEST_TEMPLATE = """
            {
                "@context": {
                    "edc": "https://w3id.org/edc/v0.0.1/ns/"
                },
                "@type": "TransferRequestDto",
                "protocol": "dataspace-protocol-http",
                "counterPartyAddress": "http://host.docker.internal:8080/dsp",
                "transferType": "HttpData-PULL"
            }
            """;

    static String createTransferRequest(String contractAgreementId) {
        var requestBuilder = Json.createObjectBuilder(JsonUtils.parse(TRANSFER_REQUEST_TEMPLATE));
        requestBuilder.add("contractId", contractAgreementId);
        requestBuilder.add("connectorId", PROV_DID_WEB);
        return requestBuilder.build().toString();
    }

    static {
        FX_EDC_CONTAINER.dependsOn(VAULT_CONTAINER);
        FX_EDC_CONTAINER.dependsOn(POSTGRES_CONTAINER);
        FX_EDC_CONTAINER.withEnv(FX_EDC_CONFIG);
        catalogRequestBody = catalogRequestBody.replace("REPLACE_ME", PROV_DID_WEB);
    }
}
