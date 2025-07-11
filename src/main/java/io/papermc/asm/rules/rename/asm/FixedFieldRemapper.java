package io.papermc.asm.rules.rename.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.commons.FieldRemapper;
import org.objectweb.asm.commons.Remapper;

public final class FixedFieldRemapper extends FieldRemapper {

    FixedFieldRemapper(final int api, final FieldVisitor fieldVisitor, final Remapper remapper) {
        super(api, fieldVisitor, remapper);
    }

    @Override
    protected AnnotationVisitor createAnnotationRemapper(final String descriptor, final AnnotationVisitor annotationVisitor) {
        return new FixedAnnotationRemapper(this.api, descriptor, annotationVisitor, this.remapper);
    }
}
