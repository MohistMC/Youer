package io.papermc.asm.rules.rename.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

public final class FixedMethodRemapper extends MethodRemapper {

    FixedMethodRemapper(final int api, final MethodVisitor methodVisitor, final Remapper remapper) {
        super(api, methodVisitor, remapper);
    }

    @Override
    protected AnnotationVisitor createAnnotationRemapper(final String descriptor, final AnnotationVisitor annotationVisitor) {
        return new FixedAnnotationRemapper(this.api, descriptor, annotationVisitor, this.remapper);
    }
}
