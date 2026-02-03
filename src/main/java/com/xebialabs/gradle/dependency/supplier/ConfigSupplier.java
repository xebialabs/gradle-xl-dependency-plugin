package com.xebialabs.gradle.dependency.supplier;

import com.typesafe.config.Config;

public interface ConfigSupplier {

    Config getConfig(ConfigFileCollector collector);

}
