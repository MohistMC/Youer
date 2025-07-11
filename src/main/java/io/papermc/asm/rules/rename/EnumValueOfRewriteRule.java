package io.papermc.asm.rules.rename;

import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import io.papermc.asm.rules.generate.GeneratedMethodHolder;
import io.papermc.asm.rules.method.OwnableMethodRewriteRule;
import io.papermc.asm.rules.method.generated.GeneratedStaticRewrite;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

final class EnumValueOfRewriteRule implements GeneratedStaticRewrite, OwnableMethodRewriteRule.Filtered {

    private final Set<ClassDesc> owners = new HashSet<>();
    private final MethodMatcher matcher;
    private final Map<String, String> fieldRenames;

    EnumValueOfRewriteRule(final EnumRenamer renamer) {
        this.owners.add(renamer.typeDesc());
        if (renamer.alternateValueOfOwner() != null) {
            this.owners.add(renamer.alternateValueOfOwner());
        }
        this.matcher = MethodMatcher.builder()
            .match("valueOf", b -> b.statik().desc(MethodTypeDesc.of(renamer.typeDesc(), ConstantDescs.CD_String)))
            .build();
        this.fieldRenames = new TreeMap<>(renamer.fieldRenames());
    }

    @Override
    public void generateMethod(final GeneratorAdapterFactory factory, final MethodCallData modified, final MethodCallData original) {
        final GeneratorAdapter methodGenerator = this.createAdapter(factory, modified);
        GeneratedMethodHolder.loadParameters(methodGenerator, modified.descriptor());
        final int tableSwitchIndexLocal = methodGenerator.newLocal(Type.INT_TYPE);
        methodGenerator.push(-1);
        methodGenerator.storeLocal(tableSwitchIndexLocal);
        methodGenerator.loadArg(0);
        methodGenerator.invokeVirtual(Type.getType(String.class), new Method("hashCode", "()I"));
        final Map<String, Integer> tableSwitchIndexMap = new LinkedHashMap<>();
        final String[] tableSwitchIndexToRenamedField = new String[this.fieldRenames.size()];
        final Map<Integer, List<String>> hashToField = new LinkedHashMap<>();
        int curIdx = 0;
        for (final Map.Entry<String, String> entry : this.fieldRenames.entrySet()) {
            tableSwitchIndexMap.put(entry.getKey(), curIdx);
            tableSwitchIndexToRenamedField[curIdx] = entry.getValue();
            curIdx++;
            hashToField.computeIfAbsent(entry.getKey().hashCode(), k -> new ArrayList<>()).add(entry.getKey());
        }
        final int[] lookupSwitchKeys = hashToField.keySet().stream().mapToInt(Integer::intValue).toArray();
        Arrays.sort(lookupSwitchKeys);
        final Label lookupSwitchEndLabel = methodGenerator.newLabel(); // is also default label in this case
        final Label[] labels = new Label[lookupSwitchKeys.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = methodGenerator.newLabel();
        }
        methodGenerator.visitLookupSwitchInsn(lookupSwitchEndLabel, lookupSwitchKeys, labels);
        for (int i = 0; i < labels.length; i++) {
            methodGenerator.mark(labels[i]);
            // LocalVariableSorter will insert the trailing int local for this and all following visitFrame calls; adding it manually would cause duplicate locals in the frame
            methodGenerator.visitFrame(Opcodes.F_NEW, 1, new Object[]{"java/lang/String"}, 1, new Object[]{"java/lang/String"});
            // generate case
            final List<String> matchingStrings = hashToField.get(lookupSwitchKeys[i]);
            if (matchingStrings.size() == 1) {
                methodGenerator.loadArg(0); // load pass string arg
                methodGenerator.push(matchingStrings.get(0)); // load maybe matching string
                methodGenerator.invokeVirtual(Type.getType(String.class), new Method("equals", "(Ljava/lang/Object;)Z"));
                methodGenerator.visitJumpInsn(Opcodes.IFEQ, lookupSwitchEndLabel);
                methodGenerator.push(tableSwitchIndexMap.get(matchingStrings.get(0)));
                methodGenerator.storeLocal(tableSwitchIndexLocal);
                methodGenerator.goTo(lookupSwitchEndLabel);
            } else {
                final Label[] nestedLabels = new Label[matchingStrings.size()];
                for (int j = 0; j < matchingStrings.size() - 1; j++) {
                    nestedLabels[j] = methodGenerator.newLabel();
                }
                nestedLabels[matchingStrings.size() - 1] = lookupSwitchEndLabel;
                for (int j = 0; j < matchingStrings.size(); j++) {
                    final String maybeMatchingString = matchingStrings.get(j);
                    methodGenerator.loadArg(0); // load pass string arg
                    methodGenerator.push(maybeMatchingString);
                    methodGenerator.invokeVirtual(Type.getType(String.class), new Method("equals", "(Ljava/lang/Object;)Z"));
                    methodGenerator.visitJumpInsn(Opcodes.IFEQ, nestedLabels[j]);
                    methodGenerator.push(tableSwitchIndexMap.get(maybeMatchingString));
                    methodGenerator.storeLocal(tableSwitchIndexLocal);
                    methodGenerator.goTo(lookupSwitchEndLabel);
                    if (nestedLabels[j] != lookupSwitchEndLabel) {
                        methodGenerator.mark(nestedLabels[j]); // mark start of next label (except last one)
                        methodGenerator.visitFrame(Opcodes.F_NEW, 1, new Object[]{"java/lang/String"}, 1, new Object[]{"java/lang/String"});
                    }
                }
            }
        }
        methodGenerator.mark(lookupSwitchEndLabel);
        methodGenerator.visitFrame(Opcodes.F_NEW, 1, new Object[]{"java/lang/String"}, 1, new Object[]{"java/lang/String"});
        methodGenerator.loadLocal(tableSwitchIndexLocal);
        final Label[] tableSwitchLabels = new Label[tableSwitchIndexToRenamedField.length];
        for (int i = 0; i < tableSwitchLabels.length; i++) {
            tableSwitchLabels[i] = methodGenerator.newLabel();
        }
        final Label tableSwitchDefaultLabel = methodGenerator.newLabel();
        final Label tableSwitchEndLabel = methodGenerator.newLabel();
        methodGenerator.visitTableSwitchInsn(0, tableSwitchIndexToRenamedField.length - 1, tableSwitchDefaultLabel, tableSwitchLabels);
        for (int i = 0; i < tableSwitchIndexToRenamedField.length; i++) {
            methodGenerator.mark(tableSwitchLabels[i]);
            methodGenerator.visitFrame(Opcodes.F_NEW, 1, new Object[]{"java/lang/String"}, 1, new Object[]{"java/lang/String"});
            methodGenerator.push(tableSwitchIndexToRenamedField[i]);
            methodGenerator.goTo(tableSwitchEndLabel);
        }
        methodGenerator.mark(tableSwitchDefaultLabel);
        methodGenerator.visitFrame(Opcodes.F_NEW, 1, new Object[]{"java/lang/String"}, 1, new Object[]{"java/lang/String"});
        methodGenerator.loadArg(0); // default to the passed in value
        methodGenerator.mark(tableSwitchEndLabel);
        methodGenerator.visitFrame(Opcodes.F_NEW, 1, new Object[]{"java/lang/String"}, 2, new Object[]{"java/lang/String", "java/lang/String"});
        methodGenerator.invokeStatic(Type.getType(original.owner().descriptorString()), new Method(original.name(), original.descriptor().descriptorString()));
        methodGenerator.returnValue();
        methodGenerator.endMethod();
    }

    @Override
    public void generateConstructor(final GeneratorAdapterFactory factory, final MethodCallData modified, final ConstructorCallData original) {
        throw new UnsupportedOperationException("EnumValueOfRewriteRule does not support constructor generation");
    }

    @Override
    public MethodMatcher methodMatcher() {
        return this.matcher;
    }

    @Override
    public Set<ClassDesc> owners() {
        return this.owners;
    }
}
