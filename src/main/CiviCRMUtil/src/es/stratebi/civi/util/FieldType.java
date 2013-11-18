package es.stratebi.civi.util;

import org.pentaho.di.core.row.ValueMetaInterface;

// CiviCRM - Kettle Type Mappings 
public final class FieldType {   
    final public static int cType1 = ValueMetaInterface.TYPE_INTEGER;
    final public static int cType2 = ValueMetaInterface.TYPE_STRING;
    final public static int cType4 = ValueMetaInterface.TYPE_DATE;
    final public static int cType16 = ValueMetaInterface.TYPE_INTEGER;
    final public static int cType256 = ValueMetaInterface.TYPE_DATE;

    final public static FieldType civiFieldType = new FieldType();
}
