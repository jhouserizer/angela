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
package org.terracotta.angela.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.common.util.JDK;
import org.terracotta.angela.common.util.JavaLocationResolver;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static org.terracotta.angela.common.AngelaProperties.JAVA_HOME;
import static org.terracotta.angela.common.AngelaProperties.JAVA_OPTS;
import static org.terracotta.angela.common.AngelaProperties.JAVA_RESOLVER;
import static org.terracotta.angela.common.AngelaProperties.JAVA_VENDOR;
import static org.terracotta.angela.common.AngelaProperties.JAVA_VERSION;

/**
 * Instances of this class are immutable.
 * <p>
 * WARNING
 * <p>
 * This object goes sadly through a lot of ignite calls...
 * This is really error-prone because it represents a specific env for one host.
 * Only the Java args are relevant to be transferred remotely to spawn a new Ignite agent.
 */
public class TerracottaCommandLineEnvironment implements Serializable {
  private static final long serialVersionUID = 1L;

  private final static Logger LOGGER = LoggerFactory.getLogger(TerracottaCommandLineEnvironment.class);

  public static final TerracottaCommandLineEnvironment DEFAULT;

  static {
    switch (JAVA_RESOLVER.getValue()) {
      case "toolchain": {
        String version = JAVA_VERSION.getValue();
        // Important - Use a LinkedHashSet to preserve the order of preferred Java vendor
        Set<String> vendors = JAVA_VENDOR.getValue().equals("") ? new LinkedHashSet<>() : singleton(JAVA_VENDOR.getValue());
        // Important - Use a LinkedHashSet to preserve the order of opts, as some opts are position-sensitive
        Set<String> opts = JAVA_OPTS.getValue().equals("") ? new LinkedHashSet<>() : singleton(JAVA_OPTS.getValue());
        DEFAULT = new TerracottaCommandLineEnvironment(false, version, vendors, opts);
        break;
      }
      case "user": {
        Set<String> opts = JAVA_OPTS.getValue().equals("") ? new LinkedHashSet<>() : singleton(JAVA_OPTS.getValue());
        DEFAULT = new TerracottaCommandLineEnvironment(true, "", emptySet(), opts);
        break;
      }
      default:
        throw new AssertionError("Unsupported value for '" + JAVA_RESOLVER.getPropertyName() + "': " + JAVA_RESOLVER.getValue());
    }
  }

  private final boolean useJavaHome;
  private final String javaVersion;
  private final Set<String> javaVendors;
  private final Set<String> javaOpts;

  /**
   * Create a new instance that contains whatever is necessary to build a JVM command line, minus classpath and main class.
   *
   * @param javaVersion the java version specified in toolchains.xml, can be empty if any version will fit.
   * @param javaVendors a set of acceptable java vendors specified in toolchains.xml, can be empty if any vendor will fit.
   * @param javaOpts    some command line arguments to give to the JVM, like -Xmx2G, -XX:Whatever or -Dsomething=abc.
   *                    Can be empty if no JVM argument is needed.
   */
  private TerracottaCommandLineEnvironment(boolean useJavaHome, String javaVersion, Set<String> javaVendors, Set<String> javaOpts) {
    validate(javaVersion, javaVendors, javaOpts);
    this.useJavaHome = useJavaHome;
    this.javaVersion = javaVersion;
    this.javaVendors = unmodifiableSet(new LinkedHashSet<>(javaVendors));
    this.javaOpts = unmodifiableSet(new LinkedHashSet<>(javaOpts));
  }

  private static void validate(String javaVersion, Set<String> javaVendors, Set<String> javaOpts) {
    requireNonNull(javaVersion);
    requireNonNull(javaVendors);
    requireNonNull(javaOpts);

    if (javaVendors.stream().anyMatch(vendor -> vendor == null || vendor.isEmpty())) {
      throw new IllegalArgumentException("None of the java vendors can be null or empty");
    }

    if (javaOpts.stream().anyMatch(opt -> opt == null || opt.isEmpty())) {
      throw new IllegalArgumentException("None of the java opts can be null or empty");
    }
  }

  public boolean isToolchainBased() {
    return !useJavaHome;
  }

  public TerracottaCommandLineEnvironment withJavaVersion(String javaVersion) {
    return new TerracottaCommandLineEnvironment(false, javaVersion, javaVendors, javaOpts);
  }

  public TerracottaCommandLineEnvironment withJavaVendors(String... javaVendors) {
    return new TerracottaCommandLineEnvironment(false, javaVersion, new LinkedHashSet<>(asList(javaVendors)), javaOpts);
  }

  public TerracottaCommandLineEnvironment withJavaOpts(String... javaOpts) {
    return new TerracottaCommandLineEnvironment(useJavaHome, javaVersion, javaVendors, new LinkedHashSet<>(asList(javaOpts)));
  }

  public TerracottaCommandLineEnvironment withCurrentJavaHome() {
    return new TerracottaCommandLineEnvironment(true, "", emptySet(), javaOpts);
  }

  /**
   * @deprecated Use {@link #withCurrentJavaHome()} instead.
   */
  @Deprecated
  public TerracottaCommandLineEnvironment withJavaHome(Path home) {
    return withCurrentJavaHome();
  }

  public Path getJavaHome() {
    if (useJavaHome) {
      return Paths.get(JAVA_HOME.getValue());
    } else {
      List<JDK> jdks = new JavaLocationResolver().resolveJavaLocations(getJavaVersion(), getJavaVendors(), true);
      if (jdks.size() > 1) {
        LOGGER.warn("Multiple matching java versions found: {} - using the 1st one", jdks);
      }
      return jdks.get(0).getHome().toLocalPath();
    }
  }

  public String getJavaVersion() {
    return javaVersion;
  }

  public Set<String> getJavaVendors() {
    return javaVendors;
  }

  public Set<String> getJavaOpts() {
    return javaOpts;
  }

  public Map<String, String> buildEnv(Map<String, String> overrides) {
    LOGGER.debug("overrides={}", overrides);

    Map<String, String> env = new HashMap<>();
    Path javaHome = getJavaHome();
    env.put("JAVA_HOME", javaHome.toString());

    Set<String> javaOpts = getJavaOpts();
    if (!javaOpts.isEmpty()) {
      String joinedJavaOpts = String.join(" ", javaOpts);
      env.put("JAVA_OPTS", joinedJavaOpts);
    }

    for (Map.Entry<String, String> entry : overrides.entrySet()) {
      if (entry.getValue() == null) {
        env.remove(entry.getKey()); // ability to clear an existing entry
      } else {
        env.put(entry.getKey(), entry.getValue());
      }
    }

    Stream.of("JAVA_HOME", "JAVA_OPTS").forEach(key -> LOGGER.debug(" {} = {}", key, env.get(key)));

    return env;
  }

  @Override
  public String toString() {
    return "TerracottaCommandLineEnvironment{" +
        "javaVersion='" + javaVersion + '\'' +
        ", javaVendors=" + javaVendors +
        ", javaOpts=" + javaOpts +
        '}';
  }
}
