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

package org.factoryx.library.connector.embedded.jpa;

import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepository;
import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepositoryFactory;
import org.factoryx.library.connector.embedded.repository.JpaNegotiationRecordRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaNegotiationRecordRepositoryFactory implements NegotiationRecordRepositoryFactory {

    private final JpaNegotiationRecordRepository repository;

    public JpaNegotiationRecordRepositoryFactory(JpaNegotiationRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public NegotiationRecordRepository getRepository() {
        return repository;
    }
}
