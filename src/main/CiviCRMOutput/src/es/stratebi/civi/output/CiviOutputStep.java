package es.stratebi.civi.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.stratebi.civi.util.CiviRestService;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import es.stratebi.civi.CiviStep;

public class CiviOutputStep extends CiviStep implements StepInterface {
  private CiviRestService crUtil;

  public CiviOutputStep(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
    super(s, stepDataInterface, c, t, dis);
  }

  public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {

    civiMeta = (CiviOutputMeta) smi;
    civiData = (CiviOutputData) sdi;

    Object[] inData = getRow(); // get row, blocks when needed!
    if (inData == null) // no more input to be expected...
    {
      setOutputDone();
      // Closing CiviCrm Server connection
      // crUtil.closeSocketConnection();
      return false;
    }

    if (first) {
      first = false;
      // Tomar el flujo de entrada
      ((CiviOutputData) civiData).outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
      // Actualizar los metadatos de los campos existentes y añadir result field name 
      ((CiviOutputMeta) civiMeta).getFields(((CiviOutputData) civiData).outputRowMeta, getStepname(), null, null, this);
      List<ValueMetaInterface> metaList = ((CiviOutputData) civiData).outputRowMeta.getValueMetaList();
      
      ((CiviOutputData) civiData).conversionMeta = new ValueMetaInterface[metaList.size()];
      int i = 0;
      for (ValueMetaInterface metaInterface : metaList) {
        ValueMetaInterface returnMeta = metaInterface;

        ValueMetaInterface conversionMeta = returnMeta.clone();
        ((CiviOutputData) civiData).conversionMeta[i] = conversionMeta;
        i++;
      }
    }
    
    String params = "";
    String[] fieldNames = getInputRowMeta().getFieldNames();
    for (String field : ((CiviOutputMeta) civiMeta).getCiviCrmKeyList()) {
      String civiField = ((CiviOutputMeta) civiMeta).getCiviCrmOutputMap().get(field);
//      params = params + "&" + civiField;
      int i = 0;
      for (String fName : fieldNames) {
        if (fName.equals(field)) {
          if (inData[i] != null && !inData[i].toString().isEmpty()) {
            params = params + "&" + civiField + "=" + inData[i];
          } else {
              // Ignorar campo id si este no viene con datos para crear un nuevo registro
              if (!civiField.equalsIgnoreCase("id")) params += civiField + "=";
          }

        }
        i++;
      }
    }

    crUtil.setDebug(((CiviOutputMeta) civiMeta).getCiviCrmDebugMode());

    // Open CiviCrm server connection and send rows to CiviCrm Rest API
    String jsonResult = crUtil.putValues(params, "", false);
    String resultField = environmentSubstitute(((CiviOutputMeta) civiMeta).getCiviCrmResultField());
    if (log.isRowLevel()) {
      logRowlevel("Call no limit url\n\t\t" + crUtil.getCallUrl());
      logRowlevel("CiviCRM API response \n\t\t" + jsonResult);
      if (resultField != null && !resultField.equals("")) {
        logRowlevel("JSON: " + jsonResult);
      }
    }
    ArrayList<Object> inList = new ArrayList<Object>(Arrays.asList(inData));
    if (resultField != null && !resultField.equals("")) {
      if (inData.length > ((CiviOutputData) civiData).outputRowMeta.size()) {
        int index = ((CiviOutputData) civiData).outputRowMeta.indexOfValue(resultField);
        inList.set(index, jsonResult);
      } else {
        inList.add(jsonResult);
      }
    }
    putRow(((CiviOutputData) civiData).outputRowMeta, inList.toArray());

    // Some basic logging
    if (checkFeedback(getLinesRead())) {
      if (log.isBasic())
        logBasic(BaseMessages.getString(PKG, "CiviCrmStep.Msg.LinesReaded", Long.toString(getLinesWritten())));
    }

    return true;
  }

  public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
    boolean passed = super.init(smi, sdi);
    if (passed) {
      crUtil = new CiviRestService(environmentSubstitute(((CiviOutputMeta) civiMeta).getCiviCrmRestUrl()),
          environmentSubstitute(((CiviOutputMeta) civiMeta).getCiviCrmApiKey()),
          environmentSubstitute(((CiviOutputMeta) civiMeta).getCiviCrmSiteKey()),
          environmentSubstitute(((CiviOutputMeta) civiMeta).getCiviCrmEntity()));
      crUtil.setAction(((CiviOutputMeta) civiMeta).getCiviCrmAction());
    }
    return passed;
  }

  public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
    super.dispose(smi, sdi);
  }

  @Override
  protected Object[] readOneRow() throws KettleException {
    return null;
  }

}
