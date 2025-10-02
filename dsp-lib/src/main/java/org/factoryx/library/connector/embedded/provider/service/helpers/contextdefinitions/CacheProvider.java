/*
 * Copyright (c) 2025. Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.factoryx.library.connector.embedded.provider.service.helpers.contextdefinitions;

import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.Json;
import jakarta.json.JsonReader;

import java.io.StringReader;

public class CacheProvider {

    public static JsonDocument getDSP2025Context() {
        JsonReader contextReader = Json.createReader(new StringReader(CacheProvider.CONTEXT));
        return JsonDocument.of(contextReader.readObject());
    }

    public static final String CONTEXT = """
            {
                                                                "@context": {
                                                                  "@version": 1.1,
                                                                  "@protected": true,
                                                                  "xsd": "http://www.w3.org/2001/XMLSchema#",
                                                                  "dct": "http://purl.org/dc/terms/",
                                                                  "dcat": "http://www.w3.org/ns/dcat#",
                                                                  "odrl": "http://www.w3.org/ns/odrl/2/",
                                                                  "dspace": "https://w3id.org/dspace/2025/1/",
                                                                  "DatasetRequestMessage": {
                                                                    "@id": "dspace:DatasetRequestMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "dataset": "dspace:dataset"
                                                                    }
                                                                  },
                                                                  "CatalogRequestMessage": {
                                                                    "@id": "dspace:CatalogRequestMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "filter": {
                                                                        "@id": "dspace:filter",
                                                                        "@container": "@set"
                                                                      }
                                                                    }
                                                                  },
                                                                  "CatalogError": {
                                                                    "@id": "dspace:CatalogError",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "code": "dspace:code",
                                                                      "reason": {
                                                                        "@id": "dspace:reason",
                                                                        "@container": "@set"
                                                                      }
                                                                    }
                                                                  },
                                                                  "ContractRequestMessage": {
                                                                    "@id": "dspace:ContractRequestMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "@import": "https://w3id.org/dspace/2025/1/odrl-profile.jsonld",
                                                                      "@propagate": true,
                                                                      "callbackAddress": "dspace:callbackAddress",
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      },
                                                                      "offer": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:offer"
                                                                      }
                                                                    }
                                                                  },
                                                                  "ContractOfferMessage": {
                                                                    "@id": "dspace:ContractOfferMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "@import": "https://w3id.org/dspace/2025/1/odrl-profile.jsonld",
                                                                      "@propagate": true,
                                                                      "callbackAddress": "dspace:callbackAddress",
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      },
                                                                      "offer": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:offer"
                                                                      }
                                                                    }
                                                                  },
                                                                  "ContractAgreementMessage": {
                                                                    "@id": "dspace:ContractAgreementMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "@import": "https://w3id.org/dspace/2025/1/odrl-profile.jsonld",
                                                                      "@propagate": true,
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      },
                                                                      "agreement": {
                                                                        "@id": "dspace:agreement",
                                                                        "@type": "@id"
                                                                      },
                                                                      "timestamp": "dspace:timestamp"
                                                                    }
                                                                  },
                                                                  "ContractAgreementVerificationMessage": {
                                                                    "@id": "dspace:ContractAgreementVerificationMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      }
                                                                    }
                                                                  },
                                                                  "ContractNegotiationEventMessage": {
                                                                    "@id": "dspace:ContractNegotiationEventMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      },
                                                                      "eventType": {
                                                                        "@type": "@vocab",
                                                                        "@id": "dspace:eventType"
                                                                      }
                                                                    }
                                                                  },
                                                                  "ContractNegotiationTerminationMessage": {
                                                                    "@id": "dspace:ContractNegotiationTerminationMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "code": "dspace:code",
                                                                      "reason": {
                                                                        "@id": "dspace:reason",
                                                                        "@container": "@set"
                                                                      },
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      }
                                                                    }
                                                                  },
                                                                  "ContractNegotiation": {
                                                                    "@id": "dspace:ContractNegotiation",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      },
                                                                      "state": {
                                                                        "@type": "@vocab",
                                                                        "@id": "dspace:state"
                                                                      }
                                                                    }
                                                                  },
                                                                  "ContractNegotiationError": {
                                                                    "@id": "dspace:ContractNegotiationError",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      },
                                                                      "code": "dspace:code",
                                                                      "reason": {
                                                                        "@id": "dspace:reason",
                                                                        "@container": "@set"
                                                                      }
                                                                    }
                                                                  },
                                                                  "TransferRequestMessage": {
                                                                    "@id": "dspace:TransferRequestMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "callbackAddress": "dspace:callbackAddress",
                                                                      "dataAddress": "dspace:dataAddress",
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      },
                                                                      "format": {
                                                                        "@type": "@vocab",
                                                                        "@id": "dct:format"
                                                                      },
                                                                      "agreementId": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:agreementId"
                                                                      }
                                                                    }
                                                                  },
                                                                  "TransferStartMessage": {
                                                                    "@id": "dspace:TransferStartMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      },
                                                                      "dataAddress": "dspace:dataAddress"
                                                                    }
                                                                  },
                                                                  "TransferCompletionMessage": {
                                                                    "@id": "dspace:TransferCompletionMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      }
                                                                    }
                                                                  },
                                                                  "TransferTerminationMessage": {
                                                                    "@id": "dspace:TransferTerminationMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "code": "dspace:code",
                                                                      "reason": {
                                                                        "@id": "dspace:reason",
                                                                        "@container": "@set"
                                                                      },
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      }
                                                                    }
                                                                  },
                                                                  "TransferSuspensionMessage": {
                                                                    "@id": "dspace:TransferSuspensionMessage",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "code": "dspace:code",
                                                                      "reason": {
                                                                        "@id": "dspace:reason",
                                                                        "@container": "@set"
                                                                      },
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      }
                                                                    }
                                                                  },
                                                                  "TransferError": {
                                                                    "@id": "dspace:TransferError",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "code": "dspace:code",
                                                                      "consumerPid": "dspace:consumerPid",
                                                                      "providerPid": "dspace:providerPid",
                                                                      "reason": {
                                                                        "@id": "dspace:reason",
                                                                        "@container": "@set"
                                                                      }
                                                                    }
                                                                  },
                                                                  "DataAddress": {
                                                                    "@id": "dspace:DataAddress",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "endpointType": {
                                                                        "@type": "@vocab",
                                                                        "@id": "dspace:endpointType"
                                                                      },
                                                                      "endpointProperties": {
                                                                        "@id": "dspace:endpointProperties",
                                                                        "@container": "@set"
                                                                      },
                                                                      "endpoint": "dspace:endpoint"
                                                                    }
                                                                  },
                                                                  "EndpointProperty": {
                                                                    "@id": "dspace:EndpointProperty",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "name": "dspace:name",
                                                                      "value": "dspace:value"
                                                                    }
                                                                  },
                                                                  "TransferProcess": {
                                                                    "@id": "dspace:TransferProcess",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "providerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:providerPid"
                                                                      },
                                                                      "consumerPid": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:consumerPid"
                                                                      },
                                                                      "state": {
                                                                        "@type": "@vocab",
                                                                        "@id": "dspace:state"
                                                                      }
                                                                    }
                                                                  },
                                                                  "VersionsError": {
                                                                    "@id": "dspace:VersionsError",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "code": "dspace:code",
                                                                      "reason": {
                                                                        "@id": "dspace:reason",
                                                                        "@container": "@set"
                                                                      }
                                                                    }
                                                                  },
                                                                  "Catalog": {
                                                                    "@id": "dcat:Catalog",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "service": {
                                                                        "@id": "dcat:service",
                                                                        "@container": "@set"
                                                                      },
                                                                      "participantId": {
                                                                        "@type": "@id",
                                                                        "@id": "dspace:participantId"
                                                                      },
                                                                      "catalog": {
                                                                        "@id": "dcat:catalog",
                                                                        "@container": "@set"
                                                                      },
                                                                      "dataset": {
                                                                        "@id": "dcat:dataset",
                                                                        "@container": "@set"
                                                                      },
                                                                      "distribution": {
                                                                        "@id": "dcat:distribution",
                                                                        "@container": "@set"
                                                                      }
                                                                    }
                                                                  },
                                                                  "Dataset": {
                                                                    "@id": "dcat:Dataset",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "@import": "https://w3id.org/dspace/2025/1/odrl-profile.jsonld",
                                                                      "@propagate": true,
                                                                      "distribution": {
                                                                        "@id": "dcat:distribution",
                                                                        "@container": "@set"
                                                                      },
                                                                      "hasPolicy": {
                                                                        "@id": "odrl:hasPolicy",
                                                                        "@container": "@set"
                                                                      }
                                                                    }
                                                                  },
                                                                  "DataService": {
                                                                    "@id": "dcat:DataService",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "endpointDescription": "dcat:endpointDescription",
                                                                      "endpointURL": "dcat:endpointURL"
                                                                    }
                                                                  },
                                                                  "Distribution": {
                                                                    "@id": "dcat:Distribution",
                                                                    "@context": {
                                                                      "@version": 1.1,
                                                                      "@protected": true,
                                                                      "format": {
                                                                        "@type": "@vocab",
                                                                        "@id": "dct:format"
                                                                      },
                                                                      "accessService": {
                                                                        "@id": "dcat:accessService"
                                                                      }
                                                                    }
                                                                  },
                                                                  "CatalogService": {
                                                                    "@id":"dspace:CatalogService",
                                                                    "@context": {
                                                                      "id": "@id",
                                                                      "type": "@type",
                                                                      "serviceEndpoint": {
                                                                        "@id": "https://www.w3.org/ns/did#serviceEndpoint",
                                                                        "@type": "@id"
                                                                      }
                                                                    }
                                                                  },
                                                                  "ACCEPTED": "dspace:ACCEPTED",
                                                                  "FINALIZED": "dspace:FINALIZED",
                                                                  "REQUESTED": "dspace:REQUESTED",
                                                                  "STARTED": "dspace:STARTED",
                                                                  "COMPLETED": "dspace:COMPLETED",
                                                                  "SUSPENDED": "dspace:SUSPENDED",
                                                                  "TERMINATED": "dspace:TERMINATED",
                                                                  "OFFERED": "dspace:OFFERED",
                                                                  "AGREED": "dspace:AGREED",
                                                                  "VERIFIED": "dspace:VERIFIED"
                                                                }
                                                              }
            """;

    // https://w3id.org/dspace/2025/1/odrl-profile.jsonld
    public static JsonDocument getDSP2025OdrlProfile() {
        JsonReader contextReader = Json.createReader(new StringReader(ODRL_PROFILE));
        return JsonDocument.of(contextReader.readObject());
    }
    public static final String ODRL_PROFILE = """
            {
              "@context": {
                "odrl": "http://www.w3.org/ns/odrl/2/",
                "Policy": "odrl:Policy",
                "Rule": "odrl:Rule",
                "profile": {
                  "@type": "@id",
                  "@id": "odrl:profile"
                },
                "prohibit": "odrl:prohibit",
                "Agreement": "odrl:Agreement",
                "Assertion": "odrl:Assertion",
                "Offer": "odrl:Offer",
                "Set": "odrl:Set",
                "Asset": "odrl:Asset",
                "hasPolicy": {
                  "@type": "@id",
                  "@id": "odrl:hasPolicy"
                },
                "target": {
                  "@type": "@id",
                  "@id": "odrl:target"
                },
                "assignee": {
                  "@type": "@id",
                  "@id": "odrl:assignee"
                },
                "assigner": {
                  "@type": "@id",
                  "@id": "odrl:assigner"
                },
                "Action": "odrl:Action",
                "action": {
                  "@type": "@vocab",
                  "@id": "odrl:action"
                },
                "Permission": "odrl:Permission",
                "permission": {
                  "@type": "@id",
                  "@id": "odrl:permission",
                  "@container": "@set"
                },
                "Prohibition": "odrl:Prohibition",
                "prohibition": {
                  "@type": "@id",
                  "@id": "odrl:prohibition",
                  "@container": "@set"
                },
                "obligation": {
                  "@type": "@id",
                  "@id": "odrl:obligation",
                  "@container": "@set"
                },
                "use": "odrl:use",
                "Duty": "odrl:Duty",
                "duty": {
                  "@type": "@id",
                  "@id": "odrl:duty",
                  "@container": "@set"
                },
                "Constraint": "odrl:Constraint",
                "constraint": {
                  "@type": "@id",
                  "@id": "odrl:constraint",
                  "@container": "@set"
                },
                "Operator": "odrl:Operator",
                "operator": {
                  "@type": "@vocab",
                  "@id": "odrl:operator"
                },
                "RightOperand": "odrl:RightOperand",
                "rightOperand": "odrl:rightOperand",
                "LeftOperand": "odrl:LeftOperand",
                "leftOperand": {
                  "@type": "@vocab",
                  "@id": "odrl:leftOperand"
                },
                "eq": "odrl:eq",
                "gt": "odrl:gt",
                "gteq": "odrl:gteq",
                "lt": "odrl:lt",
                "lteq": "odrl:lteq",
                "neq": "odrl:neq",
                "isA": "odrl:isA",
                "hasPart": "odrl:hasPart",
                "isPartOf": "odrl:isPartOf",
                "isAllOf": "odrl:isAllOf",
                "isAnyOf": "odrl:isAnyOf",
                "isNoneOf": "odrl:isNoneOf",
                "or": "odrl:or",
                "xone": "odrl:xone",
                "and": "odrl:and",
                "andSequence": "odrl:andSequence"
              }
            }
            """;

    public static JsonDocument getEDCContext() {
        JsonReader contextReader = Json.createReader(new StringReader(EDC_CONTEXT));
        return JsonDocument.of(contextReader.readObject());
    }

    static final String EDC_CONTEXT = """
            {
              "@context": {
                "@version": 1.1,
                "edc": "https://w3id.org/edc/v0.0.1/ns/",
                "QuerySpec": {
                  "@id": "edc:QuerySpec",
                  "@context": {
                    "sortOrder": "edc:sortOrder",
                    "sortField": "edc:sortField",
                    "offset": "edc:offset",
                    "limit": "edc:limit",
                    "filterExpression": {
                      "@id": "edc:filterExpression",
                      "@container": "@set"
                    }
                  }
                },
                "Criterion": {
                  "@id": "edc:Criterion",
                  "@context": {
                    "operandLeft": "edc:operandLeft",
                    "operator": "edc:operator",
                    "operandRight": "edc:operandRight"
                  }
                },
                "inForceDate": "edc:inForceDate",
                "id": "edc:id",
                "description": "edc:description",
                "isCatalog": "edc:isCatalog"
              }
            }
            """;
}
