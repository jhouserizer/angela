/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.angela.agent.kit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.terracotta.angela.agent.kit.LocalKitManager.INSTALLATION_LOCK_FILE_NAME;

/**
 * @author Aurelien Broszniowski
 */

public class LocalKitManagerTest {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  @Test
  public void testLock() throws InterruptedException {
    final File file = Paths.get(".").resolve(INSTALLATION_LOCK_FILE_NAME).toFile();

    System.out.println(file.getAbsolutePath());
    file.delete();

    final LocalKitManager localKitManager = new LocalKitManager(null, null);

    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      final Thread thread = new Thread(() -> {
        localKitManager.lockConcurrentInstall(Paths.get("."));
        localKitManager.unlockConcurrentInstall(Paths.get("."));
      });
      thread.start();
      threads.add(thread);
    }

    for (Thread thread : threads) {
      thread.join();
    }

    assertThat(file.exists(), equalTo(false));
  }


}
