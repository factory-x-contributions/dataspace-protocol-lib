/*
 * Copyright (c) 2024. Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.factoryx.library.connector.embedded.provider.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
/**
 * A simple health check endpoint
 * @author eschrewe
 */
public class HealthController {

    @GetMapping("${org.factoryx.library.dspapiprefix:/dsp}/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.status(HttpStatus.OK).body("OK");
    }

    @PostMapping("${org.factoryx.library.dspapiprefix:/dsp}/test")
    public ResponseEntity<String> testPost(@RequestBody String body) {
        log.info("/test POST received body: \n{}", body);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("UNSUPPORTED");
    }
}
