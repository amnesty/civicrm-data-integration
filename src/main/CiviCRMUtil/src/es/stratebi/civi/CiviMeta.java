package es.stratebi.civi;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import es.stratebi.civi.util.FieldAttrs;
import es.stratebi.civi.util.FieldType;

public abstract class CiviMeta extends BaseStepMeta implements StepMetaInterface {

  protected static Class<?> PKG = CiviMeta.class; // for i18n purposes

  protected String civiCrmRestUrl;
  protected String civiCrmApiKey;
  protected String civiCrmSiteKey;
  protected String civiCrmEntity;
  protected ArrayList<String> civiCrmEntityList = new ArrayList<String>();

  protected HashMap<String, FieldAttrs> civiCrmFields = new HashMap<String, FieldAttrs>();
  protected HashMap<String, String> outputMap = new HashMap<String, String>();
  protected ArrayList<String> civiCrmKeyList = new ArrayList<String>();

  protected Boolean civiCrmDebugMode;
  protected String civiCrmResultField;

  public CiviMeta() {
    super();
  }

  // getters and setters for the step settings

  public String getCiviCrmRestUrl() {
    return civiCrmRestUrl;
  }

  public void setCiviCrmRestUrl(String civiCrmRestUrl) {
    this.civiCrmRestUrl = civiCrmRestUrl;
  }

  public String getCiviCrmApiKey() {
    return civiCrmApiKey;
  }

  public void setCiviCrmApiKey(String civiCrmApiKey) {
    this.civiCrmApiKey = civiCrmApiKey;
  }

  public String getCiviCrmSiteKey() {
    return civiCrmSiteKey;
  }

  public void setCiviCrmSiteKey(String civiCrmSiteKey) {
    this.civiCrmSiteKey = civiCrmSiteKey;
  }

  public String getCiviCrmEntity() {
    return civiCrmEntity;
  }

  public void setCiviCrmEntity(String civiCrmEntity) {
    this.civiCrmEntity = civiCrmEntity;
  }

  public ArrayList<String> getCiviCrmKeyList() {
    return civiCrmKeyList;
  }

  public void setCiviCrmKeyList(ArrayList<String> keyList) {
    this.civiCrmKeyList = keyList;
  }

  public ArrayList<String> getCiviCrmEntityList() {
    return civiCrmEntityList;
  }

  public HashMap<String, String> getCiviCrmOutputMap() {
    return outputMap;
  }

  public void setCiviCrmOutputMap(HashMap<String, String> outputMap) {
    this.outputMap = outputMap;
  }

  public void setCiviCrmEntityList(ArrayList<String> civiCrmEntityList) {
    this.civiCrmEntityList = civiCrmEntityList;
  }

  public HashMap<String, FieldAttrs> getCiviCrmFields() {
    return civiCrmFields;
  }

  public void setCiviCrmFields(HashMap<String, FieldAttrs> fields) {
    this.civiCrmFields.clear();
    this.civiCrmFields.putAll(fields);
  }

  /*
   * Siempre al crear esta clase se establecen valores por defecto que hacen que
   * el paso funcione correctamente, esto valores son los cargados al iniciarse
   * el dialogo por primera vez.
   * 
   * @see org.pentaho.di.trans.step.StepMetaInterface#setDefault()
   */
  public void setDefault() {
    civiCrmApiKey = "";
    civiCrmSiteKey = "";
    civiCrmEntity = "";
    civiCrmRestUrl = "";

//    civiCrmRestUrl = "http://localhost/drupal/modules/civicrm-4.3.4-drupal/civicrm/extern/rest.php";
//    civiCrmApiKey = "juan";
//    civiCrmSiteKey = "123456789";
//    civiCrmEntity = "Contact";
  }

