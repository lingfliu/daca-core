package xyz.issc.daca.parsable.json;

import com.google.gson.annotations.SerializedName;

public enum ParseTypeParsable {
    @SerializedName("uint")
    UINT,
    @SerializedName("int")
    INT,
    @SerializedName("string_int")
    STRING_INT,

    @SerializedName("float")
    FLOAT,
    @SerializedName("string_float")
    STRING_FLOAT, //not recommended

    @SerializedName("decimal")
    DECIMAL,

    @SerializedName("string")
    STRING,
    @SerializedName("bool")
    BOOL,
    @SerializedName("bool_array")
    BOOL_ARRAY
}
