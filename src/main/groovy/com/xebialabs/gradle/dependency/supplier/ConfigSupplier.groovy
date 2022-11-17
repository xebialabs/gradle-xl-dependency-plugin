package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.Config

interface ConfigSupplier {

  Config getConfig(ConfigFileCollector collector)

}
