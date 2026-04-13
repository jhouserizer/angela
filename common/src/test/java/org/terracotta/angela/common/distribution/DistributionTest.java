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
import org.terracotta.angela.common.topology.LicenseType;
import org.terracotta.angela.common.topology.PackageType;
import org.terracotta.angela.common.topology.Version;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DistributionTest {
  @Test
  public void testVersion121AndAboveUsesDistribution121Controller() {
    assertControllerType("12.1", Distribution121Controller.class);
    assertControllerType("12.1-SNAPSHOT", Distribution121Controller.class);
    assertControllerType("12.1.0-SNAPSHOT", Distribution121Controller.class);
    assertControllerType("12.1.0", Distribution121Controller.class);
    assertControllerType("12.1.5", Distribution121Controller.class);
    assertControllerType("12.1.5.1.2", Distribution121Controller.class);
    assertControllerType("12.2", Distribution121Controller.class);
    assertControllerType("12.2.0-SNAPSHOT", Distribution121Controller.class);
    assertControllerType("12.5", Distribution121Controller.class);
    assertControllerType("12.10", Distribution121Controller.class);

    assertControllerType("13.0", Distribution121Controller.class);
    assertControllerType("13.0.0-SNAPSHOT", Distribution121Controller.class);
    assertControllerType("13.0.5.1.2", Distribution121Controller.class);
    assertControllerType("15.0", Distribution121Controller.class);

    // inline
    assertInlineControllerType("12.1", Distribution121InlineController.class);
    assertInlineControllerType("12.1.0-SNAPSHOT", Distribution121InlineController.class);
    assertInlineControllerType("12.2", Distribution121InlineController.class);
    assertInlineControllerType("13.0", Distribution121InlineController.class);
  }

  @Test
  public void testVersion107AndAboveUsesDistribution107Controller() {
    assertControllerType("10.7", Distribution107Controller.class);
    assertControllerType("10.7.0-SNAPSHOT", Distribution107Controller.class);
    assertControllerType("10.7.1.1.1", Distribution107Controller.class);

    assertControllerType("11.1.0-SNAPSHOT", Distribution107Controller.class);
    assertControllerType("11.1", Distribution107Controller.class);
    assertControllerType("11.1.1.1.1", Distribution107Controller.class);
    assertControllerType("11.10", Distribution107Controller.class);

    assertControllerType("12.0", Distribution107Controller.class);
    assertControllerType("12.0-SNAPSHOT", Distribution107Controller.class);
    assertControllerType("12.0.0-SNAPSHOT", Distribution107Controller.class);
    assertControllerType("12.0.1", Distribution107Controller.class);
    assertControllerType("12.0.5.1.2", Distribution107Controller.class);

    // inline
    assertInlineControllerType("10.7.0-SNAPSHOT", Distribution107InlineController.class);
    assertInlineControllerType("10.7", Distribution107InlineController.class);
    assertInlineControllerType("10.8", Distribution107InlineController.class);

    assertInlineControllerType("11.0", Distribution107InlineController.class);
    assertInlineControllerType("11.5", Distribution107InlineController.class);

    assertInlineControllerType("12.0", Distribution107InlineController.class);
  }

  @Test
  public void testVersion102To106UsesDistribution102Controller() {
    assertControllerType("10.2.0-SNAPSHOT", Distribution102Controller.class);
    assertControllerType("10.3", Distribution102Controller.class);
    assertControllerType("10.5", Distribution102Controller.class);
    assertControllerType("10.6", Distribution102Controller.class);
  }

  @Test
  public void testVersion511AndAboveUsesDistribution121Controller() {
    assertControllerType("5.11", Distribution121Controller.class);
    assertControllerType("5.11-SNAPSHOT", Distribution121Controller.class);
    assertControllerType("5.11.0", Distribution121Controller.class);
    assertControllerType("5.11.5.1.2", Distribution121Controller.class);
    assertControllerType("5.12", Distribution121Controller.class);
    assertControllerType("5.12.0-SNAPSHOT", Distribution121Controller.class);
    assertControllerType("5.15", Distribution121Controller.class);

    // inline
    assertInlineControllerType("5.11", Distribution121InlineController.class);
    assertInlineControllerType("5.11-SNAPSHOT", Distribution121InlineController.class);
    assertInlineControllerType("5.12", Distribution121InlineController.class);
  }

  @Test
  public void testVersion57To510UsesDistribution107Controller() {
    assertControllerType("5.7", Distribution107Controller.class);
    assertControllerType("5.8", Distribution107Controller.class);
    assertControllerType("5.9", Distribution107Controller.class);
    assertControllerType("5.10", Distribution107Controller.class);
    assertControllerType("5.10-SNAPSHOT", Distribution107Controller.class);
    assertControllerType("5.10.5.1.2", Distribution107Controller.class);

    // inline
    assertInlineControllerType("5.7", Distribution107InlineController.class);
    assertInlineControllerType("5.10", Distribution107InlineController.class);
  }

  @Test
  public void testVersion43AndAboveUsesDistribution43Controller() {
    assertControllerType("4.3-SNAPSHOT", Distribution43Controller.class);
    assertControllerType("4.3.5.1.2", Distribution43Controller.class);
    assertControllerType("4.10", Distribution43Controller.class);
  }

  @Test
  public void testVersion39AndAboveUsesDistribution107Controller() {
    assertControllerType("3.9", Distribution107Controller.class);
    assertControllerType("3.9-SNAPSHOT", Distribution107Controller.class);
    assertControllerType("3.15", Distribution107Controller.class);

    // inline
    assertInlineControllerType("3.9", Distribution107InlineController.class);
    assertInlineControllerType("3.10", Distribution107InlineController.class);
  }

  private void assertControllerType(String versionString, Class<? extends DistributionController> expectedController) {
    Version version = Version.version(versionString);
    Distribution distribution = Distribution.distribution(version, PackageType.KIT, LicenseType.TERRACOTTA_OS);
    Class<? extends DistributionController> actualController = distribution.getDistributionControllerType();
    assertThat(actualController, is(equalTo(expectedController)));
  }

  private void assertInlineControllerType(String versionString, Class<? extends DistributionController> expectedController) {
    Version version = Version.version(versionString);
    Distribution distribution = Distribution.distribution(version, PackageType.KIT, LicenseType.TERRACOTTA_OS, RuntimeOption.INLINE_SERVERS);
    Class<? extends DistributionController> actualController = distribution.getDistributionControllerType();
    assertThat(actualController, is(equalTo(expectedController)));
  }
}
