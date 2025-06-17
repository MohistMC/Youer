package io.papermc.asm.rules.rename.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnnotationRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Custom annotation remapper that includes remapping enum values in annotations.
 */
public final class FixedAnnotationRemapper extends AnnotationRemapper {

    FixedAnnotationRemapper(final int api, final String descriptor, final AnnotationVisitor annotationVisitor, final Remapper remapper) {
        super(api, descriptor, annotationVisitor, remapper);
    }

    @Override
    public void visitEnum(final String name, final String descriptor, final String value) {
        final String enumOwner = Type.getType(descriptor).getInternalName();
        super.visitEnum(name, descriptor, this.remapper.mapFieldName(enumOwner, value, descriptor));
    }

    @Override
    protected AnnotationVisitor createAnnotationRemapper(final String descriptor, final AnnotationVisitor annotationVisitor) {
        return new FixedAnnotationRemapper(this.api, descriptor, annotationVisitor, this.remapper);
    }
}
