package es.stratebi.civi.util;

import java.lang.reflect.Field;

/*
 * 
 * 
 * Aqui defino todos los atributos que pudiera tener (que he encontrado) un campo retornado por
 * CIVICRM para evitarme el problema de saber a quien le pongo el valor retornado y hacer multiples
 * if he creado un mEtodo set(campo, valor) que usa Reflection y recibe el nombre del atributo y el
 * se encarga de asignarle el valor a ese campo del objeto.
 * 
 * Se han creeado 2 constructores Utiles para crear la clase, y se modificO toString para devolver
 * todos los campos con sus valores
 */
public class FieldAttrs {

    private String fFieldKey = "";
    private String fApi_aliases = "";
    private String fApi_default = "";
    private String fCustom_group_id = "";
    private String fData_type = "";
    private String fDataPattern = "";
    private String fDate_format = "";
    private String fDefault = "";
    private String fEnumValues = "";
    private String fExport = "";
    private String fExtends = "";
    private String fExtends_entity_column_id = "";
    private String fExtends_entity_column_value = "";
    private String fFKClassName = "";
    private String fGroupTitle = "";
    private String fHeaderPattern = "";
    private String fHtml_type = "";
    private String fImport = "";
    private String fIs_multiple = "";
    private String fIs_search_range = "";
    private String fIs_view = "";
    private String fLabel = "";
    private String fMaxlength = "";
    private String fName = "";
    private String fOption_group_id = "";
    private String fOptions = "";
    private String fOptions_per_line = "";
    private String fPseudoconstant = "";
    private String fRequired = "";
    private String fRule = "";
    private String fSize = "";
    private String fText_length = "";
    private String fTime_format = "";
    private String fTitle = "";
    private String fType = "";
    private String fWhere = "";

    public FieldAttrs() {
        super();
    }

