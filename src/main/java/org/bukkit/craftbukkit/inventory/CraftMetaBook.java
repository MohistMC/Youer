package org.bukkit.craftbukkit.inventory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.item.component.WritableBookContent;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.WritableBookMeta;

// Spigot start
import java.util.AbstractList;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import org.bukkit.craftbukkit.util.CraftChatMessage;
// Spigot end

@DelegateDeserialization(SerializableMeta.class)
public class CraftMetaBook extends CraftMetaItem implements BookMeta, WritableBookMeta {
    @ItemMetaKey.Specific(ItemMetaKey.Specific.To.NBT)
    static final ItemMetaKeyType<WritableBookContent> BOOK_CONTENT = new ItemMetaKeyType<>(DataComponents.WRITABLE_BOOK_CONTENT);
    static final ItemMetaKey BOOK_PAGES = new ItemMetaKey("pages");
    static final int MAX_PAGES = Integer.MAX_VALUE; // SPIGOT-6911: Use Minecraft limits
    static final int MAX_PAGE_LENGTH = WritableBookContent.PAGE_EDIT_LENGTH; // SPIGOT-6911: Use Minecraft limits

    // We store the pages in their raw original text representation. See SPIGOT-5063, SPIGOT-5350, SPIGOT-3206
    // For writable books (CraftMetaBook) the pages are stored as plain Strings.
    protected List<String> pages; // null and empty are two different states internally

    CraftMetaBook(CraftMetaItem meta) {
        super(meta);

        if (meta instanceof CraftMetaBook) {
            CraftMetaBook bookMeta = (CraftMetaBook) meta;

            if (bookMeta.pages != null) {
                this.pages = new ArrayList<String>(bookMeta.pages.size());

                pages.addAll(bookMeta.pages);
            }
        } else if (meta instanceof CraftMetaBookSigned) {
            CraftMetaBookSigned bookMeta = (CraftMetaBookSigned) meta;

            if (bookMeta.pages != null) {
                this.pages = new ArrayList<String>(bookMeta.pages.size());

                // Convert from JSON to plain Strings:
                pages.addAll(Lists.transform(bookMeta.pages, CraftChatMessage::fromComponent));
            }
        }
    }

    CraftMetaBook(DataComponentPatch tag) {
        super(tag);

        getOrEmpty(tag, BOOK_CONTENT).ifPresent((writable) -> {
            List<Filterable<String>> pages = writable.pages();
            this.pages = new ArrayList<String>(pages.size());

            // Note: We explicitly check for and truncate oversized books and pages,
            // because they can come directly from clients when handling book edits.
            for (int i = 0; i < Math.min(pages.size(), MAX_PAGES); i++) {
                String page = pages.get(i).raw();
                page = validatePage(page);

                this.pages.add(page);
            }
        });
    }

    CraftMetaBook(Map<String, Object> map) {
        super(map);

        Iterable<?> pages = SerializableMeta.getObject(Iterable.class, map, BOOK_PAGES.BUKKIT, true);
        if (pages != null) {
            this.pages = new ArrayList<String>();
            for (Object page : pages) {
                if (page instanceof String) {
                    internalAddPage(validatePage((String) page));
                }
            }
        }
    }

    @Override
    void applyToItem(CraftMetaItem.Applicator itemData) {
        super.applyToItem(itemData);

        if (pages != null) {
            List<Filterable<String>> list = new ArrayList<>();
            for (String page : pages) {
                list.add(Filterable.from(FilteredText.passThrough(page)));
            }
            itemData.put(BOOK_CONTENT, new WritableBookContent(list));
        }
    }

    @Override
    boolean isEmpty() {
        return super.isEmpty() && isBookEmpty();
    }

    boolean isBookEmpty() {
        return !(pages != null);
    }

    @Override
    boolean applicableTo(Material type) {
        return type == Material.WRITABLE_BOOK;
    }

    @Override
    public boolean hasAuthor() {
        return false;
    }

    @Override
    public boolean hasTitle() {
        return false;
    }

    @Override
    public boolean hasPages() {
        return (pages != null) && !pages.isEmpty();
    }

