package ca.spottedleaf.dataconverter.util;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.custom.V3818_Commands;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.Pair;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.commands.ReturnCommand;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CommandArgumentUpgrader {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final CommandBuildContext context;
    private final CommandSourceStack source;
    private final Map<Class<?>, BiFunction<ArgumentType<?>, CommandBuildContext, ArgumentType<?>>> replacements;

    public static CommandArgumentUpgrader upgrader_1_20_4_to_1_20_5(final int functionPermissionLevel) {
        return new CommandArgumentUpgrader(functionPermissionLevel, builder -> {
            builder.registerReplacement(ItemArgument.class, (argument, ctx) -> new ItemParser_1_20_4());
            builder.registerReplacement(ComponentArgument.class, (argument, ctx) -> new ComponentParser_1_20_4());
            builder.registerExtraCommand(CommandArgumentUpgrader::registerSummon_1_20_4_to_1_20_5);
        });
    }

    public CommandArgumentUpgrader(
        final int functionPermissionLevel,
        final Consumer<ReplacementsBuilder> consumer
    ) {
        this(
            new Commands(Commands.CommandSelection.DEDICATED, makeDummyCommandBuildContext()).getDispatcher(),
            functionPermissionLevel,
            consumer
        );
    }

    private CommandArgumentUpgrader(
        final CommandDispatcher<CommandSourceStack> dispatcher,
        final int functionPermissionLevel,
        final Consumer<ReplacementsBuilder> consumer
    ) {
        final ReplacementsBuilder builder = new ReplacementsBuilder();
        consumer.accept(builder);
        this.replacements = Map.copyOf(builder.replacements);

        final CommandBuildContext context = makeDummyCommandBuildContext();
        this.dispatcher = new CommandDispatcher<>();
        this.context = context;
        final List<CommandNode<CommandSourceStack>> aliases = new ArrayList<>();
        for (final CommandNode<CommandSourceStack> child : dispatcher.getRoot().getChildren()) {
            final CopyResult result = this.copyCommand(this.dispatcher.getRoot(), child, null);
            if (result.replaced()) {
                this.dispatcher.getRoot().addChild(result.root);
            }
            aliases.addAll(result.aliases);
        }
        aliases.forEach(redirectNode -> {
            final CommandNode<CommandSourceStack> toNode = this.dispatcher.getRoot()
                .getChild(redirectNode.getRedirect().getName());
            if (toNode != null) {
                this.dispatcher.getRoot().addChild(
                    new LiteralCommandNode<>(
                        redirectNode.getName(),
                        null,
                        toNode.getRequirement(),
                        toNode,
                        redirectNode.getRedirectModifier(),
                        redirectNode.isFork()
                    )
                );
            }
        });
        for (final Consumer<CommandDispatcher<CommandSourceStack>> extra : builder.extra) {
            extra.accept(this.dispatcher);
        }
        ExecuteCommand.register(this.dispatcher, context);
        ReturnCommand.register(this.dispatcher);
        // This looks weird, but it's what vanilla does when loading functions for datapacks
        this.source = new CommandSourceStack(
            CommandSource.NULL,
            Vec3.ZERO,
            Vec2.ZERO,
            null,
            functionPermissionLevel,
            "",
            CommonComponents.EMPTY,
            null,
            null
        );
    }

    public static final class ReplacementsBuilder {
        private final Map<Class<?>, BiFunction<ArgumentType<?>, CommandBuildContext, ArgumentType<?>>> replacements =
            new HashMap<>();
        private final List<Consumer<CommandDispatcher<CommandSourceStack>>> extra = new ArrayList<>();

        private ReplacementsBuilder() {
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public <A extends ArgumentType<?>> void registerReplacement(
            final Class<A> type,
            final BiFunction<A, CommandBuildContext, ? extends ArgumentType<UpgradedArgument>> upgrader
        ) {
            this.replacements.put(type, (BiFunction) upgrader);
        }

        public void registerExtraCommand(final Consumer<CommandDispatcher<CommandSourceStack>> consumer) {
            this.extra.add(consumer);
        }
    }

    public interface UpgradableArgument {
        String upgrade(int index, List<Pair<String, ParsedArgument<CommandSourceStack, ?>>> arguments);
    }

    public record UpgradedArgument(String upgraded) {}

    private static final class ItemParser_1_20_4 implements ArgumentType<UpgradedArgument> {
        @Override
        public UpgradedArgument parse(final StringReader reader) throws CommandSyntaxException {
            final ResourceLocation id = ResourceLocation.read(reader);

            final CompoundTag itemNBT = new CompoundTag();
            itemNBT.putString("id", id.toString());
            itemNBT.putInt("Count", 1);

            if (reader.canRead() && reader.peek() == '{') {
                itemNBT.put("tag", new TagParser(reader).readStruct());
            }

            final CompoundTag converted = MCDataConverter.convertTag(
                MCTypeRegistry.ITEM_STACK, itemNBT, MCVersions.V1_20_4, SharedConstants.getCurrentVersion().getDataVersion().getVersion()
            );

            final String newId = converted.getString("id");

            if (converted.contains("components", Tag.TAG_COMPOUND)) {
                return new UpgradedArgument(newId + V3818_Commands.toCommandFormat(converted.getCompound("components")));
            } else {
                return new UpgradedArgument(newId);
            }
        }
    }

    private static final class ComponentParser_1_20_4 implements ArgumentType<UpgradedArgument> {
        private static final Field JSON_READER_POS = Util.make(() -> {
            try {
                final Field field = JsonReader.class.getDeclaredField("pos");
                field.setAccessible(true);
                return field;
            } catch (final NoSuchFieldException var1) {
                throw new IllegalStateException("Couldn't get field 'pos' for JsonReader", var1);
            }
        });

        private static final Field JSON_READER_LINESTART = Util.make(() -> {
            try {
                final Field field = JsonReader.class.getDeclaredField("lineStart");
                field.setAccessible(true);
                return field;
            } catch (final NoSuchFieldException var1) {
                throw new IllegalStateException("Couldn't get field 'lineStart' for JsonReader", var1);
            }
        });

        @Override
        public UpgradedArgument parse(final StringReader reader) throws CommandSyntaxException {
            final JsonElement element;
            try {
                element = parseJson(reader);
            } catch (final Exception e) {
                throw new SimpleCommandExceptionType(new LiteralMessage(e.getMessage())).createWithContext(reader);
            }
            V3818_Commands.walkComponent(element);
            return new UpgradedArgument(GsonHelper.toStableString(element));
        }

        public static JsonElement parseJson(final StringReader stringReader) {
            final JsonReader jsonReader = new JsonReader(new java.io.StringReader(stringReader.getRemaining()));
            jsonReader.setLenient(false);

            final JsonElement jsonElement;
            try {
                jsonElement = Streams.parse(jsonReader);
            } catch (final StackOverflowError var9) {
                throw new JsonParseException(var9);
            } finally {
                stringReader.setCursor(stringReader.getCursor() + getPos(jsonReader));
            }
            return jsonElement;
        }

        private static int getPos(final JsonReader jsonReader) {
            try {
                return JSON_READER_POS.getInt(jsonReader) - JSON_READER_LINESTART.getInt(jsonReader);
            } catch (IllegalAccessException var2) {
                throw new IllegalStateException("Couldn't read position of JsonReader", var2);
            }
        }
    }

    // important: leadingSlash should not just be the result of a startsWith on command,
    // it should reflect whether the command use is in a place that will skip a leading slash when parsing
    public String upgradeCommandArguments(final String command, final boolean leadingSlash) {
        final StringReader reader = new StringReader(command);
        if (leadingSlash && reader.peek() == '/') {
            reader.skip();
        }
        final ParseResults<CommandSourceStack> parseResult = this.dispatcher.parse(reader, this.source);
        if (!parseResult.getExceptions().isEmpty()) {
            return command;
        }
        final Map<StringRange, String> replacements = new LinkedHashMap<>();
        final List<Pair<String, ParsedArgument<CommandSourceStack, ?>>> mergedArguments = new ArrayList<>();
        addArguments(mergedArguments, parseResult.getContext());
        for (int i = 0; i < mergedArguments.size(); i++) {
            final Pair<String, ParsedArgument<CommandSourceStack, ?>> pair = mergedArguments.get(i);
            if (pair.value().getResult() instanceof UpgradedArgument upgraded) {
                replacements.put(pair.value().getRange(), upgraded.upgraded());
            } else if (pair.value().getResult() instanceof UpgradableArgument upgradable) {
                replacements.put(pair.value().getRange(), upgradable.upgrade(i, mergedArguments));
            }
        }
        String upgradedCommand = command;
        while (!replacements.isEmpty()) {
            final Map.Entry<StringRange, String> next = replacements.entrySet().iterator().next();
            replacements.remove(next.getKey());
            upgradedCommand = upgradedCommand.substring(0, next.getKey().getStart()) + next.getValue() + upgradedCommand.substring(next.getKey().getEnd());
            // Update the offsets for the remaining replacements
            final int diff = next.getValue().length() - next.getKey().getLength();
            final Map<StringRange, String> replacementsCopy = new LinkedHashMap<>(replacements);
            replacements.clear();
            replacementsCopy.forEach((range, value) -> {
                replacements.put(new StringRange(range.getStart() + diff, range.getEnd() + diff), value);
            });
        }
        return upgradedCommand;
    }

    public String upgradeSingleArgument(
        final Function<CommandBuildContext, ? extends ArgumentType<?>> argumentFactory,
        final String input
    ) {
        final ArgumentType<?> argument = argumentFactory.apply(this.context);
        final ArgumentType<?> replaced = this.replaceArgumentType(this.context, argument);
        if (argument == replaced) {
            return input;
        }
        try {
            final UpgradedArgument parsed = (UpgradedArgument) replaced.parse(new StringReader(input));
            return parsed.upgraded();
        } catch (final CommandSyntaxException e) {
            return input;
        }
    }

    private static void addArguments(
        final List<Pair<String, ParsedArgument<CommandSourceStack, ?>>> mergedArguments,
        final @Nullable CommandContextBuilder<CommandSourceStack> context
    ) {
        if (context == null) {
            return;
        }
        context.getArguments().forEach((name, argument) -> mergedArguments.add(Pair.of(name, argument)));
        addArguments(mergedArguments, context.getChild());
    }

    private ArgumentType<?> replaceArgumentType(final CommandBuildContext ctx, final ArgumentType<?> type) {
        final BiFunction<ArgumentType<?>, CommandBuildContext, ArgumentType<?>> upgrader =
            this.replacements.get(type.getClass());
        if (upgrader != null) {
            return upgrader.apply(type, ctx);
        }
        return type;
    }

    record CopyResult(
        CommandNode<CommandSourceStack> root,
        boolean replaced,
        List<CommandNode<CommandSourceStack>> aliases
    ) {
        CopyResult replacedResult() {
            if (this.replaced) {
                return this;
            }
            return new CopyResult(this.root, true, new ArrayList<>(this.aliases));
        }
    }

    private CopyResult copyCommand(
        final CommandNode<CommandSourceStack> parent,
        final CommandNode<CommandSourceStack> node,
        @Nullable CopyResult result
    ) {
        final CommandNode<CommandSourceStack> copy;
        final boolean replaced;
        if (node instanceof LiteralCommandNode<?> literal) {
            if (node.getName().equals("execute") || node.getName().equals("return")) {
                return new CopyResult(parent, false, new ArrayList<>());
            }
            if (node.getRedirect() != null) {
                if (result != null) {
                    throw new IllegalStateException("Cannot handle non-root redirects");
                }
                final List<CommandNode<CommandSourceStack>> aliases = new ArrayList<>();
                aliases.add(node);
                return new CopyResult(parent, false, aliases);
            }
            copy = new LiteralCommandNode<>(
                literal.getLiteral(),
                node.getCommand(),
                node.getRequirement(),
                null,
                node.getRedirectModifier(),
                node.isFork()
            );
            replaced = false;
        } else if (node instanceof ArgumentCommandNode<?, ?>) {
            final ArgumentCommandNode<CommandSourceStack, ?> argument =
                (ArgumentCommandNode<CommandSourceStack, ?>) node;
            final ArgumentType<?> replacedType = this.replaceArgumentType(this.context, argument.getType());
            replaced = replacedType != argument.getType();
            copy = new ArgumentCommandNode<>(
                node.getName(),
                replacedType,
                node.getCommand(),
                node.getRequirement(),
                null,
                node.getRedirectModifier(),
                node.isFork(),
                argument.getCustomSuggestions()
            );
        } else {
            throw new UnsupportedOperationException();
        }
        if (result == null) {
            result = new CopyResult(copy, false, new ArrayList<>());
        }
        if (replaced) {
            result = result.replacedResult();
        }
        for (final CommandNode<CommandSourceStack> child : node.getChildren()) {
            result = this.copyCommand(copy, child, result);
        }
        if (parent != this.dispatcher.getRoot()) {
            parent.addChild(copy);
        }
        return result;
    }

    public static void registerSummon_1_20_4_to_1_20_5(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("summon")
                .then(Commands.argument("entity", ResourceLocationArgument.id())
                    .executes(commandContext -> Command.SINGLE_SUCCESS)
                    .then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(commandContext -> Command.SINGLE_SUCCESS)
                        .then(Commands.argument("nbt", new ArgumentType<UpgradableArgument>() {
                                @Override
                                public UpgradableArgument parse(final StringReader reader) throws CommandSyntaxException {
                                    final CompoundTag tag = CompoundTagArgument.compoundTag().parse(reader);

                                    return (index, args) -> {
                                        final CompoundTag tagCopy = tag.copy();

                                        final Pair<String, ParsedArgument<CommandSourceStack, ?>> entityTypePair =
                                            args.get(index - 2);
                                        final ResourceLocation entityType =
                                            (ResourceLocation) entityTypePair.value().getResult();

                                        tagCopy.putString("id", entityType.toString());
                                        final CompoundTag convertedTag = MCDataConverter.convertTag(
                                            MCTypeRegistry.ENTITY,
                                            tagCopy,
                                            MCVersions.V1_20_4, SharedConstants.getCurrentVersion().getDataVersion().getVersion()
                                        );
                                        convertedTag.remove("id");

                                        return convertedTag.toString();
                                    };
                                }
                            })
                            .executes(commandContext -> Command.SINGLE_SUCCESS))))
        );
    }

    private static CommandBuildContext makeDummyCommandBuildContext() {
        return Commands.createValidationContext(
            new HolderLookup.Provider() {

                @Override
                public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                    return Stream.of();
                }

                @Override
                public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(
                    final ResourceKey<? extends Registry<? extends T>> registryRef
                ) {
                    return Optional.of(new HolderLookup.RegistryLookup<T>() {
                        @Override
                        public ResourceKey<? extends Registry<? extends T>> key() {
                            return registryRef;
                        }

                        @Override
                        public Lifecycle registryLifecycle() {
                            return Lifecycle.stable();
                        }

                        @Override
                        public Stream<Holder.Reference<T>> listElements() {
                            return Stream.of();
                        }

                        @Override
                        public Stream<HolderSet.Named<T>> listTags() {
                            return Stream.of();
                        }

                        @Override
                        public Optional<Holder.Reference<T>> get(final ResourceKey<T> key) {
                            return Optional.of(Holder.Reference.createStandAlone(this, key));
                        }

                        @Override
                        public Optional<HolderSet.Named<T>> get(final TagKey<T> tag) {
                            return Optional.of(HolderSet.emptyNamed(this, tag));
                        }
                    });
                }
            }
        );
    }
}
