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

import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.service.deserializers.service_dtos.*;
import org.springframework.stereotype.Service;

/**
 * The service for de-serializing incoming DSP messages for various versions.
 */
@Service
@Slf4j
public class DeserializerService {

    public TransferStartMessage deserializeTransferStartMessage(String rawJson, DspVersion version) {
        return switch (version) {
            case V_08 -> V_08_Deserializer.deserializeTransferStartMessage(rawJson);
            case V_2025_1 -> V_2025_1_Deserializer.deserializeTransferStartMessage(rawJson);
        };
    }

    public TransferSuspensionMessage deserializeTransferSuspensionMessage(String rawJson, DspVersion version) {
        return switch (version) {
            case V_08 -> V_08_Deserializer.deserializeTransferSuspensionMessage(rawJson);
            case V_2025_1 -> V_2025_1_Deserializer.deserializeTransferSuspensionMessage(rawJson);
        };
    }

    public TransferTerminationMessage deserializeTransferTerminationMessage(String rawJson, DspVersion version) {
        return switch (version) {
            case V_08 -> V_08_Deserializer.deserializeTransferTerminationMessage(rawJson);
            case V_2025_1 -> V_2025_1_Deserializer.deserializeTransferTerminationMessage(rawJson);
        };
    }

    public TransferCompletionMessage deserializeTransferCompletionMessage(String rawJson, DspVersion version) {
        return switch (version) {
            case V_08 -> V_08_Deserializer.deserializeTransferCompletionMessage(rawJson);
            case V_2025_1 -> V_2025_1_Deserializer.deserializeTransferCompletionMessage(rawJson);
        };
    }

    public TransferRequestMessage deserializeTransferRequestMessage(String rawJson, DspVersion version) {
        return switch (version) {
            case V_08 -> V_08_Deserializer.deserializeTransferRequestMessage(rawJson);
            case V_2025_1 -> V_2025_1_Deserializer.deserializeTransferRequestMessage(rawJson);
        };
    }

    public NegotiationTerminationMessage deserializeNegotiationTerminationMessage(String rawJson, DspVersion version) {
        return switch (version) {
            case V_08 -> V_08_Deserializer.deserializeContractTerminationMessage(rawJson);
            case V_2025_1 -> V_2025_1_Deserializer.deserializeContractTerminationMessage(rawJson);
        };
    }

    public CatalogRequestMessage deserializeCatalogRequestMessage(String catalogRequest, DspVersion version) {
        return switch (version) {
            case V_08 -> V_08_Deserializer.deserializeCatalogRequest(catalogRequest);
            case V_2025_1 -> V_2025_1_Deserializer.deserializeCatalogRequest(catalogRequest);
        };
    }

    public ContractRequestMessage deserializeContractRequestMessage(String contractRequest, DspVersion version) {
        return switch (version) {
            case V_08 -> V_08_Deserializer.deserializeContractRequest(contractRequest);
            case V_2025_1 -> V_2025_1_Deserializer.deserializeContractRequest(contractRequest);
        };
    }

    public ContractVerificationMessage deserializeContractVerificationMessage(String contractVerification, DspVersion version) {
        return switch (version) {
            case V_08 -> V_08_Deserializer.deserializeContractVerification(contractVerification);
            case V_2025_1 -> V_2025_1_Deserializer.deserializeContractVerification(contractVerification);
        };
    }
}