    @Override
    public boolean hasGeneration() {
        return false;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public boolean setTitle(final String title) {
        return false;
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public void setAuthor(final String author) {
    }

    @Override
    public Generation getGeneration() {
        return null;
    }

    @Override
    public void setGeneration(Generation generation) {
    }

    @Override
    public String getPage(final int page) {
        Preconditions.checkArgument(isValidPage(page), "Invalid page number (%s)", page);
        // assert: pages != null
        return pages.get(page - 1);
    }

    @Override
    public void setPage(final int page, final String text) {
        Preconditions.checkArgument(isValidPage(page), "Invalid page number (%s/%s)", page, getPageCount());
        // assert: pages != null

        String newText = validatePage(text);
        pages.set(page - 1, newText);
    }

    @Override
    public void setPages(final String... pages) {
        setPages(Arrays.asList(pages));
    }

    @Override
    public void addPage(final String... pages) {
        for (String page : pages) {
            page = validatePage(page);
            internalAddPage(page);
        }
    }

    String validatePage(String page) {
        if (page == null) {
            page = "";
        } else if (page.length() > MAX_PAGE_LENGTH) {
            page = page.substring(0, MAX_PAGE_LENGTH);
        }
        return page;
    }

    private void internalAddPage(String page) {
        // asserted: page != null
        if (this.pages == null) {
            this.pages = new ArrayList<String>();
        } else if (this.pages.size() >= MAX_PAGES) {
            return;
        }
        this.pages.add(page);
    }

    @Override
    public int getPageCount() {
        return (pages == null) ? 0 : pages.size();
    }

    @Override
    public List<String> getPages() {
        if (pages == null) return ImmutableList.of();
        return pages.stream().collect(ImmutableList.toImmutableList());
    }

    @Override
    public void setPages(List<String> pages) {
        if (pages.isEmpty()) {
            this.pages = null;
            return;
        }

        if (this.pages != null) {
            this.pages.clear();
        }
        for (String page : pages) {
            addPage(page);
        }
    }

    private boolean isValidPage(int page) {
        return page > 0 && page <= getPageCount();
    }

    @Override
    public CraftMetaBook clone() {
        CraftMetaBook meta = (CraftMetaBook) super.clone();
        if (this.pages != null) {
            meta.pages = new ArrayList<String>(this.pages);
        }
        meta.spigot = meta.new SpigotMeta(); // Spigot
        return meta;
    }

    @Override
    int applyHash() {
        final int original;
        int hash = original = super.applyHash();
        if (this.pages != null) {
            hash = 61 * hash + 17 * this.pages.hashCode();
        }
        return original != hash ? CraftMetaBook.class.hashCode() ^ hash : hash;
    }

    @Override
    boolean equalsCommon(CraftMetaItem meta) {
        if (!super.equalsCommon(meta)) {
            return false;
        }
        if (meta instanceof CraftMetaBook that) {

            return (Objects.equals(this.pages, that.pages));
        }
        return true;
    }

    @Override
    boolean notUncommon(CraftMetaItem meta) {
        return super.notUncommon(meta) && (meta instanceof CraftMetaBook || isBookEmpty());
    }

    @Override
    Builder<String, Object> serialize(Builder<String, Object> builder) {
        super.serialize(builder);

        if (pages != null) {
            builder.put(BOOK_PAGES.BUKKIT, ImmutableList.copyOf(pages));
        }

        return builder;
    }

    // Spigot start
    private BookMeta.Spigot spigot = new SpigotMeta();
    private class SpigotMeta extends BookMeta.Spigot {

        private String pageToJSON(String page) {
            // Convert from plain String to JSON (similar to conversion between writable books and written books):
            Component component = CraftChatMessage.fromString(page, true, true)[0];
            return CraftChatMessage.toJSON(component);
        }

        private String componentsToPage(BaseComponent[] components) {
            // Convert component to plain String:
            Component component = CraftChatMessage.fromJSONOrNull(CraftChatMessage.getBungee().toString(components));
            return CraftChatMessage.fromComponent(component);
        }

        @Override
        public BaseComponent[] getPage(final int page) {
            Preconditions.checkArgument(isValidPage(page), "Invalid page number");
            return CraftChatMessage.getBungee().parse(pageToJSON(pages.get(page - 1)));
        }

        @Override
        public void setPage(final int page, final BaseComponent... text) {
            if (!isValidPage(page)) {
                throw new IllegalArgumentException("Invalid page number " + page + "/" + getPageCount());
            }

            BaseComponent[] newText = text == null ? new BaseComponent[0] : text;
            CraftMetaBook.this.pages.set(page - 1, componentsToPage(newText));
        }

        @Override
        public void setPages(final BaseComponent[]... pages) {
            setPages(Arrays.asList(pages));
        }

        @Override
        public void addPage(final BaseComponent[]... pages) {
            for (BaseComponent[] page : pages) {
                if (page == null) {
                    page = new BaseComponent[0];
                }

                CraftMetaBook.this.internalAddPage(componentsToPage(page));
            }
        }

        @Override
        public List<BaseComponent[]> getPages() {
            if (CraftMetaBook.this.pages == null) return ImmutableList.of();
            final List<String> copy = ImmutableList.copyOf(CraftMetaBook.this.pages);
            return new AbstractList<BaseComponent[]>() {

                @Override
                public BaseComponent[] get(int index) {
                    return CraftChatMessage.getBungee().parse(pageToJSON(copy.get(index)));
                }

                @Override
                public int size() {
                    return copy.size();
                }
            };
        }

        @Override
        public void setPages(List<BaseComponent[]> pages) {
            if (pages.isEmpty()) {
                CraftMetaBook.this.pages = null;
                return;
            }

            if (CraftMetaBook.this.pages != null) {
                CraftMetaBook.this.pages.clear();
            }

            for (BaseComponent[] page : pages) {
                addPage(page);
            }
        }
    };

    @Override
    public BookMeta.Spigot spigot() {
        return spigot;
    }
    // Spigot end
}
