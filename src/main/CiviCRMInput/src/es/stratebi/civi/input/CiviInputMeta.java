package es.stratebi.civi.input;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import es.stratebi.civi.CiviMeta;

public class CiviInputMeta extends CiviMeta implements StepMetaInterface {

    private Integer civiCrmPageSize;
    private Boolean hasPreviousStep = false;
    private HashMap<String, String> civiCrmFilterMap = new HashMap<String, String>();
    private ArrayList<String> civiCrmFilterList = new ArrayList<String>();
    private ArrayList<String> civiCrmFilterOperator = new ArrayList<String>();
    private ArrayList<String> civiCrmPrevFields = new ArrayList<String>();
    private Boolean civiCrmPassRowOnFail = false;
    private String civiCrmOnMultipleRows;
    private String civiCrmEntityOptionField;

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

        retval.civiCrmFilterMap = new HashMap<String, String>();
        for (String kField : civiCrmFilterMap.keySet()) {
            retval.civiCrmFilterMap.put(kField, civiCrmFilterMap.get(kField));
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
        StringBuilder retval = new StringBuilder(150);
        retval.append(xml);

        retval.append("    ").append(XMLHandler.addTagValue("OnMultipleRows", civiCrmOnMultipleRows));
        retval.append("    ").append(XMLHandler.addTagValue("OptionField", civiCrmEntityOptionField));
        retval.append("    ").append(XMLHandler.addTagValue("PassRowOnFail", civiCrmPassRowOnFail ? "Y" : "N"));
        retval.append("    ").append(XMLHandler.addTagValue("PageSize", civiCrmPageSize));
        retval.append("    ").append(XMLHandler.addTagValue("hasPreviousStep", hasPreviousStep ? "Y" : "N"));

        for (String filterKey : civiCrmFilterList) {
            retval.append("      <filter-key>").append(filterKey).append("</filter-key>").append(Const.CR);
        }

        retval.append("      <filter>").append(Const.CR);
        for (String fKey : civiCrmFilterMap.keySet()) {
            retval.append("        ").append(XMLHandler.addTagValue(fKey, civiCrmFilterMap.get(fKey)));
        }
        retval.append("      </filter>").append(Const.CR);

        for (String field : civiCrmPrevFields) {
            retval.append("      <previous-field>").append(field).append("</previous-field>").append(Const.CR);
        }

        for (String field : civiCrmFilterOperator) {
            try {
                retval.append("      <filter-operator>").append(URLEncoder.encode(field, "UTF8")).append("</filter-operator>").append(Const.CR);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return retval.toString();
    }

    public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) {
        super.getFields(r, origin, info, nextStep, space);

        if (civiCrmResultField != null && !civiCrmResultField.equals("")) {
            ValueMetaInterface v = new ValueMeta(civiCrmResultField, ValueMetaInterface.TYPE_STRING);
            v.setOrigin(origin);
            r.addValueMeta(v);
        }
    }

    public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {
        try {

            super.loadXML(stepnode, databases, counters);

            civiCrmOnMultipleRows = XMLHandler.getTagValue(stepnode, "OnMultipleRows");
            civiCrmEntityOptionField = XMLHandler.getTagValue(stepnode, "OptionField");
            civiCrmPassRowOnFail = XMLHandler.getTagValue(stepnode, "PassRowOnFail").equals("Y");

            try {
                civiCrmPageSize = Integer.parseInt(XMLHandler.getTagValue(stepnode, "PageSize"));
            } catch (Exception e1) {
                civiCrmPageSize = 25;
            }
            hasPreviousStep = XMLHandler.getTagValue(stepnode, "hasPreviousStep").equals("Y");

            int totalNodes;

            civiCrmFilterList = new ArrayList<String>();

            totalNodes = XMLHandler.countNodes(stepnode, "filter-key");

            for (int i = 0; i < totalNodes; i++) {
                try {
                    // Extrayendo y actualizando el valor del campo
                    Node node = XMLHandler.getSubNodeByNr(stepnode, "filter-key", i);
                    String filterName = XMLHandler.getNodeValue(node);
                    civiCrmFilterList.add(filterName);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            civiCrmFilterMap = new HashMap<String, String>();

            Node fieldNode = XMLHandler.getNodes(stepnode, "filter").get(0);
            // Iterando por los campos de la clase
            for (String f : civiCrmFilterList) {
                try {
                    // Extrayendo y actualizando el valor del campo
                    String fValue = XMLHandler.getTagValue(fieldNode, f);
                    if (fValue != null)
                        civiCrmFilterMap.put(f, fValue);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            civiCrmPrevFields = new ArrayList<String>();

            totalNodes = XMLHandler.countNodes(stepnode, "previous-field");

            for (int i = 0; i < totalNodes; i++) {
                try {
                    // Extrayendo y actualizando el valor del campo
                    Node node = XMLHandler.getSubNodeByNr(stepnode, "previous-field", i);
                    String fieldName = XMLHandler.getNodeValue(node);
                    civiCrmPrevFields.add(fieldName);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            civiCrmFilterOperator = new ArrayList<String>();

            totalNodes = XMLHandler.countNodes(stepnode, "filter-operator");

            for (int i = 0; i < totalNodes; i++) {
                try {
                    // Extrayendo y actualizando el valor del campo
                    Node node = XMLHandler.getSubNodeByNr(stepnode, "filter-operator", i);
                    String fieldName = URLDecoder.decode(XMLHandler.getNodeValue(node), "UTF8");
                    civiCrmFilterOperator.add(fieldName);
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

            civiCrmOnMultipleRows = rep.getStepAttributeString(id_step, "OnMultipleRows");
            civiCrmEntityOptionField = rep.getStepAttributeString(id_step, "civiCrmEntityOptionField");
            civiCrmPassRowOnFail = rep.getStepAttributeBoolean(id_step, "civiCrmPassRowOnFail");

            civiCrmPageSize = (int) rep.getStepAttributeInteger(id_step, "civiCrmPageSize");
            hasPreviousStep = rep.getStepAttributeBoolean(id_step, "civiCrmPageSize");

            civiCrmFilterList = new ArrayList<String>();
            int nFields = rep.countNrStepAttributes(id_step, "filterKey");
            for (int i = 0; i < nFields; i++) {
                String cf = rep.getStepAttributeString(id_step, i, "filterKey");
                civiCrmFilterList.add(cf);
            }

            civiCrmFilterMap = new HashMap<String, String>();
            nFields = rep.countNrStepAttributes(id_step, "filter");
            for (int i = 0; i < nFields; i++) {
                String[] cf = rep.getStepAttributeString(id_step, i, "filter").split("=");
                civiCrmFilterMap.put(cf[0], cf[1]);
            }

            nFields = rep.countNrStepAttributes(id_step, "prevFields");
            civiCrmPrevFields = new ArrayList<String>();
            for (int i = 0; i < nFields; i++) {
                String cf = rep.getStepAttributeString(id_step, i, "prevFields");
                civiCrmPrevFields.add(cf);
            }

            nFields = rep.countNrStepAttributes(id_step, "filterOperator");
            civiCrmFilterOperator = new ArrayList<String>();
            for (int i = 0; i < nFields; i++) {
                String cf = rep.getStepAttributeString(id_step, i, "filterOperator");
                civiCrmFilterOperator.add(cf);
            }
        } catch (Exception e) {
            throw new KettleException(BaseMessages.getString(PKG, "CiviCrmStep.Exception.UnexpectedErrorInReadingStepInfo"), e);
        }
    }

    public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {
        try {
            super.saveRep(rep, id_transformation, id_step);

            rep.saveStepAttribute(id_transformation, id_step, "civiCrmOnMultipleRows", civiCrmOnMultipleRows);
            rep.saveStepAttribute(id_transformation, id_step, "civiCrmEntityOptionField", civiCrmEntityOptionField);
            rep.saveStepAttribute(id_transformation, id_step, "civiCrmPassRowOnFail", civiCrmPassRowOnFail);

            rep.saveStepAttribute(id_transformation, id_step, "civiCrmPageSize", civiCrmPageSize);
            rep.saveStepAttribute(id_transformation, id_step, "hasPreviousStep", hasPreviousStep);

            int i = 0;
            for (String filterKey : civiCrmFilterList) {
                rep.saveStepAttribute(id_transformation, id_step, i++, "filterKey", filterKey);
            }

            i = 0;
            for (String filterField : civiCrmFilterMap.keySet()) {
                rep.saveStepAttribute(id_transformation, id_step, i++, "filter", filterField + "=" + civiCrmFilterMap.get(filterField));
            }

            i = 0;
            for (String previousField : civiCrmPrevFields) {
                rep.saveStepAttribute(id_transformation, id_step, i++, "previousField", previousField);
            }


            i = 0;
            for (String filterOperator : civiCrmFilterOperator) {
                rep.saveStepAttribute(id_transformation, id_step, i++, "filterOperator", filterOperator);
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

    protected HashMap<String, String> getCiviCrmFilterMap() {
        return civiCrmFilterMap;
    }

    protected void setCiviCrmFilterMap(HashMap<String, String> filterMap) {
        this.civiCrmFilterMap = filterMap;
    }

    protected ArrayList<String> getCiviCrmFilterList() {
        return civiCrmFilterList;
    }

    protected void setCiviCrmFilterList(ArrayList<String> filterList) {
        this.civiCrmFilterList = filterList;
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

    protected ArrayList<String> getCiviCrmPrevFields() {
        return civiCrmPrevFields;
    }

    protected void setCiviCrmPrevFields(ArrayList<String> prevFields) {
        this.civiCrmPrevFields = prevFields;
    }

    protected ArrayList<String> getCiviCrmFilterOperator() {
        return civiCrmFilterOperator;
    }

    protected void setCiviCrmFilterOperator(ArrayList<String> civiCrmFilterOperator) {
        this.civiCrmFilterOperator = civiCrmFilterOperator;
    }

    protected boolean hasPreviousStep() {
        return hasPreviousStep;
    }

    protected void setHasPreviousStep(boolean hasPreviousStep) {
        this.hasPreviousStep = hasPreviousStep;
    }

    public void setCiviCrmPassRowOnFail(boolean civiCrmPassRowOnFail) {
        this.civiCrmPassRowOnFail = civiCrmPassRowOnFail;
    }

    public Boolean getCiviCrmPassRowOnFail() {
        return civiCrmPassRowOnFail;
    }

    public void setCiviCrmOnMultipleRows(String civiCrmOnMultipleRows) {
        this.civiCrmOnMultipleRows = civiCrmOnMultipleRows;
    }

    public String getCiviCrmOnMultipleRows() {
        return civiCrmOnMultipleRows;
    }

    public void setCiviCrmEntityOptionField(String civiCrmEntityOptionField) {
        this.civiCrmEntityOptionField = civiCrmEntityOptionField;
    }

    public String getCiviCrmEntityOptionField() {
        return civiCrmEntityOptionField;
    }
}
