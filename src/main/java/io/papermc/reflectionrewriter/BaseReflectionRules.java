package io.papermc.reflectionrewriter;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import java.lang.constant.ClassDesc;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Set;

public final class BaseReflectionRules {
    private final ClassDesc proxy;
    private final RewriteRule classRule;
    private final RewriteRule methodHandlesLookupRule;
    private final RewriteRule lambdaMetafactoryRule;
    private final RewriteRule constantBootstrapsRule;
    private final RewriteRule methodTypeRule;

    public BaseReflectionRules(final String proxyClassName) {
        this.proxy = ClassDesc.of(proxyClassName);
        this.classRule = this.createClassRule();
        this.methodHandlesLookupRule = this.createMethodHandlesLookupRule();
        this.lambdaMetafactoryRule = this.createLamdaMetafactoryRule();
        this.constantBootstrapsRule = this.createConstantBootstrapsRule();
        this.methodTypeRule = this.createMethodTypeRule();
    }

    public RewriteRule classRule() {
        return this.classRule;
    }

    public RewriteRule methodHandlesLookupRule() {
        return this.methodHandlesLookupRule;
    }

    public RewriteRule lambdaMetafactoryRule() {
        return this.lambdaMetafactoryRule;
    }

    public RewriteRule constantBootstrapsRule() {
        return this.constantBootstrapsRule;
    }

    public RewriteRule methodTypeRule() {
        return this.methodTypeRule;
    }

    public List<RewriteRule> rules() {
        return List.of(
            this.classRule,
            this.methodHandlesLookupRule,
            this.lambdaMetafactoryRule,
            this.constantBootstrapsRule,
            this.methodTypeRule
        );
    }

    private static final MethodMatcher CLASS_RULE = MethodMatcher.builder()
        .match("forName", b -> b.desc("(Ljava/lang/String;)Ljava/lang/Class;"))
        .match(Set.of("getField", "getDeclaredField"), b -> b.desc("(Ljava/lang/String;)Ljava/lang/reflect/Field;"))
        .match(Set.of("getMethod", "getDeclaredMethod"), b -> b.desc("(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"))
        .build();

    private RewriteRule createClassRule() {
        return RewriteRule.forOwnerClass(Class.class, rf -> {
            rf.plainStaticRewrite(this.proxy, CLASS_RULE);
        });
    }

    private static final MethodMatcher METHOD_HANDLE_LOOKUP_RULE = MethodMatcher.builder()
        .match(Set.of("findStatic", "findVirtual"), b -> b.desc("(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
        .match("findClass", b -> b.desc("(Ljava/lang/String;)Ljava/lang/Class;"))
        .match("findSpecial", b -> b.desc("(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"))
        .match(Set.of("findGetter", "findSetter", "findStaticGetter", "findStaticSetter"), b -> b.desc("(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"))
        .match(Set.of("findVarHandle", "findStaticVarHandle"), b -> b.desc("(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;"))
        .match("bind", b -> b.desc("(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
        .build();

    private RewriteRule createMethodHandlesLookupRule() {
        return RewriteRule.forOwnerClass(MethodHandles.Lookup.class, rf -> {
            rf.plainStaticRewrite(this.proxy, METHOD_HANDLE_LOOKUP_RULE);
        });
    }

    private static final MethodMatcher LAMBDA_METAFACTORY_RULE = MethodMatcher.builder()
        .match("metafactory", b -> b.desc("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"))
        .match("altMetafactory", b -> b.desc("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"))
        .build();

    private RewriteRule createLamdaMetafactoryRule() {
        return RewriteRule.forOwnerClass(LambdaMetafactory.class, rf -> {
            rf.plainStaticRewrite(this.proxy, LAMBDA_METAFACTORY_RULE);
        });
    }

    private static final MethodMatcher CONSTANT_BOOTSTRAPS_RULE = MethodMatcher.builder()
        .match("getStaticFinal", b -> b.desc(
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/Object;",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;"
        ))
        .match(Set.of("fieldVarHandle", "staticFieldVarHandle"), b -> b.desc("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;"))
        .build();

    private RewriteRule createConstantBootstrapsRule() {
        return RewriteRule.forOwnerClass(ConstantBootstraps.class, rf -> {
            rf.plainStaticRewrite(this.proxy, CONSTANT_BOOTSTRAPS_RULE);
        });
    }

    private static final MethodMatcher METHOD_TYPE_RULE = MethodMatcher.builder()
        .match("fromMethodDescriptorString", b -> b.desc("(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;"))
        .build();

    private RewriteRule createMethodTypeRule() {
        return RewriteRule.forOwnerClass(MethodType.class, rf -> {
            rf.plainStaticRewrite(this.proxy, METHOD_TYPE_RULE);
        });
    }
}
