package io.papermc.asm.rules.rename.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * asm-commons ClassRemapper doesn't fully
 * remap values in annotation "fields".
 */
public final class FixedClassRemapper extends ClassRemapper {

    public FixedClassRemapper(final int api, final ClassVisitor classVisitor, final Remapper remapper) {
        super(api, classVisitor, remapper);
    }

    @Override
    protected AnnotationVisitor createAnnotationRemapper(final String descriptor, final AnnotationVisitor annotationVisitor) {
        return new FixedAnnotationRemapper(this.api, descriptor, annotationVisitor, this.remapper);
    }

    @Override
    protected FieldVisitor createFieldRemapper(final FieldVisitor fieldVisitor) {
        return new FixedFieldRemapper(this.api, fieldVisitor, this.remapper);
    }

    @Override
    protected RecordComponentVisitor createRecordComponentRemapper(final RecordComponentVisitor recordComponentVisitor) {
        return new FixedRecordComponentRemapper(this.api, recordComponentVisitor, this.remapper);
    }

    @Override
    protected MethodVisitor createMethodRemapper(final MethodVisitor methodVisitor) {
        return new FixedMethodRemapper(this.api, methodVisitor, this.remapper);
    }
}
