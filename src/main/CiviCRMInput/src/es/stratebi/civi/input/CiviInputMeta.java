package es.stratebi.civi.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import es.stratebi.civi.CiviMeta;

public class CiviInputMeta extends CiviMeta implements StepMetaInterface {

  protected Integer civiCrmPageSize;
  protected HashMap<String, String> filterMap = new HashMap<String, String>();
  protected ArrayList<String> filterList = new ArrayList<String>();

  public CiviInputMeta() {
    super();
  }

  /*
   * Siempre al crear esta clase se establecen valores por defecto que hacen que
   * el paso funcione correctamente, esto valores son los cargados al iniciarse
   * el dialogo por primera vez.
   * 
   * @see org.pentaho.di.trans.step.StepMetaInterface#setDefault()
   */
  public void setDefault() {
    super.setDefault();
    civiCrmPageSize = 25;
  }

  public Object clone() {

    // field by field copy is default
    CiviInputMeta retval = (CiviInputMeta) super.clone();

    retval.filterMap = new HashMap<String, String>();
    for (String kField : filterMap.keySet()) {
      retval.filterMap.put(new String(kField), new String(filterMap.get(kField)));
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
    String xml = super.getXML();
    StringBuffer retval = new StringBuffer(150);
    retval.append(xml);

    for (String filterKey : filterList) {
      retval.append("      <filter-key>").append(filterKey).append("</filter-key>").append(Const.CR);
    }

    retval.append("      <filter>").append(Const.CR);
    for (String fKey : filterMap.keySet()) {
      retval.append("        ").append(XMLHandler.addTagValue(fKey, filterMap.get(fKey)));
    }
    retval.append("      </filter>").append(Const.CR);

    return retval.toString();
  }

  public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {

    try {

      super.loadXML(stepnode, databases, counters);

      try {
        civiCrmPageSize = Integer.parseInt(XMLHandler.getTagValue(stepnode, "PageSize"));
      } catch (Exception e1) {
        civiCrmPageSize = 25;
      }
      civiCrmPageSize = (civiCrmPageSize == null) ? 25 : civiCrmPageSize;

      int totalNodes = 0;

      filterList = new ArrayList<String>();

      totalNodes = XMLHandler.countNodes(stepnode, "filter-key");

      for (int i = 0; i < totalNodes; i++) {
        try {
          // Extrayendo y actualizando el valor del campo
          Node node = XMLHandler.getSubNodeByNr(stepnode, "filter-key", i);
          String entityName = XMLHandler.getNodeValue(node);
          filterList.add(entityName);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        }
      }

      filterMap = new HashMap<String, String>();

      Node fieldNode = XMLHandler.getNodes(stepnode, "filter").get(0);
      // Iterando por los campos de la clase
      for (String f : fields.keySet()) {
        try {
          // Extrayendo y actualizando el valor del campo
          String fValue = XMLHandler.getTagValue(fieldNode, f);
          if (fValue != null)
            filterMap.put(f, (fValue != null) ? fValue : "");
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
      super.readRep(rep, id_step, databases, counters);
      
      civiCrmPageSize = (int) rep.getStepAttributeInteger(id_step, "civiCrmPageSize");

      int nFields = rep.countNrStepAttributes(id_step, "filterKey");
      for (int i = 0; i < nFields; i++) {
        String cf = rep.getStepAttributeString(id_step, i, "filterKey");
        filterList.add(cf);
      }

      filterMap = new HashMap<String, String>();
      nFields = rep.countNrStepAttributes(id_step, "filter");
      for (int i = 0; i < nFields; i++) {
        String[] cf = rep.getStepAttributeString(id_step, i, "filter").split("=");
        filterMap.put(cf[0], cf[1]);
      }

    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString(PKG, "CiviCrmStep.Exception.UnexpectedErrorInReadingStepInfo"), e);
    }
  }

  public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {
    try {
      super.saveRep(rep, id_transformation, id_step);
      
      rep.saveStepAttribute(id_transformation, id_step, "civiCrmPageSize", civiCrmPageSize);

      int i = 0;
      for (String filterKey : filterList) {
        rep.saveStepAttribute(id_transformation, id_step, i++, "filterKey", filterKey);
      }

      i = 0;
      for (String filterField : filterMap.keySet()) {
        rep.saveStepAttribute(id_transformation, id_step, i++, "filter", filterField + "=" + filterMap.get(filterField));
      }
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString(PKG, "CiviCrmStep.Exception.UnableToSaveStepInfoToRepository") + id_step, e);
    }
  }

  protected Integer getCiviCrmPageSize() {
    return civiCrmPageSize;
  }

  protected void setCiviCrmPageSize(Integer civiCrmPageSize) {
    this.civiCrmPageSize = civiCrmPageSize;
  }

  protected HashMap<String, String> getFilterMap() {
    return filterMap;
  }

  protected void setFilterMap(HashMap<String, String> filterMap) {
    this.filterMap = filterMap;
  }

  protected ArrayList<String> getFilterList() {
    return filterList;
  }

  protected void setFilterList(ArrayList<String> filterList) {
    this.filterList = filterList;
  }

  public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name) {
    return new CiviInputDialog(shell, meta, transMeta, name);
  }

  public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans disp) {
    return new CiviInputStep(stepMeta, stepDataInterface, cnr, transMeta, disp);
  }

  public StepDataInterface getStepData() {
    return new CiviInputData();
  }

}
