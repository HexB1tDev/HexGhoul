// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.nbt;

/** Real hierarchy: ListTag -> CollectionTag<Tag> -> AbstractList<Tag>; Tag. */
public class ListTag extends java.util.AbstractList<Tag> implements Tag {
    public ListTag() {
    }
    @Override public Tag get(int index) { throw new UnsupportedOperationException(); }
    @Override public int size() { throw new UnsupportedOperationException(); }
    /** getCompound(index) */ public CompoundTag m_128728_(int index) { throw new UnsupportedOperationException(); }
}
