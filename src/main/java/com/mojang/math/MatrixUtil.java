package com.mojang.math;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MatrixUtil {
    private static final float G = 3.0F + 2.0F * Math.sqrt(2.0F);
    private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle((float) (java.lang.Math.PI / 4));

    private MatrixUtil() {
    }

    public static Matrix4f mulComponentWise(final Matrix4f m, final float factor) {
        return m.set(
            m.m00() * factor,
            m.m01() * factor,
            m.m02() * factor,
            m.m03() * factor,
            m.m10() * factor,
            m.m11() * factor,
            m.m12() * factor,
            m.m13() * factor,
            m.m20() * factor,
            m.m21() * factor,
            m.m22() * factor,
            m.m23() * factor,
            m.m30() * factor,
            m.m31() * factor,
            m.m32() * factor,
            m.m33() * factor
        );
    }

    private static GivensParameters approxGivensQuat(final float a11, final float a12, final float a22) {
        float ch = 2.0F * (a11 - a22);
        float sh = a12;
        return G * sh * sh < ch * ch ? GivensParameters.fromUnnormalized(sh, ch) : PI_4;
    }

    private static GivensParameters qrGivensQuat(final float a1, final float a2) {
        float p = (float)java.lang.Math.hypot(a1, a2);
        float sh = p > 1.0E-6F ? a2 : 0.0F;
        float ch = Math.abs(a1) + Math.max(p, 1.0E-6F);
        if (a1 < 0.0F) {
            float f = sh;
            sh = ch;
            ch = f;
        }

        return GivensParameters.fromUnnormalized(sh, ch);
    }

    private static void similarityTransform(final Matrix3f a, final Matrix3f q) {
        a.mul(q);
        q.transpose();
        q.mul(a);
        a.set(q);
    }

    private static void stepJacobi(final Matrix3f m, final Matrix3f tmpMat, final Quaternionf tmpQ, final Quaternionf output) {
        if (m.m01 * m.m01 + m.m10 * m.m10 > 1.0E-6F) {
            GivensParameters p = approxGivensQuat(m.m00, 0.5F * (m.m01 + m.m10), m.m11);
            Quaternionf qt = p.aroundZ(tmpQ);
            output.mul(qt);
            p.aroundZ(tmpMat);
            similarityTransform(m, tmpMat);
        }

        if (m.m02 * m.m02 + m.m20 * m.m20 > 1.0E-6F) {
            GivensParameters p = approxGivensQuat(m.m00, 0.5F * (m.m02 + m.m20), m.m22).inverse();
            Quaternionf qt = p.aroundY(tmpQ);
            output.mul(qt);
            p.aroundY(tmpMat);
            similarityTransform(m, tmpMat);
        }

        if (m.m12 * m.m12 + m.m21 * m.m21 > 1.0E-6F) {
            GivensParameters p = approxGivensQuat(m.m11, 0.5F * (m.m12 + m.m21), m.m22);
            Quaternionf qt = p.aroundX(tmpQ);
            output.mul(qt);
            p.aroundX(tmpMat);
            similarityTransform(m, tmpMat);
        }
    }

    public static void eigenvalueJacobi(final Matrix3f inOut, final int steps, final Quaternionf result) {
        result.identity();
        Matrix3f scratchMat = new Matrix3f();
        Quaternionf scratchQ = new Quaternionf();

        for (int i = 0; i < steps; i++) {
            stepJacobi(inOut, scratchMat, scratchQ, result);
        }

        result.normalize();
    }

    public static void svdDecompose(final Matrix4fc input, final Vector3f t, final Quaternionf u, final Vector3f s, final Quaternionf v) {
        float scaleFactor = 1.0F / input.m33();
        svdDecompose(new Matrix3f(input).scale(scaleFactor), u, s, v);
        input.getTranslation(t).mul(scaleFactor);
    }

    private static void svdDecompose(final Matrix3f input, final Quaternionf u, final Vector3f s, final Quaternionf v) {
        Matrix3f scratch = new Matrix3f(input);
        scratch.transpose();
        scratch.mul(input);
        eigenvalueJacobi(scratch, 5, v);
        float columnScaleSquare0 = scratch.m00;
        float columnScaleSquare1 = scratch.m11;
        boolean zeroColumn0 = columnScaleSquare0 < 1.0E-6;
        boolean zeroColumn1 = columnScaleSquare1 < 1.0E-6;
        Matrix3f u012s = input.rotate(v);
        u.identity();
        Quaternionf tmpQ = new Quaternionf();
        GivensParameters p;
        if (zeroColumn0) {
            p = qrGivensQuat(u012s.m11, -u012s.m10);
        } else {
            p = qrGivensQuat(u012s.m00, u012s.m01);
        }

        Quaternionf qt0 = p.aroundZ(tmpQ);
        Matrix3f u12s = p.aroundZ(scratch);
        u.mul(qt0);
        u12s.transpose().mul(u012s);
        scratch = u012s;
        if (zeroColumn0) {
            p = qrGivensQuat(u12s.m22, -u12s.m20);
        } else {
            p = qrGivensQuat(u12s.m00, u12s.m02);
        }

        p = p.inverse();
        Quaternionf qt1 = p.aroundY(tmpQ);
        Matrix3f u2s = p.aroundY(scratch);
        u.mul(qt1);
        u2s.transpose().mul(u12s);
        scratch = u12s;
        if (zeroColumn1) {
            p = qrGivensQuat(u2s.m22, -u2s.m21);
        } else {
            p = qrGivensQuat(u2s.m11, u2s.m12);
        }

        Quaternionf qt2 = p.aroundX(tmpQ);
        Matrix3f sm = p.aroundX(scratch);
        u.mul(qt2);
        sm.transpose().mul(u2s);
        s.set(sm.m00, sm.m11, sm.m22);
        v.conjugate();
    }

    public static boolean checkPropertyRaw(final Matrix4fc matrix, final int property) {
        return (matrix.properties() & property) != 0;
    }

    public static boolean checkProperty(final Matrix4fc matrix, final int property) {
        if (checkPropertyRaw(matrix, property)) {
            return true;
        } else if (matrix instanceof Matrix4f mutableMatrix) {
            int currentProperties = mutableMatrix.properties();
            mutableMatrix.determineProperties();
            mutableMatrix.assume(mutableMatrix.properties() | currentProperties);
            return checkPropertyRaw(matrix, property);
        } else {
            return false;
        }
    }

    public static boolean isIdentity(final Matrix4fc matrix) {
        return checkProperty(matrix, Matrix4fc.PROPERTY_IDENTITY);
    }

    public static boolean isPureTranslation(final Matrix4fc matrix) {
        return checkProperty(matrix, Matrix4fc.PROPERTY_TRANSLATION);
    }
}