  /*
   * Dado que aqui tenemos todos los atributos que han salido del dialogo este
   * metodo se usa para actualizar la estructura de los campo que van a salir
   * hacia algun otro paso. Note que aqui no hablamos de datos, sino de su
   * estructura para saber como se van a comportar, en parte se debe a que cada
   * fila debe contener un valor de la clase ValueMetaInterface que es la que
   * dice para cada atributo el tipo al que pertenece.
   */
  public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) {

    /*
     * Adicionar los campos de salida, estos son los que se toman luego en el
     * metodo processRow en la clase CiviInput. Note que esta clase es tambien
     * un atributo privado de CiviInput Aqui decimos el tipo de dato del campo
     */

    for (String cField : civiCrmKeyList) {
      try {
        // Añadido una verifición para evitar que campos ----------> Line no existentes queden fuera de la salida del paso al
        // no encontrarse en el listado de campos devuelto por CiviCRM. En este caso se asume
        // que el campo es una cadena automáticamente
        ValueMetaInterface v = new ValueMeta(outputMap.get(cField), (civiCrmFields.get(cField) != null) ? getMetaInterfaceType(civiCrmFields.get(cField).getfType()) : ValueMetaInterface.TYPE_STRING);
        v.setOrigin(origin);
        r.addValueMeta(v);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public int getMetaInterfaceType(String type) {
    // Valor por defecto para las conversiones en caso de que no podamos
    // identificar la constante
    int returnTypeValue = ValueMetaInterface.TYPE_STRING;
    try {
      Field f = FieldType.class.getDeclaredField("cType" + type);
      f.setAccessible(true);
      returnTypeValue = f.getInt(FieldType.civiFieldType);
    } catch (NoSuchFieldException e) {
      // e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    return returnTypeValue;
  }

  public Object clone() {

    // field by field copy is default
    CiviMeta retval = (CiviMeta) super.clone();

    retval.civiCrmFields = new HashMap<String, FieldAttrs>();
    for (FieldAttrs cField : civiCrmFields.values()) {
      try {
        retval.civiCrmFields.put(cField.getfFieldKey(), (FieldAttrs) cField.clone());
      } catch (CloneNotSupportedException e) {
        e.printStackTrace();
      }
    }

    retval.civiCrmKeyList = new ArrayList<String>();
    for (String kField : civiCrmKeyList) {
      retval.civiCrmKeyList.add(new String(kField));
    }

    retval.outputMap = new HashMap<String, String>();
    for (String kField : outputMap.keySet()) {
      retval.outputMap.put(new String(kField), new String(outputMap.get(kField)));
    }

    return retval;
  }

  /*
   * Este metodo se llama siempre antes de ejecutar una transformacion, aqui
   * estan los valores de cada parametro de los pasos que la componen por lo que
   * lo valores que se toman son los que va a tener los atributos de cada paso
   * 
   * @see org.pentaho.di.trans.step.BaseStepMeta#getXML()
   */
  public String getXML() throws KettleValueException {

    StringBuffer retval = new StringBuffer(150);

    retval.append("    ").append(XMLHandler.addTagValue("ApiKey", civiCrmApiKey));
    retval.append("    ").append(XMLHandler.addTagValue("SiteKey", civiCrmSiteKey));
    retval.append("    ").append(XMLHandler.addTagValue("Entity", civiCrmEntity));
    retval.append("    ").append(XMLHandler.addTagValue("RestUrl", civiCrmRestUrl));
    retval.append("    ").append(XMLHandler.addTagValue("ResultField", civiCrmResultField));
    retval.append("    ").append(XMLHandler.addTagValue("DebugMode", civiCrmDebugMode));

    for (String entity : civiCrmEntityList) {
      retval.append("      <entity-name>").append(entity).append("</entity-name>").append(Const.CR);
    }

    for (FieldAttrs cf : civiCrmFields.values()) {
      retval.append("      <field>").append(Const.CR);
      // Iterando por los campos de la clase
      Field[] fList = cf.getClass().getDeclaredFields();
      for (Field f : fList) {
        f.setAccessible(true);
        try {
          retval.append("        ").append(XMLHandler.addTagValue(f.getName(), (f.get(cf) != null) ? f.get(cf).toString() : ""));
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (NullPointerException e) {
          e.printStackTrace();
        }
      }
      retval.append("      </field>").append(Const.CR);
    }

    for (String entity : civiCrmKeyList) {
      retval.append("      <key-index>").append(entity).append("</key-index>").append(Const.CR);
    }

    for (String fKey : outputMap.keySet()) {
      retval.append("      <output-field>").append(Const.CR);
      retval.append("        ").append(XMLHandler.addTagValue("input-field", fKey));
      retval.append("        ").append(XMLHandler.addTagValue("output-field", outputMap.get(fKey)));
      retval.append("      </output-field>").append(Const.CR);
    }

    return retval.toString();
  }

  public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {

    try {

      civiCrmApiKey = XMLHandler.getTagValue(stepnode, "ApiKey");
      civiCrmSiteKey = XMLHandler.getTagValue(stepnode, "SiteKey");
      civiCrmEntity = XMLHandler.getTagValue(stepnode, "Entity");
      civiCrmRestUrl = XMLHandler.getTagValue(stepnode, "RestUrl");
      civiCrmResultField = XMLHandler.getTagValue(stepnode, "ResultField");
      civiCrmDebugMode = Boolean.valueOf(XMLHandler.getTagValue(stepnode, "civiCrmDebugMode"));
      civiCrmEntityList = new ArrayList<String>();

      int totalNodes = XMLHandler.countNodes(stepnode, "entity-name");

      for (int i = 0; i < totalNodes; i++) {
        try {
          // Extrayendo y actualizando el valor del campo
          Node node = XMLHandler.getSubNodeByNr(stepnode, "entity-name", i);
          String entityName = XMLHandler.getNodeValue(node);
          civiCrmEntityList.add(entityName);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        }
      }

      totalNodes = XMLHandler.countNodes(stepnode, "field");
      civiCrmFields = new HashMap<String, FieldAttrs>();

      for (int i = 0; i < totalNodes; i++) {
        Node fieldNode = XMLHandler.getSubNodeByNr(stepnode, "field", i);
        FieldAttrs cf = new FieldAttrs();
        // Iterando por los campos de la clase
        Field[] fList = cf.getClass().getDeclaredFields();
        for (Field f : fList) {
          f.setAccessible(true);
          try {
            // Extrayendo y actualizando el valor del campo
            f.set(cf, XMLHandler.getTagValue(fieldNode, f.getName()));
          } catch (IllegalArgumentException e) {
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        }
        // Guardando el campo en el mapa
        civiCrmFields.put(cf.getfFieldKey(), cf);
      }

      civiCrmKeyList = new ArrayList<String>();

      totalNodes = XMLHandler.countNodes(stepnode, "key-index");

      for (int i = 0; i < totalNodes; i++) {
        try {
          // Extrayendo y actualizando el valor del campo
          Node node = XMLHandler.getSubNodeByNr(stepnode, "key-index", i);
          String entityName = XMLHandler.getNodeValue(node);
          civiCrmKeyList.add(entityName);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        }
      }

      outputMap = new HashMap<String, String>();

      // Iterando por los campos de la clase

      totalNodes = XMLHandler.countNodes(stepnode, "output-field");

      for (int i = 0; i < totalNodes; i++) {
        try {
          // Extrayendo y actualizando el valor del campo
          Node node = XMLHandler.getSubNodeByNr(stepnode, "output-field", i);
          String streamField = XMLHandler.getTagValue(node, "input-field");
          String outputField = XMLHandler.getTagValue(node, "output-field");
          outputMap.put(streamField, (outputField != null) ? outputField : "");
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      throw new KettleXMLException("Template Plugin Unable to read step info from XML node", e);
    }

  }

  public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {
    try {
      civiCrmApiKey = rep.getStepAttributeString(id_step, "apiKey");
      civiCrmSiteKey = rep.getStepAttributeString(id_step, "siteKey");
      civiCrmEntity = rep.getStepAttributeString(id_step, "entity");
      civiCrmRestUrl = rep.getStepAttributeString(id_step, "restUrl");
      civiCrmResultField = rep.getStepAttributeString(id_step, "resultField");
      civiCrmDebugMode = Boolean.valueOf(rep.getStepAttributeString(id_step, "civiCrmDebugMode"));

      int nFields = rep.countNrStepAttributes(id_step, "entityList");
      for (int i = 0; i < nFields; i++) {
        String cf = rep.getStepAttributeString(id_step, i, "entityList");
        civiCrmEntityList.add(cf);
      }

      civiCrmFields = new HashMap<String, FieldAttrs>();
      nFields = rep.countNrStepAttributes(id_step, "field");
      for (int i = 0; i < nFields; i++) {
        FieldAttrs cf = new FieldAttrs(rep.getStepAttributeString(id_step, i, "field"));
        civiCrmFields.put(cf.getfFieldKey(), cf);
      }

      nFields = rep.countNrStepAttributes(id_step, "keyList");
      for (int i = 0; i < nFields; i++) {
        String cf = rep.getStepAttributeString(id_step, i, "keyList");
        civiCrmKeyList.add(cf);
      }

      outputMap = new HashMap<String, String>();
      nFields = rep.countNrStepAttributes(id_step, "output");
      for (int i = 0; i < nFields; i++) {
        String[] cf = rep.getStepAttributeString(id_step, i, "output").split("=");
        outputMap.put(cf[0], cf[1]);
      }
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString(PKG, "CiviCrmStep.Exception.UnexpectedErrorInReadingStepInfo"), e);
    }
  }

  public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {
    try {
      rep.saveStepAttribute(id_transformation, id_step, "apiKey", civiCrmApiKey);
      rep.saveStepAttribute(id_transformation, id_step, "siteKey", civiCrmSiteKey);
      rep.saveStepAttribute(id_transformation, id_step, "entity", civiCrmEntity);
      rep.saveStepAttribute(id_transformation, id_step, "restUrl", civiCrmRestUrl); 
      rep.saveStepAttribute(id_transformation, id_step, "resultField", civiCrmResultField); 
      rep.saveStepAttribute(id_transformation, id_step, "civiCrmDebugMode", civiCrmDebugMode);
      
      int i = 0;
      for (String entity : civiCrmEntityList) {
        rep.saveStepAttribute(id_transformation, id_step, i++, "entityList", entity);
      }

      i = 0;
      for (Iterator<FieldAttrs> itField = civiCrmFields.values().iterator(); itField.hasNext();) {
        FieldAttrs field = (FieldAttrs) itField.next();
        rep.saveStepAttribute(id_transformation, id_step, i++, "field", field.toString());
      }

      i = 0;
      for (String entity : civiCrmKeyList) {
        rep.saveStepAttribute(id_transformation, id_step, i++, "keyList", entity);
      }

      i = 0;
      for (String outputField : outputMap.keySet()) {
        rep.saveStepAttribute(id_transformation, id_step, i++, "output", outputField + "=" + outputMap.get(outputField));
      }
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString(PKG, "CiviCrmStep.Exception.UnableToSaveStepInfoToRepository") + id_step, e);
    }
  }

  public void check(List<CheckResultInterface> remarks, TransMeta transmeta, StepMeta stepMeta, RowMetaInterface prev, String input[],
      String output[], RowMetaInterface info) {
    CheckResult cr;

    // See if we have input streams leading to this step!
    if (input.length > 0) {
      cr = new CheckResult(CheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG, "CiviCrmStep.Check.StepIsReceivingInfoFromOtherSteps"),
          stepMeta);
      remarks.add(cr);
    } else {
      cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "CiviCrmStep.Check.NoInputReceivedFromOtherSteps"),
          stepMeta);
      remarks.add(cr);
    }

    // also check that each expected key fields are acually coming
    if (prev != null && prev.size() > 0) {
      String error_message = "";
      boolean error_found = false;
      if (error_found) {
        cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta);
      } else {
        cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "CiviCrmStep.Check.AllFieldsFoundInInput"),
            stepMeta);
      }
      remarks.add(cr);
    } else {
      String error_message = BaseMessages.getString(PKG, "CiviCrmStep.Check.CouldNotReadFromPreviousSteps") + Const.CR;
      cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta);
      remarks.add(cr);
    }

  }

  public abstract StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name);

  public abstract StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans disp);

  public abstract StepDataInterface getStepData();

  public Boolean getCiviCrmDebugMode() {
    return civiCrmDebugMode;
  }

  public void setCiviCrmDebugMode(Boolean civiDebugMode) {
    this.civiCrmDebugMode = civiDebugMode;
  }

  public String getCiviCrmResultField() {
    return civiCrmResultField;
  }

  public void setCiviCrmResultField(String string) {
    this.civiCrmResultField = string;
  }

}
