package io.papermc.asm.rules.rename.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.commons.RecordComponentRemapper;
import org.objectweb.asm.commons.Remapper;

public final class FixedRecordComponentRemapper extends RecordComponentRemapper {

    FixedRecordComponentRemapper(final int api, final RecordComponentVisitor recordComponentVisitor, final Remapper remapper) {
        super(api, recordComponentVisitor, remapper);
    }

    @Override
    protected AnnotationVisitor createAnnotationRemapper(final String descriptor, final AnnotationVisitor annotationVisitor) {
        return new FixedAnnotationRemapper(this.api, descriptor, annotationVisitor, this.remapper);
    }
}
