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

package org.factoryx.library.connector.embedded.provider.service.deserializers;

import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.*;

public class V_2024_1_Deserializer {

    public static TransferStartMessage deserializeTransferStartMessage(String rawJson) {
        return V_2025_1_Deserializer.deserializeTransferStartMessage(rawJson);
    }

    public static TransferSuspensionMessage deserializeTransferSuspensionMessage(String rawJson) {
        return V_2025_1_Deserializer.deserializeTransferSuspensionMessage(rawJson);
    }

    public static TransferTerminationMessage deserializeTransferTerminationMessage(String rawJson) {
        return V_2025_1_Deserializer.deserializeTransferTerminationMessage(rawJson);
    }

    public static TransferCompletionMessage deserializeTransferCompletionMessage(String rawJson) {
        return V_2025_1_Deserializer.deserializeTransferCompletionMessage(rawJson);
    }

    public static TransferRequestMessage deserializeTransferRequestMessage(String rawJson) {
        return V_08_Deserializer.deserializeTransferRequestMessage(rawJson);
    }

    public static NegotiationTerminationMessage deserializeContractTerminationMessage(String rawJson) {
        return V_08_Deserializer.deserializeContractTerminationMessage(rawJson);
    }

    public static CatalogRequestMessage deserializeCatalogRequest(String json) {
        return V_08_Deserializer.deserializeCatalogRequest(json);
    }

    public static ContractRequestMessage deserializeContractRequest(String json) {
        return V_08_Deserializer.deserializeContractRequest(json);
    }

    public static ContractVerificationMessage deserializeContractVerification(String json) {
        return V_08_Deserializer.deserializeContractVerification(json);
    }
}
