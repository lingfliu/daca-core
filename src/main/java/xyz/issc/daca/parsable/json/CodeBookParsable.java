package xyz.issc.daca.parsable.json;

import com.google.gson.Gson;
import xyz.issc.daca.spec.CodeBook;
import xyz.issc.daca.utils.StringUtils;

import java.util.List;

public class CodeBookParsable {

    String header;

    boolean headless;

    CodeParsable meta_code;

    List<CodeParsable> codes;

    boolean msb;

    public CodeBook convert() {
        CodeBook codebook = new CodeBook();
        codebook.setHeader(StringUtils.hexString2bytes(header));
        codebook.setHeadless(headless);
        codebook.setMetaCode(meta_code.convert());
        for (CodeParsable code : codes) {
            codebook.getCodes().put(code.type, code.convert());
        }
      return codebook;
   }

   public static CodeBook fromJson(String json) {
      CodeBookParsable parsable = new Gson().fromJson(json, CodeBookParsable.class);
      return parsable.convert();
   }
}