    /*
     * Este metodo usa la variante inversa de toString, dada una cadena con la forma
     * field=valor&&field=valor ... la leemos y dividimos usando los separadores definidos entre
     * atributos inicialenete y luego entre atributo y valor para asignarlo a los campos mediante
     * reflection
     */
    public FieldAttrs(String fieldValues) {
        String[] fieldValuePair = fieldValues.split("::");
        for (String fvp : fieldValuePair) {
            String[] field = fvp.split("=");
            try {
                set(field[0], field[1]);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
    }

    public String getfFieldKey() {
        return fFieldKey;
    }

    public void setfFieldKey(String fFieldKey) {
        this.fFieldKey = fFieldKey;
    }

    public void set(String field, String value) throws Exception {
        Field f = this.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(this, (value != null) ? value : "");
    }

    public String getfApi_aliases() {
        return fApi_aliases;
    }

    public void setfApi_aliases(String fApi_aliases) {
        this.fApi_aliases = fApi_aliases;
    }

    public String getfApi_default() {
        return fApi_default;
    }

    public void setfApi_default(String fApi_default) {
        this.fApi_default = fApi_default;
    }

    public String getfCustom_group_id() {
        return fCustom_group_id;
    }

    public void setfCustom_group_id(String fCustom_group_id) {
        this.fCustom_group_id = fCustom_group_id;
    }

    public String getfData_type() {
        return fData_type;
    }

    public void setfData_type(String fData_type) {
        this.fData_type = fData_type;
    }

    public String getfDataPattern() {
        return fDataPattern;
    }

    public void setfDataPattern(String fDataPattern) {
        this.fDataPattern = fDataPattern;
    }

    public String getfDate_format() {
        return fDate_format;
    }

    public void setfDate_format(String fDate_format) {
        this.fDate_format = fDate_format;
    }

    public String getfDefault() {
        return fDefault;
    }

    public void setfDefault(String fDefault) {
        this.fDefault = fDefault;
    }

    public String getfEnumValues() {
        return fEnumValues;
    }

    public void setfEnumValues(String fEnumValues) {
        this.fEnumValues = fEnumValues;
    }

    public String getfExport() {
        return fExport;
    }

    public void setfExport(String fExport) {
        this.fExport = fExport;
    }

    public String getfExtends() {
        return fExtends;
    }

    public void setfExtends(String fExtends) {
        this.fExtends = fExtends;
    }

    public String getfExtends_entity_column_id() {
        return fExtends_entity_column_id;
    }

    public void setfExtends_entity_column_id(String fExtends_entity_column_id) {
        this.fExtends_entity_column_id = fExtends_entity_column_id;
    }

    public String getfExtends_entity_column_value() {
        return fExtends_entity_column_value;
    }

    public void setfExtends_entity_column_value(String fExtends_entity_column_value) {
        this.fExtends_entity_column_value = fExtends_entity_column_value;
    }

    public String getfFKClassName() {
        return fFKClassName;
    }

    public void setfFKClassName(String fFKClassName) {
        this.fFKClassName = fFKClassName;
    }

    public String getfGroupTitle() {
        return fGroupTitle;
    }

    public void setfGroupTitle(String fGroupTitle) {
        this.fGroupTitle = fGroupTitle;
    }

    public String getfHeaderPattern() {
        return fHeaderPattern;
    }

    public void setfHeaderPattern(String fHeaderPattern) {
        this.fHeaderPattern = fHeaderPattern;
    }

    public String getfHtml_type() {
        return fHtml_type;
    }

    public void setfHtml_type(String fHtml_type) {
        this.fHtml_type = fHtml_type;
    }

    public String getfImport() {
        return fImport;
    }

    public void setfImport(String fImport) {
        this.fImport = fImport;
    }

    public String getfIs_multiple() {
        return fIs_multiple;
    }

    public void setfIs_multiple(String fIs_multiple) {
        this.fIs_multiple = fIs_multiple;
    }

    public String getfIs_search_range() {
        return fIs_search_range;
    }

    public void setfIs_search_range(String fIs_search_range) {
        this.fIs_search_range = fIs_search_range;
    }

    public String getfIs_view() {
        return fIs_view;
    }

    public void setfIs_view(String fIs_view) {
        this.fIs_view = fIs_view;
    }

    public String getfLabel() {
        return fLabel;
    }

    public void setfLabel(String fLabel) {
        this.fLabel = fLabel;
    }

    public String getfMaxlength() {
        return fMaxlength;
    }

    public void setfMaxlength(String fMaxlength) {
        this.fMaxlength = fMaxlength;
    }

    public String getfName() {
        return fName;
    }

    public void setfName(String fName) {
        this.fName = fName;
    }

    public String getfOption_group_id() {
        return fOption_group_id;
    }

    public void setfOption_group_id(String fOption_group_id) {
        this.fOption_group_id = fOption_group_id;
    }

    public String getfOptions() {
        return fOptions;
    }

    public void setfOptions(String fOptions) {
        this.fOptions = fOptions;
    }

    public String getfOptions_per_line() {
        return fOptions_per_line;
    }

    public void setfOptions_per_line(String fOptions_per_line) {
        this.fOptions_per_line = fOptions_per_line;
    }

    public String getfPseudoconstant() {
        return fPseudoconstant;
    }

    public void setfPseudoconstant(String fPseudoconstant) {
        this.fPseudoconstant = fPseudoconstant;
    }

    public String getfRequired() {
        return fRequired;
    }

    public void setfRequired(String fRequired) {
        this.fRequired = fRequired;
    }

    public String getfRule() {
        return fRule;
    }

    public void setfRule(String fRule) {
        this.fRule = fRule;
    }

    public String getfSize() {
        return fSize;
    }

    public void setfSize(String fSize) {
        this.fSize = fSize;
    }

    public String getfText_length() {
        return fText_length;
    }

    public void setfText_length(String fText_length) {
        this.fText_length = fText_length;
    }

    public String getfTime_format() {
        return fTime_format;
    }

    public void setfTime_format(String fTime_format) {
        this.fTime_format = fTime_format;
    }

    public String getfTitle() {
        return fTitle;
    }

    public void setfTitle(String fTitle) {
        this.fTitle = fTitle;
    }

    public String getfType() {
        return fType;
    }

    public void setfType(String fType) {
        this.fType = fType;
    }

    public String getfWhere() {
        return fWhere;
    }

    public void setfWhere(String fWhere) {
        this.fWhere = fWhere;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        FieldAttrs civiFieldClone = new FieldAttrs();

        civiFieldClone.fApi_aliases = this.fApi_aliases;
        civiFieldClone.fApi_default = this.fApi_default;
        civiFieldClone.fCustom_group_id = this.fCustom_group_id;
        civiFieldClone.fData_type = this.fData_type;
        civiFieldClone.fDataPattern = this.fDataPattern;
        civiFieldClone.fDate_format = this.fDate_format;
        civiFieldClone.fDefault = this.fDefault;
        civiFieldClone.fEnumValues = this.fEnumValues;
        civiFieldClone.fExport = this.fExport;
        civiFieldClone.fExtends = this.fExtends;
        civiFieldClone.fExtends_entity_column_id = this.fExtends_entity_column_id;
        civiFieldClone.fExtends_entity_column_value = this.fExtends_entity_column_value;
        civiFieldClone.fFKClassName = this.fFKClassName;
        civiFieldClone.fGroupTitle = this.fGroupTitle;
        civiFieldClone.fHeaderPattern = this.fHeaderPattern;
        civiFieldClone.fHtml_type = this.fHtml_type;
        civiFieldClone.fImport = this.fImport;
        civiFieldClone.fIs_multiple = this.fIs_multiple;
        civiFieldClone.fIs_search_range = this.fIs_search_range;
        civiFieldClone.fIs_view = this.fIs_view;
        civiFieldClone.fLabel = this.fLabel;
        civiFieldClone.fMaxlength = this.fMaxlength;
        civiFieldClone.fName = this.fName;
        civiFieldClone.fOption_group_id = this.fOption_group_id;
        civiFieldClone.fOptions = this.fOptions;
        civiFieldClone.fOptions_per_line = this.fOptions_per_line;
        civiFieldClone.fPseudoconstant = this.fPseudoconstant;
        civiFieldClone.fRequired = this.fRequired;
        civiFieldClone.fRule = this.fRule;
        civiFieldClone.fSize = this.fSize;
        civiFieldClone.fText_length = this.fText_length;
        civiFieldClone.fTime_format = this.fTime_format;
        civiFieldClone.fTitle = this.fTitle;
        civiFieldClone.fType = this.fType;
        civiFieldClone.fWhere = this.fWhere;

        return civiFieldClone;
    }

    @Override
    /*
     * Aqu� creamos la cadena con los valores de cada campo concatenados y usando separadores para
     * luego poder recovertirla en una clase usando el constructor definido para esto.
     */
    public String toString() {
        return "fFieldKey" + "=" + fFieldKey + "::" + "fApi_aliases" + "=" + fApi_aliases + "::" + "fApi_default"
                + "=" + fApi_default + "::" + "fCustom_group_id" + "=" + fCustom_group_id + "::" + "fData_type"
                + "=" + fData_type + "::" + "fDataPattern" + "=" + fDataPattern + "::" + "fDate_format" + "="
                + fDate_format + "::" + "fDefault" + "=" + fDefault + "::" + "fEnumValues" + "=" + fEnumValues + "::"
                + "fExport" + "=" + fExport + "::" + "fExtends" + "=" + fExtends + "::" + "fExtends_entity_column_id"
                + "=" + fExtends_entity_column_id + "::" + "fExtends_entity_column_value" + "="
                + fExtends_entity_column_value + "::" + "fFKClassName" + "=" + fFKClassName + "::" + "fGroupTitle"
                + "=" + fGroupTitle + "::" + "fHeaderPattern" + "=" + fHeaderPattern + "::" + "fHtml_type" + "="
                + fHtml_type + "::" + "fImport" + "=" + fImport + "::" + "fIs_multiple" + "=" + fIs_multiple + "::"
                + "fIs_search_range" + "=" + fIs_search_range + "::" + "fIs_view" + "=" + fIs_view + "::" + "fLabel"
                + "=" + fLabel + "::" + "fMaxlength" + "=" + fMaxlength + "::" + "fName" + "=" + fName + "::"
                + "fOption_group_id" + "=" + fOption_group_id + "::" + "fOptions" + "=" + fOptions + "::"
                + "fOptions_per_line" + "=" + fOptions_per_line + "::" + "fPseudoconstant" + "=" + fPseudoconstant
                + "::" + "fRequired" + "=" + fRequired + "::" + "fRule" + "=" + fRule + "::" + "fSize" + "=" + fSize
                + "::" + "fText_length" + "=" + fText_length + "::" + "fTime_format" + "=" + fTime_format + "::"
                + "fTitle" + "=" + fTitle + "::" + "fType" + "=" + fType + "::" + "fWhere" + "=" + fWhere;
    }

}
