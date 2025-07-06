package crypto.core;

import constants.Params;
import crypto.Table;
import exceptions.EncryptionException;
import field.operations.FP;
import org.jetbrains.annotations.NotNull;
import types.data.F2Element;
import types.point.AffinePoint;
import types.point.ExtendedPoint;
import types.point.FieldPoint;
import types.point.PreComputedExtendedPoint;

import java.math.BigInteger;

import static constants.Params.T_VARBASE;
import static field.operations.FP2.*;

public class ECC {
    // Set generator
    // Output: P = (x,y)
    public static FieldPoint eccSet() {
        return new FieldPoint(Params.GENERATOR_x, Params.GENERATOR_y);
    }

    @NotNull
    public static FieldPoint eccMulFixed(BigInteger val) throws EncryptionException {
        BigInteger temp = FP.moduloOrder(val);
        temp = FP.conversionToOdd(temp);
        int[] digits = Curve.mLSBSetRecode(temp, new int[270]);  // TODO: No idea how this works
        int digit = digits[Params.W_FIXEDBASE * Params.D_FIXEDBASE - 1];
        int startI = (Params.W_FIXEDBASE - 1) * Params.D_FIXEDBASE - 1;
        for (int i = startI; i >= 2 * Params.D_FIXEDBASE - 1; i -= Params.D_FIXEDBASE) {
            digit = 2 * digit + digits[i];
        }

        // TODO: Both instances of TABLE in this function might need updating
        AffinePoint affPoint = new AffinePoint();
        affPoint = Table.tableLookup(
                (Params.V_FIXEDBASE - 1) * (1 << (Params.W_FIXEDBASE - 1)),
                digit,
                digits[Params.D_FIXEDBASE - 1],
                affPoint
        ).toAffinePoint();
        ExtendedPoint exPoint = Conversion.r5ToR1(affPoint);

        for (int j = 0; j < Params.V_FIXEDBASE - 1; j++) {
            digit = digits[Params.W_FIXEDBASE * Params.D_FIXEDBASE - (j + 1) * Params.E_FIXEDBASE - 1];
            final int iStart = (Params.W_FIXEDBASE - 1) * Params.D_FIXEDBASE - (j + 1) * Params.E_FIXEDBASE - 1;
            final int iMin = 2 * Params.D_FIXEDBASE - (j + 1) * Params.E_FIXEDBASE - 1;
            for (int i = iStart; i >= iMin; i -= Params.D_FIXEDBASE) {
                digit = 2 * digit + digits[i];
            }
            // Extract point in (x+y,y-x,2dt) representation
            final int signDigit = Params.D_FIXEDBASE - (j + 1) * Params.E_FIXEDBASE - 1;
            final int tableStart = (Params.V_FIXEDBASE - j - 2) * (1 << (Params.W_FIXEDBASE - 1));
            affPoint = Table
                    .tableLookup(tableStart, digit, digits[signDigit], affPoint)
                    .toAffinePoint();
            exPoint = eccMixedAdd(affPoint, exPoint);
        }

        for (int i = Params.E_FIXEDBASE - 2; i >= 0; i--) {
            exPoint = eccDouble(exPoint);
            for (int j = 0; j < Params.V_FIXEDBASE; j++) {
                digit = digits[Params.W_FIXEDBASE * Params.D_FIXEDBASE - j * Params.E_FIXEDBASE + i - Params.E_FIXEDBASE];
                final int kStart = (Params.W_FIXEDBASE - 1) * Params.D_FIXEDBASE - j * Params.E_FIXEDBASE + i - Params.E_FIXEDBASE;
                final int kMin = 2 * Params.D_FIXEDBASE - j * Params.E_FIXEDBASE + i - Params.E_FIXEDBASE;
                for (int k = kStart; k >= kMin; k -= Params.D_FIXEDBASE) {
                    digit = 2 * digit + digits[k];
                }
                final int signDigit = Params.D_FIXEDBASE - j * Params.E_FIXEDBASE + i - Params.E_FIXEDBASE;
                final int tableStart = (Params.V_FIXEDBASE - j - 1) * (1 << (Params.W_FIXEDBASE - 1));
                affPoint = Table
                        .tableLookup(tableStart, digit, signDigit, affPoint)
                        .toAffinePoint();
                exPoint = eccMixedAdd(affPoint, exPoint);
            }
        }
        return eccNorm(exPoint);
    }

