package io.papermc.asm.rules.builder;

import io.papermc.asm.rules.builder.RuleFactoryConfiguration;
import java.lang.constant.ClassDesc;

record RuleFactoryConfigurationImpl(ClassDesc delegateOwner) implements RuleFactoryConfiguration {
}
