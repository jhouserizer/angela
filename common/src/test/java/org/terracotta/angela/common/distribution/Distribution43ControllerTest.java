/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.angela.common.distribution;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;

public class Distribution43ControllerTest {

  @Test
  public void parseSystemPropertiesOutputFindsServerPid() {
    Number pid = Distribution43Controller.parseSystemPropertiesOutputAndFindUuid(
        Arrays.asList(
            "1234:",
            "#Fri May 29 21:02:58 CEST 2026",
            "java.vm.name=OpenJDK 64-Bit Server VM",
            "angela.processIdentifier=0e08bc4b-327c-4567-89fd-86e42796a17c"
        ),
        "0e08bc4b-327c-4567-89fd-86e42796a17c"
    );

    assertThat(pid.intValue(), is(1234));
  }

  @Test
  public void parseSystemPropertiesOutputIgnoresUnsupportedCommandOutput() {
    Number pid = Distribution43Controller.parseSystemPropertiesOutputAndFindUuid(
        Arrays.asList(
            "37897:",
            "Error: Error in command",
            "Command VM.system_properties not recognized"
        ),
        "0e08bc4b-327c-4567-89fd-86e42796a17c"
    );

    assertThat(pid, is(nullValue()));
  }

  @Test
  public void parseJcmdListOutputFindsServerPidFromJvmArgument() {
    Number pid = Distribution43Controller.parseJcmdListOutputAndFindUuid(
        Arrays.asList(
            "1111 org.other.Main -Dangela.processIdentifier=0e08bc4b-327c-4567-89fd-86e42796a17c",
            "37897 com.tc.server.TCServerMain -n Server1 -f tc-config.xml -Dangela.processIdentifier=0e08bc4b-327c-4567-89fd-86e42796a17c"
        ),
        "0e08bc4b-327c-4567-89fd-86e42796a17c"
    );

    assertThat(pid.intValue(), is(37897));
  }

  @Test
  public void parseJcmdListOutputIgnoresServerWithDifferentIdentifier() {
    Number pid = Distribution43Controller.parseJcmdListOutputAndFindUuid(
        Arrays.asList(
            "37897 com.tc.server.TCServerMain -n Server1 -f tc-config.xml -Dangela.processIdentifier=other"
        ),
        "0e08bc4b-327c-4567-89fd-86e42796a17c"
    );

    assertThat(pid, is(nullValue()));
  }

  @Test
  public void commandLineMatcherRequiresServerMainAndIdentifier() {
    String serverUuid = "0e08bc4b-327c-4567-89fd-86e42796a17c";

    assertThat(
        Distribution43Controller.isServerCommandLineWithUuid(
            "/path/to/java -Dangela.processIdentifier=" + serverUuid + " com.tc.server.TCServerMain -n Server1",
            serverUuid),
        is(true));
    assertThat(
        Distribution43Controller.isServerCommandLineWithUuid(
            "/path/to/java -Dangela.processIdentifier=" + serverUuid + " org.other.Main",
            serverUuid),
        is(false));
    assertThat(
        Distribution43Controller.isServerCommandLineWithUuid(
            "/path/to/java -Dangela.processIdentifier=other com.tc.server.TCServerMain -n Server1",
            serverUuid),
        is(false));
  }
}
