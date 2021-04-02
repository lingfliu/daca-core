package xyz.issc.daca;

import lombok.Getter;
import xyz.issc.daca.utils.ArrayHelper;

/**
 * Simple value object
 */
public class Svo {

    public static class ValueUnpackException extends Exception {
    }
    public static class ValuePackException extends Exception {
    }

    public enum Type {
        RAW, // byte arrays without conversion
        INT,
        INT_ARRAY,
        FLOAT,
        FLOAT_ARRAY,
        STRING, //strings and char
        BOOL, // 布尔
        BOOL_ARRAY
    }

    long valInt;
    long[] arrayInt;

    double valFloat;
    double[] arrayFloat;

    String valString;

    boolean valBool;
    boolean[] arrayBool;

    byte[] valRaw;

    @Getter
    public Type type;


    private Svo() {
    }

    public boolean rangeCheck(long min, long max) {
        if (type != Type.INT) {
            return false;
        }

        if (valInt < min || valInt > max) {
            return false;
        }

        return true;
    }

    public boolean rangeCheck(double min, double max) {
        if (type != Type.FLOAT) {
            return false;
        }

        if (valFloat < min || valFloat > max) {
            return false;
        }

        return true;
    }



    public static Svo pack(Type type, Object val) throws ValuePackException {
        Svo obj = new Svo();
        obj.type = type;
        switch (type) {
            case INT:
                if (val.getClass().toString().equals(long.class.toString())) {
                    obj.valInt = (long) val;
                    return obj;
                }
                else {
                    throw new ValuePackException();
                }
            case INT_ARRAY:
                if (val.getClass().equals(long[].class)) {
                    obj.arrayInt = (long[]) val;
                    return obj;
                }
                else {
                    throw new ValuePackException();
                }
            case FLOAT:
                if (val.getClass().toString().equals(double.class.toString())) {
                    obj.valFloat = (double) val;
                    return obj;
                }
                else {
                    throw new ValuePackException();
                }
            case FLOAT_ARRAY:
                if (val.getClass().equals(double[].class)) {
                    obj.arrayFloat = (double[]) val;
                    return obj;
                }
                else {
                    throw new ValuePackException();
                }
            case BOOL:
                if (val.getClass().toString().equals(boolean.class.toString())) {
                    obj.valBool = (boolean)val;
                    return obj;
                }
                else {
                    throw new ValuePackException();
                }
            case BOOL_ARRAY:
                if (val.getClass().equals(boolean[].class)) {
                    obj.arrayBool = (boolean[])val;
                    return obj;
                }
                else {
                    throw new ValuePackException();
                }
            case STRING:
                if (val.getClass().equals(String.class)) {
                    obj.valString = (String)val;
                    return obj;
                }
                else {
                    throw new ValuePackException();
                }
            case RAW:
                if (val.getClass().equals(byte[].class)) {
                    obj.valRaw = (byte[]) val;
                }
                return obj;

            default:
                throw new ValuePackException();

        }
    }

    public Svo squeeze() {
        switch (type) {
            case INT_ARRAY:
                if (arrayInt.length <= 1) {
                    type = Type.INT;
                    valInt =arrayInt[0];
                    arrayInt = null;
                }
                break;
            case FLOAT_ARRAY:
                if (arrayFloat.length <= 1) {
                    type = Type.FLOAT;
                    valFloat = arrayFloat[0];
                    arrayFloat = null;
                }
                break;
            default:
                break;
        }
        return this;
    }

    public long unpackInteger() throws ValueUnpackException {
        if (type.equals(Type.INT)) {
            return valInt;
        } else {
            throw new ValueUnpackException();
        }
    }

    public long[] unpackIntegerArray() throws ValueUnpackException {
        if (type.equals(Type.INT_ARRAY)) {
            return arrayInt;
        } else {
            throw new ValueUnpackException();
        }
    }

    public double unpackFloat() throws ValueUnpackException {
        if (type.equals(Type.FLOAT)) {
            return valFloat;
        } else {
            throw new ValueUnpackException();
        }
    }

    public double[] unpackFloatArray() throws ValueUnpackException {
        if (type.equals(Type.FLOAT_ARRAY)) {
            return arrayFloat;
        } else {
            throw new ValueUnpackException();
        }
    }

    public String unpackString() throws ValueUnpackException {
        if (type.equals(Type.STRING)) {
            return valString;
        } else {
            throw new ValueUnpackException();
        }
    }

    public boolean unpackBool() throws ValueUnpackException {
        if (type.equals(Type.BOOL)) {
            return valBool;
        } else {
            throw new ValueUnpackException();
        }
    }

    public boolean[] unpackBoolArray() throws ValueUnpackException {
        if (type.equals(Type.BOOL_ARRAY)) {
            return arrayBool;
        } else {
            throw new ValueUnpackException();
        }
    }

    public byte[] unpackRaw() throws ValueUnpackException {
        if (type.equals(Type.RAW)) {
            return valRaw;
        } else {
            throw new ValueUnpackException();
        }
    }

    public int getSize() {
        switch (type) {
            case RAW:
                return valRaw.length;
            case INT:
                return 1;
            case INT_ARRAY:
                return arrayInt.length;
            case FLOAT:
                return 1;
            case FLOAT_ARRAY:
                return arrayFloat.length;
            case BOOL:
                return 1;
            case BOOL_ARRAY:
                return arrayBool.length;
            case STRING:
                return valString.getBytes().length;
            default:
                return 0;
        }
    }

    public boolean equals(Svo obj) {
        if (type != obj.type) return false;

        switch (type) {
            case RAW:
                return ArrayHelper.cmp(valRaw, obj.valRaw, 0, valRaw.length);
            case INT:
                    return valInt == obj.valInt;
            case INT_ARRAY:
                return arrayInt.length == obj.arrayInt.length && ArrayHelper.cmp(arrayInt, obj.arrayInt, 0, arrayInt.length);
            case FLOAT:
                return valFloat == obj.valFloat;
            case FLOAT_ARRAY:
                return arrayFloat.length == obj.arrayFloat.length && ArrayHelper.cmp(arrayFloat, obj.arrayFloat, 0, arrayFloat.length);
            case BOOL:
                return valBool == obj.valBool;
            case BOOL_ARRAY:
                return arrayBool.length == obj.arrayBool.length && ArrayHelper.cmp(arrayBool, obj.arrayBool, 0, arrayBool.length);
            case STRING:
                return valString.equals(obj.valString);
            default:
                return false;
        }
    }
}