    public static FieldPoint  eccMul(
            FieldPoint p,
            BigInteger k,
            boolean clearCofactor
    ) throws EncryptionException {
        PreComputedExtendedPoint s;
        int[] signMasks = new int[T_VARBASE + 1];

        ExtendedPoint r = Curve.pointSetup(p);

        if (!eccPointValidate(r)) {
            throw new EncryptionException("Point validation failed within eccMul");
        }
        if (clearCofactor) {
            r = Curve.cofactorClearing(r);
        }

        BigInteger kOdd = FP.moduloOrder(k);
        kOdd = FP.conversionToOdd(kOdd);
        PreComputedExtendedPoint[] table = eccPrecomp(r);
        int[] digits = Curve.fixedWindowRecode(kOdd, signMasks);

        s = Table.tableLookup(table, digits[T_VARBASE], signMasks[T_VARBASE]);
        r = Conversion.r2ToR4(s, r);

        for (int i = T_VARBASE - 1; i >= 0; i--) {
            r = eccDouble(r);
            s = Table.tableLookup(table, digits[i], signMasks[i]);
            r = eccDouble(r);
            r = eccDouble(r);
            r = eccDouble(r);
            r = eccAdd(s, r);
        }

        return eccNorm(r);
    }

    private static ExtendedPoint eccMixedAdd(
            AffinePoint q,
            ExtendedPoint p
    ) {
        F2Element ta = fp2Mul1271(p.getTa(), p.getTb());          // Ta = T1
        F2Element t1 = fp2Add1271(p.getZ(), p.getZ());            // t1 = 2Z1
        ta = fp2Mul1271(ta, q.getT());                       // Ta = 2dT1*t2
        F2Element pz = fp2Add1271(p.getX(), p.getY());            // Z = (X1+Y1)
        F2Element tb = fp2Sub1271(p.getY(), p.getX());            // Tb = (Y1-X1)
        F2Element t2 = fp2Sub1271(t1, ta);              // t2 = theta
        t1 = fp2Add1271(t1, ta);                        // t1 = alpha
        ta = fp2Mul1271(q.getX(), pz);                       // Ta = (X1+Y1)(x2+y2)
        F2Element x = fp2Mul1271(q.getY(), tb);              // X = (Y1-X1)(y2-x2)
        tb = fp2Sub1271(ta, x);                         // Tbfinal = beta
        ta = fp2Add1271(ta, x);                         // Tafinal = omega
        return new ExtendedPoint(
                fp2Mul1271(tb, t2),                     // Xfinal = beta*theta
                fp2Mul1271(ta, t1),                     // Yfinal = alpha*omega
                fp2Mul1271(t1, t2),                     // Zfinal = theta*alpha
                ta,
                tb
        );
    }

