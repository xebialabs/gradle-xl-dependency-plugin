package com.xebialabs.gradle.dependency.supplier

import com.typesafe.config.Config
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType
import com.xebialabs.gradle.dependency.DependencyManagementContainer
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

interface ConfigSupplier {

  Config getConfig()
}
