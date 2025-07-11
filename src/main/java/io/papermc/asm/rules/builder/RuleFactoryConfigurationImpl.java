package io.papermc.asm.rules.builder;

import java.lang.constant.ClassDesc;

record RuleFactoryConfigurationImpl(ClassDesc delegateOwner) implements RuleFactoryConfiguration {
}