    // Point doubling 2P
    // Input: P = (X1:Y1:Z1) in twisted Edwards coordinates
    // Output: 2P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal), where Tfinal = Tafinal*Tbfinal,
    //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    public static ExtendedPoint eccDouble(ExtendedPoint p) {
        F2Element t1 = fp2Sqr1271(p.getX());                 // t1 = X1^2
        F2Element t2 = fp2Sqr1271(p.getY());                 // t2 = Y1^2
        F2Element t3 = fp2Add1271(p.getX(), p.getY());            // t3 = X1+Y1
        F2Element tb = fp2Add1271(t1, t2);              // Tbfinal = X1^2+Y1^2
        t1 = fp2Sub1271(t2, t1);                        // t1 = Y1^2-X1^2
        F2Element ta = fp2Sqr1271(t3);                  // Ta = (X1+Y1)^2
        t2 = fp2Sqr1271(p.getZ());                           // t2 = Z1^2
        ta = fp2Sub1271(ta, tb);                        // Tafinal = 2X1*Y1 = (X1+Y1)^2-(X1^2+Y1^2)
        t2 = fp2AddSub1271(t2, t1);                     // t2 = 2Z1^2-(Y1^2-X1^2)
        final F2Element y = fp2Mul1271(t1, tb);         // Yfinal = (X1^2+Y1^2)(Y1^2-X1^2)
        final F2Element x = fp2Mul1271(t2, ta);         // Xfinal = 2X1*Y1*[2Z1^2-(Y1^2-X1^2)]
        final F2Element z = fp2Mul1271(t1, t2);         // Zfinal = (Y1^2-X1^2)[2Z1^2-(Y1^2-X1^2)]
        return new ExtendedPoint(x, y, z, ta, tb);
    }

    public static FieldPoint eccNorm(ExtendedPoint p) {
        final F2Element zInv = fp2Inv1271(p.getZ());
        final F2Element x = fp2Mul1271(p.getX(), zInv);
        final F2Element y = fp2Mul1271(p.getY(), zInv);

        x.im = FP.PUtil.mod1271(x.im);
        x.real = FP.PUtil.mod1271(x.real);

        y.im = FP.PUtil.mod1271(y.im);
        y.real = FP.PUtil.mod1271(y.real);
        return new FieldPoint(x, y);
    }

    @NotNull
    public static FieldPoint eccMulDouble(
            BigInteger k,
            FieldPoint q,
            BigInteger l
    ) throws EncryptionException {
        // Step 1: Compute l*Q
        FieldPoint lQ = eccMul(q, l, false);

        // Step 2-3: Convert l*Q to precomputed format
        ExtendedPoint extLQ = Curve.pointSetup(lQ);
        PreComputedExtendedPoint preCompLQ = Conversion.r1ToR2(extLQ);

        // Step 4: Compute k*G (generator multiplication)
        FieldPoint kG = eccMulFixed(k);

        // Step 5-6: Add k*G + l*Q
        ExtendedPoint extKG = Curve.pointSetup(kG);
        ExtendedPoint result = eccAdd(preCompLQ, extKG);

        // Step 7: Normalize to affine coordinates
        return eccNorm(result);
    }

    private static ExtendedPoint eccAddCore(
            PreComputedExtendedPoint p,
            PreComputedExtendedPoint q
    ) {
        F2Element z = fp2Mul1271(p.t, q.t);
        F2Element t1 = fp2Mul1271(p.z, q.z);
        F2Element x = fp2Mul1271(p.xy, q.xy);
        F2Element y = fp2Mul1271(p.yx, q.yx);
        F2Element t2 = fp2Sub1271(t1, z);
        t1 = fp2Add1271(t1, z);
        F2Element tb = fp2Sub1271(x, y);
        F2Element ta = fp2Add1271(x, y);
        return new ExtendedPoint(
                fp2Mul1271(tb, t2),
                fp2Mul1271(ta, t1),
                fp2Mul1271(t1, t2),
                ta,
                tb
        );
    }

    static ExtendedPoint eccAdd(
            PreComputedExtendedPoint q,
            ExtendedPoint p
    ) {
        return eccAddCore(q, Conversion.r1ToR3(p));
    }

    /**
     * Point validation: check if point lies on the curve
     * @param p = (x,y) in affine coordinates, where x, y in [0, 2^127-1]
     * @return true if point lies on curve E: -x^2+y^2-1-dx^2*y^2 = 0, false otherwise
     *
     * @implNote this function does not run in constant time (input point P is assumed to be public)
     */
    public static boolean eccPointValidate(@NotNull ExtendedPoint p) {
        F2Element t1 = fp2Sqr1271(p.getY());                                 // y^2
        F2Element t2 = fp2Sqr1271(p.getX());                                 // x^2
        F2Element t3 = fp2Sub1271(t1, t2);                              // y^2 - x^2 = -x^2 + y^2

        t1 = fp2Mul1271(t1, t2);                                        // x^2*y^2
        t2 = fp2Mul1271(Params.PARAMETER_d, t1);    // dx^2*y^2

        // Create F2Element representing 1 + 0i
        F2Element one = new F2Element(
                BigInteger.ONE,
                BigInteger.ZERO
        );

        t2 = fp2Add1271(t2, one);                                       // 1 + dx^2*y^2
        t1 = fp2Sub1271(t3, t2);                                        // -x^2 + y^2 - 1 - dx^2*y^2

        // Reduce modulo (2^127-1)
        t1 = new F2Element(
                FP.PUtil.mod1271(t1.real),
                FP.PUtil.mod1271(t1.im)
        );

        // Check if the result is zero (both real and imaginary parts must be zero) to be on the curve.
        return t1.real.equals(BigInteger.ZERO) && t1.im.equals(BigInteger.ZERO);
    }

    /**
     * Generation of the precomputation table used by the variable-base scalar multiplication eccMul().
     * @param p = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates.
     * @return table T containing NPOINTS_VARBASE points: P, 3P, 5P, ... , (2*NPOINTS_VARBASE-1)P. NPOINTS_VARBASE is fixed to 8 (see FourQ.h).
     *         Precomputed points use the representation (X+Y,Y-X,2Z,2dT) corresponding to (X:Y:Z:T) in extended twisted Edwards coordinates.
     */
    @NotNull
    public static PreComputedExtendedPoint[] eccPrecomp(@NotNull ExtendedPoint p) {
        // Initialize the output table
        PreComputedExtendedPoint[] t
                = new PreComputedExtendedPoint[Params.NPOINTS_VARBASE.intValueExact()];

        PreComputedExtendedPoint p2;
        ExtendedPoint q;

        // Generating P2 = 2(X1,Y1,Z1,T1a,T1b) and T[0] = P
        q = p.dup();
        t[0] = Conversion.r1ToR2(p);                  // T[0] = P in (X+Y,Y-X,2Z,2dT) format
        q = eccDouble(q);                              // Q = 2P
        p2 = Conversion.r1ToR3(q);                    // P2 = 2P in R3 format

        // Generate odd multiples: 3P, 5P, 7P, ..., (2*NPOINTS_VARBASE-1)P
        for (int i = 1; i < Params.NPOINTS_VARBASE.intValueExact(); i++) {
            // T[i] = 2P + T[i-1] = (2*i+1)P
            q = eccAddCore(p2, t[i-1]);    // Add 2P to previous odd multiple
            t[i] = Conversion.r1ToR2(q);              // Convert result to R2 format
        }

        return t;
    }
}
